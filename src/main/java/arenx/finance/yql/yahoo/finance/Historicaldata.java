package arenx.finance.yql.yahoo.finance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.ObjectState;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import arenx.finance.PMF;
import arenx.finance.twse.TwseStock;
import arenx.finance.twse.TwseStockBean;
import arenx.finance.yql.YQL;

public class Historicaldata{

	private final static Logger logger = LoggerFactory.getLogger(Historicaldata.class);
	
	public static void updateDB() {
//		Lists.newArrayList("6187.TWO")	
		TwseStock.getFromDB().parallel()
			.map(TwseStockBean::getYahooFinanceStockID)
			.forEach(id->{
				HistoricaldataBean last = getLast(id);
				
				Date start = null;
				Date end = new Date();
				if(last==null){
					start = new Calendar.Builder().setDate(2005, 0, 1).build().getTime();
				}else{
					Calendar c = Calendar.getInstance();
					c.setTime(last.getDate());
					c.add(Calendar.DAY_OF_YEAR, 1);
					start = c.getTime();
				}
				
				updateDB(getFromYql(id,start,end).stream());
				
			});	
		
	}
	
	public static List<HistoricaldataBean> getFromYql(String symbol, Date start, Date end) {
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTime(start);
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTime(end);
		
		Calendar midCalendar = (Calendar) startCalendar.clone();
		midCalendar.add(Calendar.MONTH, 6);
		midCalendar.add(Calendar.DAY_OF_YEAR, -1);
		if(endCalendar.before(midCalendar)){
			List<HistoricaldataBean>list=getFromYql_halfyear(symbol,start,end);
			logger.debug("get {} records of '{}' between '{}' to '{}'", list.size(), symbol, start, end);
			return list;
		}
		List<HistoricaldataBean>list=new ArrayList<HistoricaldataBean>(500);
		while(endCalendar.after(startCalendar)){
			list.addAll(getFromYql_halfyear(symbol, startCalendar.getTime(), midCalendar.getTime()));
			startCalendar.add(Calendar.MONTH, 6);
			midCalendar.add(Calendar.MONTH, 6);
		}
		logger.info("get {} records of '{}' between '{}' to '{}'", list.size(), symbol, start, end);
		return list;
	}

	private static List<HistoricaldataBean> getFromYql_halfyear(String symbol, Date start, Date end) {
		logger.debug("symbol: {}, start{}, end:{}", symbol, start, end);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		HistoricaldataResults historicaldataResults = YQL.query(
			HistoricaldataResults.class,
			String.format(
				"select * from yahoo.finance.historicaldata where symbol='%s' and startDate='%s' and endDate='%s'",
				symbol,
				format.format(start),
				format.format(end)
			)
		);
		logger.debug("historicaldataResults: {}", historicaldataResults);
		if (historicaldataResults == null || historicaldataResults.quote == null) {
			return new ArrayList<HistoricaldataBean>();
		}
		return historicaldataResults.quote;
	}
	
	public static List<HistoricaldataBean> get(String symbol, Date start, Date end) {
		logger.debug("symbol: {}, start{}, end:{}", symbol, start, end);
		PersistenceManager pm = PMF.getPersistenceManager();
		try {
			Query query=pm.newQuery(HistoricaldataBean.class);
			query.setFilter("symbol==_symbol && date >= _dataStart && date <= _dataEnd");
			query.declareParameters("java.lang.String _symbol, java.util.Date _dataStart, java.util.Date _dataEnd");
			query.setOrdering("date desc");
			logger.debug("query: {}", query);
			List<HistoricaldataBean> beans =(List<HistoricaldataBean>) query.execute(symbol,start,end);
			return beans;
		} finally {
			pm.close();
		}
	}
	
	public static HistoricaldataBean getLast(String symbol) {
		logger.debug("symbol: {}", symbol);
		PersistenceManager pm = PMF.getPersistenceManager();
		try {
			Query query=pm.newQuery(HistoricaldataBean.class);
			query.setFilter("symbol==_symbol");
			query.declareParameters("java.lang.String _symbol");
			query.setOrdering("date desc");
			query.setRange(0, 1);
			logger.debug("query: {}", query);
			List<HistoricaldataBean> beans =(List<HistoricaldataBean>) query.execute(symbol);
			if(beans.isEmpty()){
				return null;
			}else{
				return beans.get(0);
			}
		} finally {
			pm.close();
		}
	}
	
//	public static void insert(List<HistoricaldataBean> beans){
//		updateDB(beans.stream());
//	}
	
	private static void updateDB(Stream<HistoricaldataBean> beans){
		PersistenceManager pm = PMF.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		
		try {
			beans.sequential().forEach(bean->{
				try {
					HistoricaldataBean oldBean = pm.getObjectById(HistoricaldataBean.class, new HistoricaldataBean.Key(bean.getSymbol(), bean.getDate()).toString());
					
					logger.info("update {} ${} {}", bean.getSymbol(), bean.getClose(), format.format(bean.getDate()));
					BeanUtils.copyProperties(oldBean, bean);
					oldBean.setUpdateDate(new Date());
					pm.makePersistent(oldBean);
				} catch (JDOObjectNotFoundException e) {
					logger.info("create {} ${} {}", bean.getSymbol(), bean.getClose(), format.format(bean.getDate()));
					bean.setUpdateDate(new Date());
					pm.makePersistent(bean);
				} catch (Exception e) {
					logger.error("Failed to update bean", e);
					return;
				}
			});
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw e;
		} finally {
			pm.close();
		}
	}
	
	public static List<String> getAllSymbol(){
		PersistenceManager pm = PMF.getPersistenceManager();
		try {
			Query query = pm.newQuery(HistoricaldataBean.class);
			query.setResult("symbol");
			query.setGrouping("symbol");
			List<String>list=(List<String>) query.execute();
			return list;
		} finally {
			pm.close();
		}
	}

	private static class HistoricaldataResults {
		@Override
		public String toString() {
			return "HistoricaldataResults [quote=" + quote + "]";
		}

		public List<HistoricaldataBean> quote;
	}

}

