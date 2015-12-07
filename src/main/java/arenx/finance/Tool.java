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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.PersistenceCapable;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
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
		
		AtomicBoolean isWalked=new AtomicBoolean(false);
		AtomicBoolean isStart=new AtomicBoolean(false);
		
		Map<Runnable,Boolean> runnable_isSSuccess_map=Collections.synchronizedMap(new HashMap<Runnable, Boolean>());
	}
	
	public static void updateAll_(){
		Set<UpdateTask>todoTaskSet = Collections.synchronizedSet(updateAll_getTaskSet());
		Set<UpdateTask>taskSet = new HashSet<UpdateTask>(todoTaskSet);
		updateAll_updatedependentTaskSet(todoTaskSet);
		updateAll_checkDepedentLoop(todoTaskSet);
		updateAll_checkMethod(todoTaskSet);
		
		
		Map<Class<?>,Set<Method>>pendingMap=Collections.synchronizedMap(updateAll_getPendingMap(todoTaskSet));
		Set<Class<?>>finishSet=Collections.synchronizedSet(new HashSet<Class<?>>());
		Set<Class<?>>failSet=Collections.synchronizedSet(new HashSet<Class<?>>());
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		Thread tryRunAllTask=new Thread(){
			public void run(){
				updateAll_tryRunAllMethod(executorService,finishSet,failSet,new HashSet<UpdateTask>(todoTaskSet));
			}
		};
		Thread monitorAllTask=new Thread(){
			public void run(){
				updateAll_scanFinishTask(executorService,finishSet,failSet,pendingMap,new HashSet<UpdateTask>(todoTaskSet));
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
		updateAll_logFailTask(taskSet);
		logger.info("finish");
	}
	
	private static void updateAll_logFailTask(Set<UpdateTask>taskSet){
		taskSet.forEach(t->{
			t.runnable_isSSuccess_map.forEach((k,v)->{
				if(!v){
					logger.warn("Failed task {}:{}:{}", t.parentClazz.getSimpleName(), t.method.getName(), k);
				}
			});
		});
	}
	
	private static void updateAll_scanFinishTask(ExecutorService executorService, Set<Class<?>>finishSet,Set<Class<?>>failSet,Map<Class<?>,Set<Method>>pendingMap,Set<UpdateTask>todoTaskSet){
		while(todoTaskSet.isEmpty()==false){
			UpdateTask nextTask=null;
			LOOP:
			for(UpdateTask task:todoTaskSet){
				if(failSet.contains(task.parentClazz)){
					nextTask = task;
					break;
				}
				if(task.isStart.get()==false){
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
				for(Future<?>future:new HashSet<Future<?>>(task.future_runnable_map.keySet())){
					try {
						future.get();
						task.runnable_isSSuccess_map.put(task.future_runnable_map.get(future), true);
						logger.info("sucess to execute runnable: '{}' of method: '{}'",task.future_runnable_map.get(future), task.method.getName());
					} catch (InterruptedException | ExecutionException e) {
						task.runnable_isSSuccess_map.put(task.future_runnable_map.get(future), false);
						isFail=true;
						logger.warn("fail to execute runnable: '{}' of method: '{}', cause by: '{}'",task.future_runnable_map.get(future), task.method.getName(), e.getMessage());
						logger.debug(e.getMessage(),e);
						int retry = task.future_retry_map.get(future)+1;
						if(retry<=task.method.getDeclaredAnnotation(Updatable.class).retry()){
							task.future_runnable_map.get(future);
							logger.warn("retry: '{}' of '{}'",task.future_runnable_map.get(future),  task.method.getName());
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
				todoTaskSet.remove(nextTask);
			}else
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
		}
	}
	
	private static void updateAll_tryRunAllMethod(ExecutorService executorService,Set<Class<?>>finishSet,Set<Class<?>>failSet, Set<UpdateTask>todoTaskSet){
		
		while(todoTaskSet.isEmpty()==false){
			UpdateTask nextTask=null;
			LOOP:
			for(UpdateTask task:todoTaskSet){
				if(finishSet.containsAll(task.dependentClassSet)){
					Collection<Runnable>runableSet=updateAll_getRunnableSet(task.method);
					logger.info("add to queue with {} tasks of {} method",  runableSet.size(),task.method);
					for(Runnable runnable:runableSet){
						Future<?>future=executorService.submit(runnable);
						task.future_runnable_map.put(future,runnable);
						task.future_retry_map.put(future,0);
					}
					task.isStart.set(true);
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
				todoTaskSet.remove(nextTask);
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
	
	private static Map<Class<?>,Set<Method>> updateAll_getPendingMap(Set<UpdateTask>todoTaskSet){
		Map<Class<?>,Set<Method>> map = new HashMap<Class<?>, Set<Method>>();
		for(UpdateTask u:todoTaskSet){
			Set<Method>set=map.get(u.parentClazz);
			if(set==null){
				set=new HashSet<Method>();
				map.put(u.parentClazz, set);
			}
			set.add(u.method);
		}
		return map;
	}
	
	private static void updateAll_checkMethod(Set<UpdateTask>todoTaskSet){
		todoTaskSet.stream()
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
	
	private static void updateAll_checkDepedentLoop(Set<UpdateTask>todoTaskSet){
		for(UpdateTask u:todoTaskSet){
			updateAll_checkDepedentLoop_recur(u,0);
			u.isWalked.set(false);
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
		if(task.isWalked.get())
			throw new RuntimeException(String.format("infinite depedency loop found with method '%s' in '%s'",task.method, task.parentClazz));
		task.isWalked.set(true);
		for(UpdateTask u:task.dependentTaskSet){
			updateAll_checkDepedentLoop_recur(u,level+1);
			u.isWalked.set(false);
		}
	}
	
	private static void updateAll_updatedependentTaskSet(Set<UpdateTask>todoTaskSet){
		for(UpdateTask u1:todoTaskSet){
			for(Class<?>dc:u1.dependentClassSet){
				for(UpdateTask u2:todoTaskSet){
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
	
	public static void createAllSchema(){
		JDOPersistenceManagerFactory d=(JDOPersistenceManagerFactory) JDOHelper.getPersistenceManagerFactory("arenx.finance");
		PersistenceNucleusContext p =d.getNucleusContext();
		StoreManager s=p.getStoreManager();
		SchemaAwareStoreManager sa=(SchemaAwareStoreManager) s;
		Reflections reflections = new Reflections("arenx.finance");
		sa.createSchemaForClasses(reflections.getTypesAnnotatedWith(PersistenceCapable.class).stream().map(m->m.getName()).collect(Collectors.toSet()), null);
	}
	

}
