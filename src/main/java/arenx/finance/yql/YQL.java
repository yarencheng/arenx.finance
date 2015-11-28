package arenx.finance.yql;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import arenx.finance.Tool;


public class YQL {
	
	private final static Logger logger = LoggerFactory.getLogger(YQL.class);
	
	private static PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("YahooYQL");
	public static PersistenceManager getPersistenceManager(){
		PersistenceManager pm = pmf.getPersistenceManager();
		return pm;
	}
	
	private static ObjectMapper mapper=new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
	public static <BEAN> BEAN query(Class<BEAN> clazz, String sql){
		logger.debug("sql: {}, clazz: {}", sql, clazz);
		String url = "https://query.yahooapis.com/v1/public/yql?q={sql}&format=json&env=store://datatables.org/alltableswithkeys&callback=";
		YQLBean yqlBean = Tool.getRestOperations().getForObject(url, YQLBean.class, sql);
		logger.debug("yqlBean: {}",yqlBean);
		BEAN newJsonNode = null;
		try {
			newJsonNode = mapper.treeToValue(yqlBean.getQuery().getResults(), clazz);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("fail to create '"+clazz+"' from '"+yqlBean.getQuery().getResults()+"'", e);
		}
		logger.debug("newJsonNode: {}", newJsonNode);
		return newJsonNode;
	}
	
}
