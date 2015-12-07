package arenx.finance;

import java.io.File;
import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitTestTool {
	
	private final static Logger logger = LoggerFactory.getLogger(UnitTestTool.class);

	private static PersistenceManagerFactory pmf;
	public static PersistenceManagerFactory createNewPersistenceManagerFactory() {
		if(pmf!=null){
			pmf.close();
		}
		File file = new File("./target/unittest.mv.db");
		if(file.exists()){
			logger.debug("delete unittest database: {}({})",file.getAbsolutePath(), FileUtils.sizeOf(file));
			file.delete();
		}
		Properties newProperties = new Properties();
		newProperties.put("javax.jdo.option.ConnectionURL", "jdbc:h2:./target/unittest");
		newProperties.put("javax.jdo.option.ConnectionDriverName", "org.h2.Driver");
		newProperties.put("javax.jdo.option.ConnectionUserName", "");
		newProperties.put("javax.jdo.option.ConnectionPassword", "");
		newProperties.put("datanucleus.schema.autoCreateAll", "true");
		newProperties.put("datanucleus.schema.validateTables", "true");
		newProperties.put("datanucleus.schema.validateConstraints", "true");
		logger.debug("create dabase for unittest with property: {}", newProperties);
		pmf = JDOHelper.getPersistenceManagerFactory(newProperties);
		return pmf;
	}
}
