package arenx.finance.statistics;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import arenx.finance.annotation.Updatable;
import arenx.finance.twse.Stock;
import arenx.finance.yql.yahoo.finance.Historicaldata;
import arenx.finance.yql.yahoo.finance.HistoricaldataBean;

//@Updatable(depedentClass = { Historicaldata.class })
@Updatable
public class StockStatistics {

	private final static Logger logger = LoggerFactory.getLogger(StockStatistics.class);

	@Updatable(retry=0)
	public static List<Runnable> update() {
		List<String> symbols = Historicaldata.getAllSymbol();
		List<Runnable> runnables = new LinkedList<Runnable>();
		for (String symbol : symbols) {
			runnables.add(new Runnable() {
				String name = "update " + symbol + " of statistics";

				public void run() {
					logger.info(name);
//					updateFromYahooFinanceHistoricaldata(symbol);
					updatePriceMean(symbol,30);
//					updatePriceVariance(symbol,30);
				}

				public String toString() {
					return name;
				}
			});
		}
		return runnables;
	}

	public static void updateFromYahooFinanceHistoricaldata(String symbol) {
		PersistenceManager pm = Statistics.getPersistenceManager();
		int count = 0;
		try {
			List<HistoricaldataBean> list = (List<HistoricaldataBean>) pm.newQuery(HistoricaldataBean.class, "symbol=='" + symbol + "'").execute();
			for (HistoricaldataBean history : list) {
				try {
					pm.getObjectById(StockStatisticsBean.class, new StockStatisticsBean.Key(history.getSymbol(), history.getDate()).toString());
					return;
				} catch (JDOObjectNotFoundException e) {
					// TODO: handle exception
				}
				StockStatisticsBean statistics = new StockStatisticsBean(history.getSymbol(), history.getDate());
				statistics.setPrice(history.getAdjClose());
				logger.debug("add statistics: {}", statistics);
				pm.makePersistent(statistics);
				count++;
			}
		} finally {
			pm.close();
		}
		logger.info("update {} record of '{}'", count, symbol);
	}

	public static void updatePriceMean(String symbol, int maxSampleSize) {
		Validate.isTrue(maxSampleSize>0,"maxSampleSize must be greater than 0");
		PersistenceManager pm = Statistics.getPersistenceManager();
//		PersistenceManager pm = JDOHelper.getPersistenceManagerFactory("arenx.finance").getPersistenceManager();
//		Transaction tx = pm.currentTransaction();
		try {
//			tx.begin();
			Query query = pm.newQuery(StockStatisticsBean.class);
			query.setFilter("symbol==_symbol && priceMean.size() < _maxSampleSize");
			query.setOrdering("date asc");
			query.declareParameters("java.lang.String _symbol, int _maxSampleSize");
			List<StockStatisticsBean> list = (List<StockStatisticsBean>) query.execute(symbol, maxSampleSize);
//			list = (List<StockStatisticsBean>) pm.detachCopyAll(list);
//			tx.commit();
			
//			if(1==1)return;
			logger.error("get list");
			
			TreeMap<Long,StockStatisticsBean> map = new TreeMap<Long,StockStatisticsBean>();
			for (StockStatisticsBean bean : list) {
				map.put(bean.getDate().getTime(), bean);
				Mean mean = new Mean();
				mean.increment(bean.getPrice());
				bean.getPriceMean().put(1, mean);
				if (maxSampleSize <= 1){
					continue;
				}
				
//				tx.begin();
				Query queryLast = pm.newQuery(StockStatisticsBean.class);
				queryLast.setFilter("symbol==_symbol && date < _date");
				queryLast.setOrdering("date desc");
				queryLast.declareParameters("java.lang.String _symbol, java.util.Date _date");
				queryLast.setRange(0, 1);
				List<StockStatisticsBean> lastBeans = (List<StockStatisticsBean>) queryLast.execute(symbol, bean.getDate());
//				lastBeans = (List<StockStatisticsBean>) pm.detachCopyAll(lastBeans);
//				logger.error("get list lastBeans");
//				tx.commit();
				
				if (lastBeans.isEmpty()) {
					continue;
				}
				StockStatisticsBean lastBean = map.getOrDefault(lastBeans.get(0).getDate().getTime(), lastBeans.get(0));
				new TreeMap<Integer, Mean>(lastBean.getPriceMean())
					.entrySet()
					.stream()
					.filter(e->e.getKey()<maxSampleSize)
					.forEach(e->{
						Mean _mean = e.getValue().copy();
						_mean.increment(bean.getPrice());
						bean.getPriceMean().put(1 + e.getKey(), _mean);
					});
			}
			
//			tx.begin();
//			pm.makePersistentAll(list);
//			tx.commit();
			
		} finally {
//			if(tx.isActive())
//				tx.rollback();
			pm.close();
		}
	}
	
	public static void updatePriceMean_(String symbol, int maxSampleSize) {
		
	}
	
	public static void updatePriceVariance(String symbol, int maxSampleSize) {
		Validate.isTrue(maxSampleSize>0,"maxSampleSize must be greater than 0");
		PersistenceManager pm = Statistics.getPersistenceManager();
		try {
			Query query = pm.newQuery(StockStatisticsBean.class);
			query.setFilter("symbol==_symbol && priceVariance.size() <= _maxSampleSize");
			query.setOrdering("date asc");
			query.declareParameters("java.lang.String _symbol, int _maxSampleSize");
			List<StockStatisticsBean> list = (List<StockStatisticsBean>) query.execute(symbol, maxSampleSize);
			for (StockStatisticsBean bean : list) {
				Variance variance = new Variance();
				variance.increment(bean.getPrice());
				bean.getPriceVariance().put(0, variance);
				if (maxSampleSize <= 1){
					continue;
				}
				Query queryLast = pm.newQuery(StockStatisticsBean.class);
				queryLast.setFilter("symbol==_symbol && date < _date");
				queryLast.setOrdering("date desc");
				queryLast.declareParameters("java.lang.String _symbol, java.util.Date _date");
				queryLast.setRange(0, 1);
				List<StockStatisticsBean> lastBeans = (List<StockStatisticsBean>) queryLast.execute(symbol, bean.getDate());
				if (lastBeans.isEmpty()) {
					continue;
				}
				StockStatisticsBean lastBean = lastBeans.get(0);
				new TreeMap<Integer, Variance>(lastBean.getPriceVariance())
					.entrySet()
					.stream()
					.filter(e->e.getKey()<maxSampleSize)
					.forEach(e->{
						Variance _variance = e.getValue().copy();
						_variance.increment(bean.getPrice());
						bean.getPriceVariance().put(1 + e.getKey(), _variance);
					});
			}
			pm.makePersistentAll(list);
		} finally {
			pm.close();
		}
	}

}
