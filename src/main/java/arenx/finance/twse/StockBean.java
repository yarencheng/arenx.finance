package arenx.finance.twse;

import java.util.Date;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(table="stock")
public class StockBean {
	@PrimaryKey
	private String ISINcode;
	@Persistent
	private Integer number;
	@Persistent
	private String name;
	@Persistent
	private Date startDate;
	@Persistent
	private String marketType;
	@Persistent
	private String industryType;
	@Persistent
	private String CFICode;

	public String getISINcode() {
		return ISINcode;
	}

	public void setISINcode(String iSINcode) {
		ISINcode = iSINcode;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public String getMarketType() {
		return marketType;
	}

	public void setMarketType(String marketType) {
		this.marketType = marketType;
	}

	public String getIndustryType() {
		return industryType;
	}

	public void setIndustryType(String industryType) {
		this.industryType = industryType;
	}

	public String getCFICode() {
		return CFICode;
	}

	public void setCFICode(String cFICode) {
		CFICode = cFICode;
	}

	@Override
	public String toString() {
		return "StockJDO [ISINcode=" + ISINcode + ", number=" + number + ", name=" + name + ", startDate="
				+ startDate + ", marketType=" + marketType + ", industryType=" + industryType + ", CFICode="
				+ CFICode + "]";
	}

}
