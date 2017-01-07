package arenx.finance;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.h2.store.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import arenx.finance.twse.TwseStock;
import arenx.finance.twse.TwseStockBean;
import arenx.finance.yql.yahoo.finance.Historicaldata;
import arenx.finance.yql.yahoo.finance.HistoricaldataBean;

public class Main {
	
	final static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws IOException, ClassNotFoundException  {
		
		Options options = new Options();
		
		options.addOption(Option.builder()
				.longOpt("log")
				.desc("Log options")
				.hasArg()
				.argName("level")
				.build());
		
		options.addOption(Option.builder()
				.longOpt("create_db_schema")
				.desc("初始化資料庫")
				.build());
		
		options.addOption(Option.builder()
				.longOpt("update_stock_from_twse")
				.desc("從台灣證交所更新股票代號")
				.build());
		
		options.addOption(Option.builder()
				.longOpt("update_price_from_yql")
				.desc("從Yahoo資料庫更新股價")
				.build());
		
		options.addOption(Option.builder()
				.longOpt("test")
				.desc("test")
				.build());
		
		if (0 == args.length) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "aaa", options );
			return;
		}
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args);
		} catch (ParseException e) {
			logger.error("failed to parse commands: {}", e.getMessage());
			return;
		}
		
		if (cmd.hasOption("log")) {
			String level = cmd.getOptionValue("log").toLowerCase();
			ch.qos.logback.classic.Logger l = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			
			if (level.equals("trace")) {
				l.setLevel(ch.qos.logback.classic.Level.TRACE);
			} else if (level.equals("debug")) {
				l.setLevel(ch.qos.logback.classic.Level.DEBUG);
			} else if (level.equals("info")) {
				l.setLevel(ch.qos.logback.classic.Level.INFO);
			} else if (level.equals("warn")) {
				l.setLevel(ch.qos.logback.classic.Level.WARN);
			} else if (level.equals("error")) {
				l.setLevel(ch.qos.logback.classic.Level.ERROR);
			} else {
				logger.warn("Unknown level: {}; use default: {}", cmd.getOptionValue("log"), l.getLevel());
			}
		}
		
		if (cmd.hasOption("create_db_schema")) {
			logger.info("create_db_schema");
			Tool.createAllSchema();
		}
		
		if (cmd.hasOption("update_stock_from_twse")) {
			logger.info("update_stock_from_twse");
			TwseStock.updateDB();
		}
		
		if (cmd.hasOption("update_price_from_yql")) {
			logger.info("update_price_from_yql");
			Historicaldata.updateDB();
		}

		if (cmd.hasOption("test")) {
			
			Date start = new Calendar.Builder().setDate(2014, 0, 1).build().getTime();
			Date end = new Calendar.Builder().setDate(2017, 0, 1).build().getTime();
			int data_size = 50;
			
			boolean test_mode = false;
			List<Integer>test_stocks_number = Lists.newArrayList(2382, 2330, 3030, 6351);
			
			// target_stock
			
			// date -> stock number -> price
			if (1!=1){
				TreeMap<Integer, TreeMap<Long, Double>> history_map_ =
					TwseStock.getFromDB()
					.filter(a-> !test_mode
							? true
							: test_stocks_number.contains(a.getNumber())
							)
					.peek(a->logger.info("start to get history of #{}{}", a.getNumber(), a.getName()))
					.map(a->new SimpleEntry<Integer, List<HistoricaldataBean>>(
							a.getNumber(),
							Historicaldata.getFromDB(a.getYahooFinanceStockID(), start, end)
							))
					.peek(a->logger.debug("start to collect history of #{} size={}", a.getKey(), a.getValue().size()))
					.collect(
							TreeMap<Integer, TreeMap<Long, Double>>::new,
							(tree,e)->{
								TreeMap<Long, Double> m = e.getValue()
									.stream()
									.collect(
											TreeMap<Long, Double>::new,
											(t,b)->t.put(b.getDate().getTime(), b.getClose()),
											(t1,t2)->t1.putAll(t2)
											);
								tree.put(e.getKey(), m);},
							(t1,t2)->{t1.putAll(t2);})
					;
				try {
					FileOutputStream fos = new FileOutputStream("history.data");
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					logger.info("start to write history.data");
					oos.writeObject(history_map_);
					oos.flush();
					oos.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (history_map_!=null) return;
			}
			
			FileInputStream fis = new FileInputStream("history.data");
			ObjectInputStream ois = new ObjectInputStream(fis);
			logger.info("start to read history.data");
			TreeMap<Integer, TreeMap<Long, Double>> history_map = (TreeMap<Integer, TreeMap<Long, Double>>) ois.readObject();
			ois.close();
			logger.info("finish to read history.data ");
			
			
			TwseStock.getFromDB()
				.parallel()
//				.filter(a->a.getNumber()==6187)
				.map(target_stock->{
			
					List<Integer> resultss =
					history_map
						.get(target_stock.getNumber())
						.keySet()
						.stream()
						.peek(date->{
							SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
							logger.debug("#{}{} {}", target_stock.getNumber(), target_stock.getName(),
									format.format(date));
						})
						.map(date->{
							TreeMap<Long, Double> target_history = new TreeMap<Long, Double>(history_map.get(target_stock.getNumber()));
							
							while(target_history.higherKey(date) != null) {
								target_history.pollLastEntry();
							}
							
							while(target_history.size()>data_size){
								target_history.pollFirstEntry();
							}
												
							return target_history;					
						})
						.filter(target_history->target_history.size()==data_size)
						.map(target_history->{
							double target_history_array[] = new double[data_size];
							long target_history_key_array[] = new long[data_size];
							
							Long lastkey = 0l;
							for(int i=0;i<target_history_array.length;i++){
								
								lastkey = target_history.higherKey(lastkey);
			
								target_history_key_array[i] = lastkey;
								target_history_array[i] = target_history.get(lastkey);
								
							}
							
							List<Entry<Double, Double>> results = 
								TwseStock.getFromDB()
								.filter(a-> !test_mode
										? a.getNumber()!=target_stock.getNumber()
										: test_stocks_number.contains(a.getNumber()) || a.getNumber()==target_stock.getNumber())
								.map(stock->{
									
									SimpleRegression sr =new SimpleRegression();
																
									for(int i=0;i<data_size;i++){
										Double x = history_map
												.get(stock.getNumber())
												.get(target_history_key_array[i]);
										if (x != null) {
											sr.addData(x, target_history_array[i]);
										}
										
									}
															
									Entry<TwseStockBean, SimpleRegression> r= new SimpleEntry<TwseStockBean, SimpleRegression> (stock, sr);
									
									return r;
								})
								.filter(entry->entry.getValue().getN()>data_size*0.9)
								.filter(entry->!Double.isNaN(entry.getValue().getRSquare()))
								.filter(entry->entry.getValue().getRSquare() >= 0.8)
								.sorted((e1,e2)->-Double.compare(e1.getValue().getRSquare(), e2.getValue().getRSquare()))
								.limit(10)
								.peek(entry->{
									SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
									logger.debug("#{}{} {} #{}{} {}({})", target_stock.getNumber(), target_stock.getName(),
											format.format(new Date(target_history.lastKey())),
											entry.getKey().getNumber(), entry.getKey().getName(),
											entry.getValue().getRSquare(),
											entry.getValue().getN());
								})
								
								.map(entry->{
									
									double r2 = entry.getValue().getRSquare();
									Double x = history_map.get(entry.getKey().getNumber()).get(target_history_key_array[data_size-1]);
									if(x==null){
										x=Double.NaN;
									}
									double y=entry.getValue().predict(x);
									
									
									return new SimpleEntry<Double, Double> (r2, y);
								})
								
								.collect(Collectors.toList())
								;
							
							double y_next = history_map.get(target_stock.getNumber()).higherEntry(target_history_key_array[data_size-1]) == null
									? Double.NaN : history_map.get(target_stock.getNumber()).higherEntry(target_history_key_array[data_size-1]).getValue();
							
							double y = target_history_array[data_size-1];
							
							long all_count = results
									.stream()
									.filter(e->!Double.isNaN(e.getValue()))
									.count();
							
							long more_count = results
									.stream()
									.filter(e->!Double.isNaN(e.getValue()))
									.filter(e->e.getValue() > y)
									.count();
							
							long less_count = results
									.stream()
									.filter(e->!Double.isNaN(e.getValue()))
									.filter(e->e.getValue() < y)
									.count();
							
							long nan_count = results
									.stream()
									.filter(e->Double.isNaN(e.getValue()))
									.count();
							
							int result =
									(Double.isNaN(y) || Double.isNaN(y_next)) ? 0 :
									(all_count < 10) ? 0 :
									(more_count >= 10) ? (y_next > y) ? 1 : -1 :
									(less_count >= 10) ? (y_next < y) ? 1 : -1 : 0;
												
							SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
							logger.debug("#{}{} {} y={}/{} result={} {}/{}/{}/{}",
									target_stock.getNumber(), target_stock.getName(),
									format.format(new Date(target_history.lastKey())),
									y,y_next,							
									result,
									more_count, less_count, nan_count, all_count);
							
							return result;
							
							
						})
						.collect(Collectors.toList());
					
					long all = resultss.size();
					long not = resultss.stream().mapToInt(a->a).filter(a->a==-1).count();
					long nan = resultss.stream().mapToInt(a->a).filter(a->a==0).count();
					long yes = resultss.stream().mapToInt(a->a).filter(a->a==1).count();
					
					logger.info("#{}{} {} {}/{}/{}", target_stock.getNumber(), target_stock.getName(), all, yes, not, nan);
					
					return new SimpleEntry<TwseStockBean, Double>(target_stock, new Double(yes)*100/(yes+not));
			
				})
//				.sequential()
				.sorted((a,b)->Double.compare(a.getValue(), b.getValue()))
				.forEach(e->{
					logger.info("#{}{} {}%", e.getKey().getNumber(), e.getKey().getName(),e.getValue());
				});
				;
				
		}
	}
}
