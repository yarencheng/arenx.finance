package arenx.finance;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public class PMF {

	private static PersistenceManagerFactory pmf = null;

	public static void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
		PMF.pmf = pmf;
	}

	public static PersistenceManager getPersistenceManager() {
		
		if (pmf == null) {
			pmf = JDOHelper.getPersistenceManagerFactory("arenx.finance");
		}
		
		return pmf.getPersistenceManager();
	}
}
