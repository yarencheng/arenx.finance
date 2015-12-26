package arenx.finance.statistics;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public class Statistics {
	private static PersistenceManagerFactory pmf = null;
	public static void setPersistenceManagerFactory(PersistenceManagerFactory pmf){
		Statistics.pmf=pmf;
	}
	public static PersistenceManager getPersistenceManager(){
		if(pmf==null)
			pmf = JDOHelper.getPersistenceManagerFactory("arenx.finance");
		PersistenceManager pm = pmf.getPersistenceManager();
		return pm;
	}
}
