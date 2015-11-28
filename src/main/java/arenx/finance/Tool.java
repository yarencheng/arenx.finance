package arenx.finance;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import arenx.finance.annotation.Updatable;
import javassist.Modifier;

public class Tool {

	private final static Logger logger = LoggerFactory.getLogger(Tool.class);

	private final static RestTemplate restTemplate;

	static {
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		clientHttpRequestFactory.setConnectionRequestTimeout(30000);
		clientHttpRequestFactory.setConnectTimeout(30000);
		clientHttpRequestFactory.setReadTimeout(30000);
		restTemplate = new RestTemplate(clientHttpRequestFactory);

	}

	public static RestOperations getRestOperations() {
		return restTemplate;
	}
	
	private static class UpdateTask{
		Class<?> parentClazz;
		Method method;

		Set<Class<?>> dependentClassSet = Collections.synchronizedSet(new HashSet<Class<?>>());
		Set<UpdateTask> dependentTaskSet = Collections.synchronizedSet(new HashSet<UpdateTask>());
		
		Map<Future<?>,Runnable> future_runnable_map=Collections.synchronizedMap(new HashMap<Future<?>, Runnable>());
		Map<Future<?>,Integer> future_retry_map=Collections.synchronizedMap(new HashMap<Future<?>, Integer>());
		
		boolean isWalked=false;
		boolean isStart=false;
	}
	
	public static void updateAll_(){
		Set<UpdateTask>taskSet = Collections.synchronizedSet(updateAll_getTaskSet());
		updateAll_updatedependentTaskSet(taskSet);
		updateAll_checkDepedentLoop(taskSet);
		updateAll_checkMethod(taskSet);
		
		
		Map<Class<?>,Set<Method>>pendingMap=Collections.synchronizedMap(updateAll_getPendingMap(taskSet));
		Set<Class<?>>finishSet=Collections.synchronizedSet(new HashSet<Class<?>>());
		Set<Class<?>>failSet=Collections.synchronizedSet(new HashSet<Class<?>>());
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		Thread tryRunAllTask=new Thread(){
			public void run(){
				updateAll_tryRunAllMethod(executorService,finishSet,failSet,new HashSet<UpdateTask>(taskSet));
			}
		};
		Thread monitorAllTask=new Thread(){
			public void run(){
				updateAll_scanFinishTask(executorService,finishSet,failSet,pendingMap,new HashSet<UpdateTask>(taskSet));
			}
		};
		tryRunAllTask.start();
		monitorAllTask.start();
		try {
			logger.debug("join tryRunAllTask");
			tryRunAllTask.join();
			logger.debug("join monitorAllTask");
			monitorAllTask.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		logger.debug("shutdown executorService");
		executorService.shutdown();
		logger.info("finish");
	}
	
	private static void updateAll_scanFinishTask(ExecutorService executorService, Set<Class<?>>finishSet,Set<Class<?>>failSet,Map<Class<?>,Set<Method>>pendingMap,Set<UpdateTask>taskSet){
		while(taskSet.isEmpty()==false){
			UpdateTask nextTask=null;
			LOOP:
			for(UpdateTask task:taskSet){
				if(failSet.contains(task.parentClazz)){
					nextTask = task;
					break;
				}
				if(task.isStart==false){
					continue;
				}
				for(Future<?>future:task.future_runnable_map.keySet()){
					if(future.isDone()==false){
						continue LOOP;
					}
				}
				logger.debug("task of method '{}' are all done", task.method);
				
				boolean isFail=false;
				boolean isRetry=false;
				for(Future<?>future:task.future_runnable_map.keySet()){
					try {
						future.get();
					} catch (InterruptedException | ExecutionException e) {
						isFail=true;
						logger.warn("fail to execute runnable: '{}' of method: '{}', cause by: '{}'",task.future_runnable_map.get(future), task.method, e.getMessage());
						logger.debug(e.getMessage(),e);
						int retry = task.future_retry_map.get(future)+1;
						if(retry<=task.method.getDeclaredAnnotation(Updatable.class).retry()){
							logger.warn("retry: '{}'",task.future_runnable_map.get(future));
							isRetry=true;
							Future<?>future_new=executorService.submit(task.future_runnable_map.get(future));
							task.future_runnable_map.put(future_new,task.future_runnable_map.get(future));
							task.future_retry_map.put(future_new,retry);
							task.future_runnable_map.remove(future);
							task.future_retry_map.remove(future);
						}
					}
				}
				logger.debug("result of method '{}' isRetry={} isFail={}", task.method, isRetry,isFail);
				
				if(isRetry){
					continue;
				}
				if(isFail){
					failSet.add(task.parentClazz);
				}else{
					pendingMap.get(task.parentClazz).remove(task.method);
					if(pendingMap.get(task.parentClazz).isEmpty()){
						finishSet.add(task.parentClazz);
					}
				}
				nextTask=task;
				break;
			}
			if(nextTask!=null){
				taskSet.remove(nextTask);
			}else
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
		}
	}
	
	private static void updateAll_tryRunAllMethod(ExecutorService executorService,Set<Class<?>>finishSet,Set<Class<?>>failSet, Set<UpdateTask>taskSet){
		
		while(taskSet.isEmpty()==false){
			UpdateTask nextTask=null;
			LOOP:
			for(UpdateTask task:taskSet){
				if(finishSet.containsAll(task.dependentClassSet)){
					Collection<Runnable>runableSet=updateAll_getRunnableSet(task.method);
					logger.info("add to queue with {} tasks of {} method",  runableSet.size(),task.method);
					for(Runnable runnable:runableSet){
						Future<?>future=executorService.submit(runnable);
						task.future_runnable_map.put(future,runnable);
						task.future_retry_map.put(future,0);
					}
					task.isStart=true;
					nextTask=task;
					break;
				}
				for(Class<?>dc:task.dependentClassSet){
					if(failSet.contains(dc)){
						logger.error("method: {} is blocked since dependent class: {} is failed",task.method,task.parentClazz);
						failSet.add(task.parentClazz);
						nextTask=task;
						break LOOP;
					}
				}
			}
			if(nextTask!=null)
				taskSet.remove(nextTask);
			else
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
		}
	}
	
	private static Collection<Runnable> updateAll_getRunnableSet(Method method){
		if(Void.TYPE.isAssignableFrom(method.getReturnType())){
			Runnable runnable=()->{
				try {
					MethodUtils.invokeExactStaticMethod(method.getDeclaringClass(), method.getName());
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e.getMessage(),e);
				}
			};
			return Collections.singleton(runnable);
		}else if(Runnable.class.isAssignableFrom(method.getReturnType())){
			Runnable runnable = null;
			try {
				runnable = (Runnable) MethodUtils.invokeExactStaticMethod(method.getDeclaringClass(), method.getName());
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(String.format("fail to invoke %s", method));
			}
			return Collections.singleton(runnable);
		}else if(Collection.class.isAssignableFrom(method.getReturnType())){
			Collection<Runnable> runnables = null;
			try {
				runnables = (Collection<Runnable>) MethodUtils.invokeExactStaticMethod(method.getDeclaringClass(), method.getName());
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(String.format("fail to invoke %s", method));
			}
			return runnables;
		}else{
			throw new RuntimeException(String.format("invalide return type %s of method %s", method.getReturnType(), method));
		}
	}
	
	private static Map<Class<?>,Set<Method>> updateAll_getPendingMap(Set<UpdateTask>taskSet){
		Map<Class<?>,Set<Method>> map = new HashMap<Class<?>, Set<Method>>();
		for(UpdateTask u:taskSet){
			Set<Method>set=map.get(u.parentClazz);
			if(set==null){
				set=new HashSet<Method>();
				map.put(u.parentClazz, set);
			}
			set.add(u.method);
		}
		return map;
	}
	
	private static void updateAll_checkMethod(Set<UpdateTask>taskSet){
		taskSet.stream()
			.map((x)->x.method)
			.forEach((x)->{
				if(!Modifier.isStatic(x.getModifiers())){
					throw new RuntimeException(String.format("method %s is not static",x));
				}
				if(!Modifier.isPublic(x.getModifiers())){
					throw new RuntimeException(String.format("method %s is not public",x));
				}
				if(!Void.TYPE.isAssignableFrom(x.getReturnType())&&
						!Runnable.class.isAssignableFrom(x.getReturnType())&&
						!Collection.class.isAssignableFrom(x.getReturnType())){
					throw new RuntimeException(String.format("return type '%s' of method '%s' is not valide", x.getReturnType(),x));
				}
			});;
	}
	
	private static void updateAll_checkDepedentLoop(Set<UpdateTask>taskSet){
		for(UpdateTask u:taskSet){
			updateAll_checkDepedentLoop_recur(u,0);
			u.isWalked=false;
		}
	}
	
	private static void updateAll_checkDepedentLoop_recur(UpdateTask task, int level){
		if(logger.isDebugEnabled()){
			StringBuilder sb=new StringBuilder();
			for(int i=0;i<level;i++)
				sb.append("  ");
			sb.append(task.method.toString());
			logger.debug(sb.toString());
		}
		if(task.isWalked)
			throw new RuntimeException(String.format("infinite depedency loop found with method '%s' in '%s'",task.method, task.parentClazz));
		task.isWalked=true;
		for(UpdateTask u:task.dependentTaskSet){
			updateAll_checkDepedentLoop_recur(u,level+1);
			u.isWalked=false;
		}
	}
	
	private static void updateAll_updatedependentTaskSet(Set<UpdateTask>taskSet){
		for(UpdateTask u1:taskSet){
			for(Class<?>dc:u1.dependentClassSet){
				for(UpdateTask u2:taskSet){
					if(u2.parentClazz.equals(dc)){
						logger.debug("'{}' is dependent on'{}'",u1.method, u2.method);
						u1.dependentTaskSet.add(u2);
					}
				}
			}
		}
	}
	
	private static Set<UpdateTask> updateAll_getTaskSet(){
		Reflections reflections = new Reflections("arenx.finance");
		Set<UpdateTask>taskSet = new HashSet<UpdateTask>();
		for(Class<?>clazz:reflections.getTypesAnnotatedWith(Updatable.class)){
			for(Method method:MethodUtils.getMethodsWithAnnotation(clazz, Updatable.class)){
				logger.debug("get an updatable method: {} within {}",method, clazz);
				UpdateTask task = new UpdateTask();
				taskSet.add(task);
				task.parentClazz=clazz;
				task.dependentClassSet.addAll(Arrays.asList(clazz.getDeclaredAnnotation(Updatable.class).depedentClass()));
				task.dependentClassSet.addAll(Arrays.asList(method.getDeclaredAnnotation(Updatable.class).depedentClass()));
				task.method=method;
			}
		}
		return taskSet;
	}
	

	

}
