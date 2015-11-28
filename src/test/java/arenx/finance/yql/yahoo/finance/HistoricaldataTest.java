package arenx.finance.yql.yahoo.finance;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.util.Calendar;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HistoricaldataTest {

	private final static Logger logger = LoggerFactory.getLogger(HistoricaldataTest.class);

	ObjectMapper mapper=new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
	
	@Test
	public void getFromRemote() throws IOException {
		String expectJson = "[{\"Symbol\":\"YHOO\",\"Date\":\"2009-09-14\",\"Open\":\"15.45\","
				+ "\"High\":\"15.58\",\"Low\":\"15.28\",\"Close\":\"15.57\",\"Volume\":\"19451200\","
				+ "\"Adj_Close\":\"15.57\"},{\"Symbol\":\"YHOO\",\"Date\":\"2009-09-11\",\"Open\":\"15.53\","
				+ "\"High\":\"15.68\",\"Low\":\"15.41\",\"Close\":\"15.59\",\"Volume\":\"26860700\","
				+ "\"Adj_Close\":\"15.59\"}]";
		HistoricaldataBean[] expectedBean = mapper.readValue(expectJson, HistoricaldataBean[].class);

		HistoricaldataBean[] actuallBean = Historicaldata.getFromRemote("YHOO", new Calendar.Builder().setDate(2009, 8, 11).build().getTime(),
				new Calendar.Builder().setDate(2009, 8, 14).build().getTime());
		//logger.info("beans:{}", beans);
		assertArrayEquals(expectedBean, actuallBean);
	}
}
