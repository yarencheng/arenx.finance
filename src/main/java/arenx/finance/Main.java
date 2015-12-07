package arenx.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	final static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] argv)  {
		Tool.createAllSchema();
		Tool.updateAll_();
		
	}
}
