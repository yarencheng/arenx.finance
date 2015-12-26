package arenx.finance.statistics;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;

import javax.jdo.FetchGroup;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import arenx.finance.UnitTestTool;
import arenx.finance.yql.YQL;
import arenx.finance.yql.yahoo.finance.HistoricaldataBean;
import arenx.finance.yql.yahoo.finance.HistoricaldataTest;

public class StockStatisticsTest {

	private final static Logger logger = LoggerFactory.getLogger(HistoricaldataTest.class);

	ObjectMapper mapper=new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
	
	PersistenceManagerFactory pmf;
	
	@Before
	public void before(){
		pmf=UnitTestTool.createNewPersistenceManagerFactory();
		Statistics.setPersistenceManagerFactory(pmf);
	}
	
	@After
	public void after() throws InterruptedException{

	}
	
	@Test
	public void updateFromYahooFinanceHistoricaldata(){
		// prepare
		HistoricaldataBean history= new HistoricaldataBean();
		ReflectionTestUtils.setField(history, "symbol", "test_symbol");
		ReflectionTestUtils.setField(history, "date", new Date());
		PersistenceManager pm = pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		pm.makePersistent(history);
		pm.close();
		
		// action
		StockStatistics.updateFromYahooFinanceHistoricaldata("test_symbol");
		
		// verify
		pm = pmf.getPersistenceManager();
		StockStatisticsBean statistics = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key(history.getSymbol(), history.getDate()).toString());
		assertEquals(history.getSymbol(), statistics.getSymbol());
		assertEquals(history.getDate(), statistics.getDate());
		assertEquals(history.getAdjClose(), statistics.getPrice());
		pm.close();
	}
	
	@Test
	public void updatePriceMean(){
		// prepare
		Calendar calendar=new Calendar.Builder().setDate(2005, 0, 1).build();
		StockStatisticsBean expectedBean1 = new StockStatisticsBean("test",calendar.getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean expectedBean2 = new StockStatisticsBean("test",calendar.getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean expectedBean3 = new StockStatisticsBean("test",calendar.getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean expectedBean4 = new StockStatisticsBean("test",calendar.getTime());
		expectedBean1.setPrice(1.1);
		expectedBean2.setPrice(2.2);
		expectedBean3.setPrice(3.3);
		expectedBean4.setPrice(4.4);
		PersistenceManager pm = pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		pm.makePersistentAll(expectedBean1,expectedBean2,expectedBean3,expectedBean4);
		pm.close();
		
		// action
		StockStatistics.updatePriceMean("test",3);
		
		// verify
		pm = pmf.getPersistenceManager();
		calendar.add(Calendar.DAY_OF_YEAR, -3);
		StockStatisticsBean actualBean1 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean actualBean2 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean actualBean3 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean actualBean4 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		assertEquals(1, actualBean1.getPriceMean().size());
		assertEquals(actualBean2.toString(), 2, actualBean2.getPriceMean().size());
		assertEquals(3, actualBean3.getPriceMean().size());
		assertEquals(3, actualBean4.getPriceMean().size());
		assertEquals(1.1, actualBean1.getPriceMean().get(1).getResult(),0.00001);
		assertEquals(2.2, actualBean2.getPriceMean().get(1).getResult(),0.00001);
		assertEquals(1.65, actualBean2.getPriceMean().get(2).getResult(),0.00001);
		assertEquals(3.3, actualBean3.getPriceMean().get(1).getResult(),0.00001);
		assertEquals(2.75, actualBean3.getPriceMean().get(2).getResult(),0.00001);
		assertEquals(2.2, actualBean3.getPriceMean().get(3).getResult(),0.00001);
		assertEquals(4.4, actualBean4.getPriceMean().get(1).getResult(),0.00001);
		assertEquals(3.85, actualBean4.getPriceMean().get(2).getResult(),0.00001);
		assertEquals(3.3, actualBean4.getPriceMean().get(3).getResult(),0.00001);
		pm.close();
	}
	
	@Test
	public void updatePriceVariance(){
		// prepare
		Calendar calendar=new Calendar.Builder().setDate(2005, 0, 1).build();
		StockStatisticsBean expectedBean1 = new StockStatisticsBean("test",calendar.getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean expectedBean2 = new StockStatisticsBean("test",calendar.getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean expectedBean3 = new StockStatisticsBean("test",calendar.getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean expectedBean4 = new StockStatisticsBean("test",calendar.getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean expectedBean5 = new StockStatisticsBean("test",calendar.getTime());
		expectedBean1.setPrice(1.1);
		expectedBean2.setPrice(2.2);
		expectedBean3.setPrice(3.3);
		expectedBean4.setPrice(4.4);
		expectedBean5.setPrice(2.75);
		PersistenceManager pm = pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		pm.makePersistentAll(expectedBean1,expectedBean2,expectedBean3,expectedBean4,expectedBean5);
		pm.close();
		
		// action
		StockStatistics.updatePriceVariance("test",3);
		
		// verify
		pm = pmf.getPersistenceManager();
		calendar.add(Calendar.DAY_OF_YEAR, -4);
		StockStatisticsBean actualBean1 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean actualBean2 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean actualBean3 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean actualBean4 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		StockStatisticsBean actualBean5 = pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key("test", calendar.getTime()).toString());
		assertEquals(1, actualBean1.getPriceVariance().size());
		assertEquals(2, actualBean2.getPriceVariance().size());
		assertEquals(3, actualBean3.getPriceVariance().size());
		assertEquals(4, actualBean4.getPriceVariance().size());
		assertEquals(4, actualBean5.getPriceVariance().size());
		assertEquals(0.605, actualBean2.getPriceVariance().get(1).getResult(),0.00001);
		assertEquals(0.605, actualBean3.getPriceVariance().get(1).getResult(),0.00001);
		assertEquals(1.21, actualBean3.getPriceVariance().get(2).getResult(),0.00001);
		assertEquals(0.605, actualBean4.getPriceVariance().get(1).getResult(),0.00001);
		assertEquals(1.21, actualBean4.getPriceVariance().get(2).getResult(),0.00001);
		assertEquals(2.0166666666667, actualBean4.getPriceVariance().get(3).getResult(),0.00001);
		assertEquals(1.36125, actualBean5.getPriceVariance().get(1).getResult(),0.00001);
		assertEquals(0.70583333333333, actualBean5.getPriceVariance().get(2).getResult(),0.00001);
		assertEquals(0.88229166666667, actualBean5.getPriceVariance().get(3).getResult(),0.00001);
		pm.close();
	}
}
