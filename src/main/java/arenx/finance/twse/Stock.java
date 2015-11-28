package arenx.finance.twse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import arenx.finance.Tool;
import arenx.finance.annotation.Updatable;

@Updatable
public class Stock {

	private final static Logger logger = LoggerFactory.getLogger(Stock.class);

	private static PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("TWSE");

	public static PersistenceManager getPersistenceManager() {
		PersistenceManager pm = pmf.getPersistenceManager();
		return pm;
	}
	
	@Updatable
	public static void updateDatabase() {
		PersistenceManager pm = getPersistenceManager();
		int count = 0;
		try {
			String[] urls = { "http://isin.twse.com.tw/isin/C_public.jsp?strMode=2",
					"http://isin.twse.com.tw/isin/C_public.jsp?strMode=4",
					"http://isin.twse.com.tw/isin/C_public.jsp?strMode=5"};
			for (String url : urls) {
				String html;
				html = Tool.getRestOperations().getForObject(url, String.class);
				logger.trace("result: {}", html);
				Document doc = Jsoup.parse(html);
				Element table = doc.select("table").get(1);
				boolean isStockSection = false;
				SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
				for (Element row : table.select("tr")) {
					if (row.childNodes().size() == 1) {
						if (row.text().equals("股票")) {
							isStockSection = true;
						} else {
							isStockSection = false;
						}
						continue;
					}
					if (isStockSection == false) {
						continue;
					}
					Elements cols = row.select("td");
					StockBean stock;
					try {
						stock = pm.getObjectById(StockBean.class, cols.get(1).text());
					} catch (JDOObjectNotFoundException e) {
						logger.info("create new stock: {}", cols.get(0).text());
						stock = new StockBean();
					}
					String tokens[] = cols.get(0).text().split(" | | | | | |　", -1);
					tokens = (String[]) Arrays.stream(tokens).filter(s -> s.length() > 0).toArray(String[]::new);
					stock.setNumber(Integer.parseInt(tokens[0]));
					stock.setName(tokens[1]);
					stock.setISINcode(cols.get(1).text());
					try {
						stock.setStartDate(format.parse(cols.get(2).text()));
					} catch (ParseException e) {
						logger.debug("cols:{} ", cols);
						throw new IllegalArgumentException("fail to parse " + cols.get(2).text() + " to date", e);
					}
					stock.setMarketType(cols.get(3).text());
					stock.setIndustryType(cols.get(4).text());
					stock.setCFICode(cols.get(5).text());
					try {
						pm.makePersistent(stock);
					} catch (Throwable e) {
						logger.error("fail to persist stock: " + stock, e);
						throw e;
					}
					count++;
					logger.debug("stock:{} ", stock);
				}
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw e;
		} finally {
			pm.close();
		}
		logger.info("update {} records", count);
	}

	public static List<String> getALLStockIDForYahooFinance() {
		PersistenceManager pm = getPersistenceManager();
		try {
			List<StockBean> results = (List<StockBean>) pm.newQuery(StockBean.class).execute();
			logger.debug("results.size()={}", results.size());
			return results.stream()
				.map(x -> {
					if(x.getMarketType().equals("上市")){
						return x.getNumber()+".TW";
					}else if(x.getMarketType().equals("上櫃")){
						return x.getNumber()+".TWO";
					}else if(x.getMarketType().equals("興櫃")){
						return x.getNumber()+".TWO";
					}else{
						throw new IllegalArgumentException("unknown market type: "+x.getMarketType());
					}
				})
				.collect(Collectors.toList());
		}finally{
			pm.close();
		}
	}

}
