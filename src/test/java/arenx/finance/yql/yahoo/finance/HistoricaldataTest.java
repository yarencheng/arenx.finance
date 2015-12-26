package arenx.finance.yql.yahoo.finance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import arenx.finance.UnitTestTool;
import arenx.finance.yql.YQL;

public class HistoricaldataTest {

	private final static Logger logger = LoggerFactory.getLogger(HistoricaldataTest.class);

	ObjectMapper mapper=new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
	
	PersistenceManagerFactory pmf;
	
	@Before
	public void before(){
		pmf=UnitTestTool.createNewPersistenceManagerFactory();
		YQL.setPersistenceManagerFactory(pmf);
	}
	
	@After
	public void after() throws InterruptedException{

	}
	
	@Test
	public void getFromYql() throws IOException {
		// prepare
		String expectJson = "[{\"Symbol\":\"YHOO\",\"Date\":\"2009-09-14\",\"Open\":\"15.45\","
				+ "\"High\":\"15.58\",\"Low\":\"15.28\",\"Close\":\"15.57\",\"Volume\":\"19451200\","
				+ "\"Adj_Close\":\"15.57\"},{\"Symbol\":\"YHOO\",\"Date\":\"2009-09-11\",\"Open\":\"15.53\","
				+ "\"High\":\"15.68\",\"Low\":\"15.41\",\"Close\":\"15.59\",\"Volume\":\"26860700\","
				+ "\"Adj_Close\":\"15.59\"}]";
		List<HistoricaldataBean> expectedBean = mapper.readValue(expectJson, TypeFactory.defaultInstance().constructCollectionType(List.class, HistoricaldataBean.class));

		// action
		List<HistoricaldataBean> actuallBean = Historicaldata.getFromYql("YHOO", new Calendar.Builder().setDate(2009, 8, 11).build().getTime(),
				new Calendar.Builder().setDate(2009, 8, 14).build().getTime());
		
		// verify
		assertEquals(expectedBean, actuallBean);
	}
	
	@Test
	public void get() throws IOException{
		// prepare
		String expectJson = "[{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-13\",\"Open\":\"54.177\","
				+ "\"High\":\"57.044\",\"Low\":\"54.177\",\"Close\":\"56.1542\",\"Volume\":\"3982200\","
				+ "\"Adj_Close\":\"53.4713\"},{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-12\",\"Open\":"
				+ "\"54.869\",\"High\":\"54.869\",\"Low\":\"53.6827\",\"Close\":\"54.0781\",\"Volume\":"
				+ "\"743400\",\"Adj_Close\":\"51.4944\"},{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-11\","
				+ "\"Open\":\"54.869\",\"High\":\"55.2645\",\"Low\":\"53.8804\",\"Close\":\"54.869\","
				+ "\"Volume\":\"829400\",\"Adj_Close\":\"52.2475\"}]";
		List<HistoricaldataBean> expectedBean = mapper.readValue(expectJson, TypeFactory.defaultInstance().constructCollectionType(List.class, HistoricaldataBean.class));
		PersistenceManager pm = pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		pm.makePersistentAll(expectedBean);
		pm.close();

		// action
		List<HistoricaldataBean> actuallBean = Historicaldata.get("6187.TWO", new Calendar.Builder().setDate(2010, 7, 11).build().getTime(),
				new Calendar.Builder().setDate(2010, 7, 14).build().getTime());

		// verify
		assertEquals(expectedBean.size(), actuallBean.size());
		assertTrue(actuallBean.containsAll(expectedBean));
	}
	
	@Test
	public void get_empty() throws IOException{
		// prepare
		String expectJson = "[{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-13\",\"Open\":\"54.177\","
				+ "\"High\":\"57.044\",\"Low\":\"54.177\",\"Close\":\"56.1542\",\"Volume\":\"3982200\","
				+ "\"Adj_Close\":\"53.4713\"},{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-12\",\"Open\":"
				+ "\"54.869\",\"High\":\"54.869\",\"Low\":\"53.6827\",\"Close\":\"54.0781\",\"Volume\":"
				+ "\"743400\",\"Adj_Close\":\"51.4944\"},{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-11\","
				+ "\"Open\":\"54.869\",\"High\":\"55.2645\",\"Low\":\"53.8804\",\"Close\":\"54.869\","
				+ "\"Volume\":\"829400\",\"Adj_Close\":\"52.2475\"}]";
		List<HistoricaldataBean> expectedBean = mapper.readValue(expectJson, TypeFactory.defaultInstance().constructCollectionType(List.class, HistoricaldataBean.class));
		PersistenceManager pm = pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		pm.makePersistentAll(expectedBean);
		pm.close();

		// action
		List<HistoricaldataBean> actuallBean = Historicaldata.get("6187.TWO", new Calendar.Builder().setDate(2012, 7, 11).build().getTime(),
				new Calendar.Builder().setDate(2012, 7, 14).build().getTime());

		// verify
		assertTrue(actuallBean.isEmpty());
	}
	
	@Test
	public void getLast() throws IOException{
		// prepare
		String expectJson = "[{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-13\",\"Open\":\"54.177\","
				+ "\"High\":\"57.044\",\"Low\":\"54.177\",\"Close\":\"56.1542\",\"Volume\":\"3982200\","
				+ "\"Adj_Close\":\"53.4713\"},{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-12\",\"Open\":"
				+ "\"54.869\",\"High\":\"54.869\",\"Low\":\"53.6827\",\"Close\":\"54.0781\",\"Volume\":"
				+ "\"743400\",\"Adj_Close\":\"51.4944\"},{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-11\","
				+ "\"Open\":\"54.869\",\"High\":\"55.2645\",\"Low\":\"53.8804\",\"Close\":\"54.869\","
				+ "\"Volume\":\"829400\",\"Adj_Close\":\"52.2475\"}]";
		List<HistoricaldataBean> expectedBean = mapper.readValue(expectJson, TypeFactory.defaultInstance().constructCollectionType(List.class, HistoricaldataBean.class));
		PersistenceManager pm = pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		pm.makePersistentAll(expectedBean);
		pm.close();

		// action
		HistoricaldataBean actuallBean = Historicaldata.getLast("6187.TWO");

		// verify
		assertEquals(expectedBean.get(0), actuallBean);
	}
	
	@Test
	public void getLast_null() throws IOException{
		// prepare

		// action
		HistoricaldataBean actuallBean = Historicaldata.getLast("6187.TWO");

		// verify
		assertNull(actuallBean);
	}
	
	@Test
	public void insert() throws IOException{
		// prepare
		String expectJson = "[{\"Symbol\":\"6187.TWO\",\"Date\":\"2010-08-13\",\"Open\":\"54.177\","
				+ "\"High\":\"57.044\",\"Low\":\"54.177\",\"Close\":\"56.1542\",\"Volume\":\"3982200\","
				+ "\"Adj_Close\":\"53.4713\"}]";

		// action
		List<HistoricaldataBean> expectedBean = mapper.readValue(expectJson, TypeFactory.defaultInstance().constructCollectionType(List.class, HistoricaldataBean.class));
		Historicaldata.insert(expectedBean);
		
		// verify
		PersistenceManager pm = pmf.getPersistenceManager();
		List<HistoricaldataBean> actualBean = (List<HistoricaldataBean>) pm.newQuery(HistoricaldataBean.class).execute();
		pm.close();
		assertEquals(expectedBean.size(), actualBean.size());
		assertTrue(actualBean.containsAll(expectedBean));
	}
	
	@Test
	public void getAllSymbol() throws JsonParseException, JsonMappingException, IOException{
		// prepare
		String json = "[{\"Symbol\":\"test1\",\"Date\":\"2010-08-13\"},{\"Symbol\":\"test2\",\"Date\":\"2010-08-13\"},"
				+ "{\"Symbol\":\"test1\",\"Date\":\"2010-08-14\"},{\"Symbol\":\"test2\",\"Date\":\"2010-08-14\"}]";
		List<HistoricaldataBean> beans = mapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, HistoricaldataBean.class));
		PersistenceManager pm = pmf.getPersistenceManager();
		pm.makePersistentAll(beans);
		pm.close();

		// action
		List<String>symbols=Historicaldata.getAllSymbol();
		logger.debug("symbols: {}", symbols);
		
		// verify
		assertEquals(2, symbols.size());
		assertTrue(symbols.contains("test1"));
		assertTrue(symbols.contains("test2"));
	}
}
