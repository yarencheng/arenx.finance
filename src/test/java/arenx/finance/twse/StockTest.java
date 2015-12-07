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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import arenx.finance.UnitTestTool;

public class StockTest {
	
	final static Logger logger = LoggerFactory.getLogger(StockTest.class);
	
	PersistenceManagerFactory pmf;
	
	@Before
	public void before(){
		pmf=UnitTestTool.createNewPersistenceManagerFactory();
		Stock.setPersistenceManagerFactory(pmf);
	}
	
	@After
	public void after() throws InterruptedException{

	}
		
	@Test
	public void getStockIDForYahooFinance(){
		// prepare
		StockBean s1=new StockBean("1");
		s1.setNumber(1);
		s1.setMarketType("上市");
		StockBean s2=new StockBean("2");
		s2.setNumber(2);
		s2.setMarketType("上櫃");
		StockBean s3=new StockBean("3");
		s3.setNumber(3);
		s3.setMarketType("興櫃");
		List<StockBean> beanList=Lists.newArrayList(s1,s2,s3);

		// action
		List<String> stringList=Stock.getStockIDForYahooFinance(beanList);
		
		// verify
		assertEquals(beanList.size(), stringList.size());
		assertTrue(stringList.get(0).equals("1.TW"));
		assertTrue(stringList.get(1).equals("2.TWO"));
		assertTrue(stringList.get(2).equals("3.TWO"));
	}
	
	@Test
	public void getFromTwscWebsite(){
		// prepare
		List<Integer>numberList=Lists.newArrayList(1101,1413,1423,8996,9960,1260,9961);

		// action
		List<StockBean> beanList=Stock.getFromTwscWebsite();
		
		// verify
		Map<Integer,List<StockBean>>beanMap = beanList.stream()
			.filter(bean->{
				return numberList.contains(bean.getNumber());
			})
			.collect(Collectors.groupingBy(StockBean::getNumber));
		logger.info("beanMap: {}",beanMap);
		assertEquals(beanMap.size(), 7);
		beanMap.forEach((k,v)->assertTrue("k="+k,numberList.contains(k)));
	}
	
	@Test
	public void insert(){
		// prepare
		StockBean expectedBean=new StockBean("test");
		expectedBean.setCFICode(RandomStringUtils.randomAlphanumeric(10));
		expectedBean.setIndustryType(RandomStringUtils.randomAlphanumeric(10));
		expectedBean.setMarketType(RandomStringUtils.randomAlphanumeric(10));
		expectedBean.setName(RandomStringUtils.randomAlphanumeric(10));
		expectedBean.setNumber(123);
		expectedBean.setStartDate(new Date());
		
		// action
		Stock.insert(Lists.newArrayList(expectedBean));
		
		//verify
		PersistenceManager pm=pmf.getPersistenceManager();
		StockBean actualBean = pm.getObjectById(StockBean.class,"test");
		assertNotNull(actualBean);
		assertEquals(expectedBean, actualBean);
	}
	
	@Test
	public void insert_replace(){
		// prepare
		StockBean bean1=new StockBean("test");
		bean1.setNumber(1);
		StockBean bean2=new StockBean("test");
		bean2.setNumber(2);
		
		// action
		Stock.insert(Lists.newArrayList(bean1));
		try {
			Stock.insert(Lists.newArrayList(bean2));
			fail();
		} catch (JDODataStoreException e) {
			// pass
		}
		
		//verify
	}
	
	@Test
	public void getAll(){
		// prepare
		StockBean bean1=new StockBean("test1");
		bean1.setNumber(1);
		StockBean bean2=new StockBean("test2");
		bean2.setNumber(2);
		List<StockBean>expectedList=Lists.newArrayList(bean1,bean2);
		
		// action
		PersistenceManager pm=pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		pm.makePersistentAll(bean1,bean2);
		pm.close();
		
		//verify
		List<StockBean>actualList=Stock.getAll();
		logger.debug("actualList: {}",actualList);
		logger.debug("expectedList: {}",expectedList);
		assertEquals(expectedList.size(), actualList.size());
		assertTrue(actualList.containsAll(expectedList));
	}
}
