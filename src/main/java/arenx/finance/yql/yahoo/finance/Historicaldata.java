package arenx.finance.yql.yahoo.finance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.ObjectState;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import arenx.finance.annotation.Updatable;
import arenx.finance.twse.Stock;
import arenx.finance.yql.YQL;

@Updatable(depedentClass={Stock.class})
public class Historicaldata{

	private final static Logger logger = LoggerFactory.getLogger(Historicaldata.class);
	
	@Updatable
	public static List<Runnable> updateDatabase() {
		List<Runnable>runnables=new LinkedList();
		for(String symbol:Stock.getStockIDForYahooFinance(Stock.getAll())){
//		for(String symbol:new String[]{"6187.TWO"}){
			runnables.add(new Runnable(){
				String name;
				public void run(){
					HistoricaldataBean last = getLast(symbol);
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
					
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					name = String.format("yahoo.finance.historical: update '%s' between %s~%s", symbol, format.format(start), format.format(end));
					logger.info("start {}", name);
					
					List<HistoricaldataBean> beans = getFromYql(symbol,start,end);
					insert(beans);
				}
				public String toString(){
					return name;
				}
			});
		}
		return runnables;
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
			logger.info("get {} records of '{}' between '{}' to '{}'", list.size(), symbol, start, end);
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
		PersistenceManager pm = YQL.getPersistenceManager();
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
		PersistenceManager pm = YQL.getPersistenceManager();
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
	
	public static void insert(List<HistoricaldataBean> beans){
		insert(beans,false);
	}
	
	private static void insert(List<HistoricaldataBean> beans, boolean force){
		PersistenceManager pm = YQL.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		Transaction tx = pm.currentTransaction();
		try {
			tx.begin();
			beans.forEach(bean->{
				if(force){
					if(JDOHelper.getObjectState(bean).equals(ObjectState.TRANSIENT)){
						try {
							HistoricaldataBean oldBean = pm.getObjectById(HistoricaldataBean.class, new HistoricaldataBean.Key(bean.getSymbol(), bean.getDate()).toString());
							pm.deletePersistent(oldBean);
						} catch (JDOObjectNotFoundException e) {
							
						}						
					}
				}
				pm.makePersistent(bean);
			});
			tx.commit();
		} finally {
			if (tx.isActive())
		    {
		        tx.rollback();
		    }
			pm.close();
		}
	}

	/**
	 * 
	 * @param symbols
	 * @return list of symbols which fails to update
	 */
	/*public static List<String> updateRemoteToLocalDatabase(List<String> symbols) {
		int count = 0;
		List<String>failSymbol=new ArrayList<String>();
		PersistenceManager pm = YQL.getPersistenceManager();
		try{
			Calendar nowCalendar = Calendar.getInstance();
			nowCalendar.add(Calendar.DAY_OF_YEAR, 2);
			LOOP_SYMBOL:
			for(String symbol:symbols){
				logger.info("update symbol: {}", symbol);
				Query query=pm.newQuery(HistoricaldataBean.class);
				query.setFilter(String.format("symbol=='%s'", symbol));
				query.setOrdering("date desc");
				query.setRange(0, 1);
				query.setResult("date");
				logger.debug("query: {}", query);
				List<Date> dates= (List<Date>) query.execute();
				logger.debug("dates: {}", dates);
				Calendar startCalendar;
				Calendar endCalendar;
				if(dates.isEmpty()){
					startCalendar = new Calendar.Builder().setDate(2010, 0, 1).build();
					endCalendar = new Calendar.Builder().setDate(2010, 6, 30).build();
				}else{
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(dates.get(0));
					calendar.add(Calendar.DAY_OF_YEAR, 1);
					startCalendar = (Calendar) calendar.clone();
					calendar.add(Calendar.MONTH, 6);
					endCalendar = (Calendar) calendar.clone();
				}
				while(startCalendar.before(nowCalendar)){
					logger.debug("startCalendar: {}, endDate: {}", startCalendar.getTime(), endCalendar.getTime());
					List<HistoricaldataBean> beans=null;
					try{
						beans=getFromYql(symbol,startCalendar.getTime(),endCalendar.getTime());
					}catch (Throwable e){
						logger.warn("fail to update symbol: "+symbol);
						logger.debug("fail to update symbol: "+symbol,e);
						failSymbol.add(symbol);
						continue LOOP_SYMBOL;
					}
					logger.debug("beans.length: {}", beans.size());
					if(beans.size()==0)
						break;
					if(logger.isDebugEnabled())
						logger.debug("beans[0]: {}, beans[{}]: {}", beans.get(0), beans.size()-1, beans.get(beans.size()-1));
					pm.makePersistentAll(beans);
					count+=beans.size();
					startCalendar = (Calendar) endCalendar.clone();
					startCalendar.add(Calendar.DAY_OF_YEAR, 1);
					endCalendar.add(Calendar.MONTH, 6);
				}
			}
		}catch (Throwable e){
			logger.error(e.getMessage());
			throw e;
		}finally{
			logger.info("update {} records from sybols: {}", count, symbols);
			pm.close();
		}
		return failSymbol;
	}*/

	private static class HistoricaldataResults {
		@Override
		public String toString() {
			return "HistoricaldataResults [quote=" + quote + "]";
		}

		public List<HistoricaldataBean> quote;
	}

}

