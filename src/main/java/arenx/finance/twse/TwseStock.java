package arenx.finance.twse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.ObjectState;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.beanutils.BeanUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import arenx.finance.PMF;
import arenx.finance.Tool;

public class TwseStock {

	private final static Logger logger = LoggerFactory.getLogger(TwseStock.class);
	
	public static void updateDB(){
		updateDB(getFromTwscWebsite());
	}
		
	private static void updateDB(Stream<TwseStockBean> beans) {
		PersistenceManager pm = PMF.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		
		try {
			beans.sequential().forEach(bean->{				
				logger.debug("bean: {}", bean);
				
				try {
					TwseStockBean oldBean = pm.getObjectById(TwseStockBean.class, bean.getISINcode());
					
					logger.info("update #{} - {}", bean.getNumber(), bean.getName());
					BeanUtils.copyProperties(oldBean, bean);
					oldBean.setUpdateDate(new Date());
					pm.makePersistent(oldBean);
				} catch (JDOObjectNotFoundException e) {
					logger.info("create #{} - {}", bean.getNumber(), bean.getName());
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
	
	public static Stream<TwseStockBean> getFromTwscWebsite() {
		
		return Lists.newArrayList(
				"http://isin.twse.com.tw/isin/C_public.jsp?strMode=2",
				"http://isin.twse.com.tw/isin/C_public.jsp?strMode=4",
				"http://isin.twse.com.tw/isin/C_public.jsp?strMode=5")
			.parallelStream()
			.map(url->{
				logger.info("request url: {}", url);
				return Tool.getRestOperations().getForObject(url, String.class);
				})
			.flatMap(html->{
				Document doc = Jsoup.parse(html);
				Element table = doc.select("table").get(1);
				boolean isStockSection = false;
				SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
				List<TwseStockBean>beanList=new LinkedList<TwseStockBean>();
				
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
					TwseStockBean stock = new TwseStockBean(cols.get(1).text());
					
					String tokens[] = cols.get(0).text().split(" | | | | | |　", -1);
					logger.trace("tokens[{}]: {}", tokens.length, tokens);
					
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
					
					logger.debug("get stock: {} - {}", stock.getNumber(), stock.getName());
					logger.trace("stock: {} ", stock);
					beanList.add(stock);
				}
				
				logger.info("get {} items", beanList.size());
				
				return beanList.stream();
				
			});
	}

	public static Stream<TwseStockBean> getFromDB(){
		PersistenceManager pm=PMF.getPersistenceManager();
		pm.setDetachAllOnCommit(true);
		try {
			return ((List<TwseStockBean>) pm.newQuery(TwseStockBean.class).execute()).stream();
		} finally {
			pm.close();
		}
	}
}
