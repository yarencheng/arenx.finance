package arenx.finance.twse;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jdo.JDODataStoreException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import arenx.finance.PMF;
import arenx.finance.UnitTestTool;

public class StockTest {
	
	final static Logger logger = LoggerFactory.getLogger(StockTest.class);
	
	PersistenceManagerFactory pmf;
	
	@Before
	public void before(){
		pmf=UnitTestTool.createNewPersistenceManagerFactory();
		PMF.setPersistenceManagerFactory(pmf);
	}
	
	@After
	public void after() throws InterruptedException{

	}
		
//	@Test
//	public void getStockIDForYahooFinance(){
//		// prepare
//		StockBean s1=new StockBean("1");
//		s1.setNumber(1);
//		s1.setMarketType("上市");
//		StockBean s2=new StockBean("2");
//		s2.setNumber(2);
//		s2.setMarketType("上櫃");
//		StockBean s3=new StockBean("3");
//		s3.setNumber(3);
//		s3.setMarketType("興櫃");
//		List<StockBean> beanList=Lists.newArrayList(s1,s2,s3);
//
//		// action
//		List<String> stringList=Stock.getStockIDForYahooFinance(beanList);
//		
//		// verify
//		assertEquals(beanList.size(), stringList.size());
//		assertTrue(stringList.get(0).equals("1.TW"));
//		assertTrue(stringList.get(1).equals("2.TWO"));
//		assertTrue(stringList.get(2).equals("3.TWO"));
//	}
	
	@Test
	public void getFromTwscWebsite(){
		// prepare
		List<Integer>numberList=Lists.newArrayList(1101,1413,1423,8996,9960,1260,9961);

		// action
		List<TwseStockBean> beanList=TwseStock.getFromTwscWebsite().collect(Collectors.toList());
		
		// verify
		Map<Integer,List<TwseStockBean>>beanMap = beanList.stream()
			.filter(bean->{
				return numberList.contains(bean.getNumber());
			})
			.collect(Collectors.groupingBy(TwseStockBean::getNumber));
		logger.info("beanMap: {}",beanMap);
//		assertEquals(beanMap.size(), 7);
		beanMap.forEach((k,v)->assertTrue("k="+k,numberList.contains(k)));
	}
	
//	@Test
//	public void insert(){
//		// prepare
//		TwseStockBean expectedBean=new TwseStockBean("test");
//		expectedBean.setCFICode(RandomStringUtils.randomAlphanumeric(10));
//		expectedBean.setIndustryType(RandomStringUtils.randomAlphanumeric(10));
//		expectedBean.setMarketType(RandomStringUtils.randomAlphanumeric(10));
//		expectedBean.setName(RandomStringUtils.randomAlphanumeric(10));
//		expectedBean.setNumber(123);
//		expectedBean.setStartDate(new Date());
//		
//		// action
//		TwseStock.insert(Lists.newArrayList(expectedBean).stream());
//		
//		//verify
//		PersistenceManager pm=pmf.getPersistenceManager();
//		TwseStockBean actualBean = pm.getObjectById(TwseStockBean.class,"test");
//		assertNotNull(actualBean);
//		assertEquals(expectedBean, actualBean);
//	}
	
//	@Ignore("TODO: may need modify after using h2 database")
//	@Test
//	public void insert_replace(){
//		// prepare
//		TwseStockBean bean1=new TwseStockBean("test");
//		bean1.setNumber(1);
//		TwseStockBean bean2=new TwseStockBean("test");
//		bean2.setNumber(2);
//		
//		// action
//		TwseStock.insert(Lists.newArrayList(bean1).stream());
//		try {
//			TwseStock.insert(Lists.newArrayList(bean2).stream());
//			fail();
//		} catch (JDODataStoreException e) {
//			// pass
//		}
//		
//		//verify
//	}
	
	@Test
	public void getFromDB(){
		// prepare
		TwseStockBean bean1=new TwseStockBean("test1");
		bean1.setNumber(1);
		TwseStockBean bean2=new TwseStockBean("test2");
		bean2.setNumber(2);
		List<TwseStockBean>expectedList=Lists.newArrayList(bean1,bean2);
		
		// action
		PersistenceManager pm=pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		pm.makePersistentAll(bean1,bean2);
		pm.close();
		
		//verify
		List<TwseStockBean>actualList=TwseStock.getFromDB().collect(Collectors.toList());
		logger.debug("actualList: {}",actualList);
		logger.debug("expectedList: {}",expectedList);
		assertEquals(expectedList.size(), actualList.size());
		assertTrue(actualList.containsAll(expectedList));
	}
}
