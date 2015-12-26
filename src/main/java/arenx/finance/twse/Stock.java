package arenx.finance.twse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.ObjectState;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import arenx.finance.Tool;
import arenx.finance.annotation.Updatable;

//@Updatable
public class Stock {

	private final static Logger logger = LoggerFactory.getLogger(Stock.class);

	private static PersistenceManagerFactory pmf = null;
	public static void setPersistenceManagerFactory(PersistenceManagerFactory pmf){
		Stock.pmf=pmf;
	}
	public static PersistenceManager getPersistenceManager() {
		if(pmf==null)
			pmf = JDOHelper.getPersistenceManagerFactory("arenx.finance");
		PersistenceManager pm = pmf.getPersistenceManager();
		return pm;
	}
	
//	@Updatable
	public static void update(){
		insert(getFromTwscWebsite(),true);
	}
	
	public static void insert(List<StockBean> beanList) {
		insert(beanList,false);
	}
	
	private static void insert(List<StockBean> beanList, boolean force) {
		PersistenceManager pm = getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		try {
			beanList.stream().forEach(bean->{
				if(force){
					if(JDOHelper.getObjectState(bean).equals(ObjectState.TRANSIENT)){
						try {
							StockBean oldBean = pm.getObjectById(StockBean.class, bean.getISINcode());
							pm.deletePersistent(oldBean);
						} catch (JDOObjectNotFoundException e) {
							
						}						
					}
				}
				pm.makePersistent(bean);
			});
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw e;
		} finally {
			pm.close();
		}
		logger.info("update {} records", beanList.size());
	}
	
	public static List<StockBean> getFromTwscWebsite() {
		List<StockBean>beanList=Collections.synchronizedList(new LinkedList<StockBean>());
		Lists.newArrayList(
				"http://isin.twse.com.tw/isin/C_public.jsp?strMode=2",
				"http://isin.twse.com.tw/isin/C_public.jsp?strMode=4",
				"http://isin.twse.com.tw/isin/C_public.jsp?strMode=5")
				.stream()
				.forEach(url->{
					String html = Tool.getRestOperations().getForObject(url, String.class);
					logger.trace("html: {}", html);
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
						StockBean stock = new StockBean(cols.get(1).text());
						String tokens[] = cols.get(0).text().split(" | | | | | |　", -1);
						tokens = (String[]) Arrays.stream(tokens).filter(s -> s.length() > 0).toArray(String[]::new);
						stock.setNumber(Integer.parseInt(tokens[0]));
						stock.setName(tokens[1]);
						try {
							stock.setStartDate(format.parse(cols.get(2).text()));
						} catch (ParseException e) {
							logger.debug("cols:{} ", cols);
							throw new IllegalArgumentException("fail to parse " + cols.get(2).text() + " to date", e);
						}
						stock.setMarketType(cols.get(3).text());
						stock.setIndustryType(cols.get(4).text());
						stock.setCFICode(cols.get(5).text());
						logger.trace("stock:{} ", stock);
						beanList.add(stock);
					}
				});

		logger.info("get {} records", beanList.size());
		return beanList;
	}

	public static List<String> getStockIDForYahooFinance(List<StockBean> list){
		return list.stream()
		.map((bean)->{
			if(bean.getMarketType().equals("上市")){
				return bean.getNumber()+".TW";
			}else if(bean.getMarketType().equals("上櫃")){
				return bean.getNumber()+".TWO";
			}else if(bean.getMarketType().equals("興櫃")){
				return bean.getNumber()+".TWO";
			}else{
				throw new IllegalArgumentException("unknown market type: "+bean.getMarketType());
			}
		}).collect(Collectors.toList());
	}
	
	public static List<StockBean> getAll(){
		PersistenceManager pm=pmf.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		try {
			return (List<StockBean>) pm.newQuery(StockBean.class).execute();
		} finally {
			pm.close();
		}
	}
}
