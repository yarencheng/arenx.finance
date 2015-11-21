package arenx.finance;

import java.lang.reflect.InvocationTargetException;

import javax.jdo.JDOFatalInternalException;
import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.datanucleus.exceptions.NucleusUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import arenx.finance.test.TestJDO;

public class Main {
	
	final static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] argv) {
		System.out.println("hiiiiiiiiii");
		logger.error("testtttttttttt");
		
		try{

			PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("Tutorial");
			PersistenceManager pm = pmf.getPersistenceManager();
			TestJDO t = pm.getObjectById(TestJDO.class, "test key");
			System.out.println(t);
		}catch (Exception e){
			e.printStackTrace();
			System.out.println(e.getClass());
			JDOFatalUserException je = (JDOFatalUserException)e;
			for(Throwable t1:je.getNestedExceptions()){
				System.out.println("t1.getClass()="+t1.getClass());
				JDOFatalInternalException jie=(JDOFatalInternalException) t1;
				for(Throwable t2:jie.getNestedExceptions()){
					System.out.println("t2.getClass()="+t2.getClass());
					InvocationTargetException ite = (InvocationTargetException)t2;
					System.out.println("ite.getMessage()="+ite.getMessage());
					NucleusUserException nue = (NucleusUserException) ite.getCause();
					System.out.println("nue.getMessage="+nue.getMessage());
					System.out.println("nue.getCause="+nue.getCause());
					System.out.println("nue.getFailedObject="+nue.getFailedObject());
					nue.printStackTrace();
				}
			}
		}
	}
}
