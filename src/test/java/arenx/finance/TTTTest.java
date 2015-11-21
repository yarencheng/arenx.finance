package arenx.finance;

import static org.junit.Assert.*;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.datanucleus.AbstractNucleusContext;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import arenx.finance.test.TestJDO;

public class TTTTest {
	
	final static Logger logger = LoggerFactory.getLogger(TTTTest.class);

	@Test
	public void fff() {
		PersistenceManagerFactory kk;
		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("Tutorial");
		PersistenceManager pm = pmf.getPersistenceManager();
//		TestJDO testJDO=new TestJDO();
//		testJDO.key="test key";
//		testJDO.value="test_value";
//		pm.makePersistent(testJDO);
//		pm.close();
		TestJDO t = pm.getObjectById(TestJDO.class, "test key");
		System.out.println(t);
		
		AbstractNucleusContext aa;
		
		logger.error("testtttttttttt");
		
	}
}
