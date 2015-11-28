package arenx.finance.yql.yahoo.finance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.datanucleus.store.types.wrappers.backed.Collection;
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
		//List<String> failSymbols = updateRemoteToLocalDatabase(Stock.getALLStockIDForYahooFinance());
		List<Runnable>runnables=new LinkedList();
		for(String s:Stock.getALLStockIDForYahooFinance()){
		//for(String s:new String[]{"1314.tw","1315.tw","1315.tw"}){
			runnables.add(()->{
				updateRemoteToLocalDatabase(Collections.singletonList(s));
			});
		}
		return runnables;
	}

	public static HistoricaldataBean[] getFromRemote(String symbol, Date start, Date end) {
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
			return new HistoricaldataBean[0];
		}
		return historicaldataResults.quote;
	}

	/**
	 * 
	 * @param symbols
	 * @return list of symbols which fails to update
	 */
	public static List<String> updateRemoteToLocalDatabase(List<String> symbols) {
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
					HistoricaldataBean[] beans=null;
					try{
						beans=getFromRemote(symbol,startCalendar.getTime(),endCalendar.getTime());
					}catch (Throwable e){
						logger.warn("fail to update symbol: "+symbol);
						logger.debug("fail to update symbol: "+symbol,e);
						failSymbol.add(symbol);
						continue LOOP_SYMBOL;
					}
					logger.debug("beans.length: {}", beans.length);
					if(beans.length==0)
						break;
					logger.debug("beans[0]: {}, beans[{}]: {}", beans[0], beans.length-1, beans[beans.length-1]);
					pm.makePersistentAll(beans);
					count+=beans.length;
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
	}

	private static class HistoricaldataResults {
		@Override
		public String toString() {
			return "HistoricaldataResults [quote=" + Arrays.toString(quote) + "]";
		}

		public HistoricaldataBean[] quote;
	}

	
}

