package arenx.finance;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import arenx.finance.twse.TwseStock;
import arenx.finance.yql.yahoo.finance.Historicaldata;

public class Main {
	
	final static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args)  {
		
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

		
//		Tool.createAllSchema();
//		Tool.updateAll_();
		//Stock.update();
	}
}
