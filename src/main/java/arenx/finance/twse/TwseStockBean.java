package arenx.finance.twse;

import java.util.Date;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(table="stock",detachable="true")
public class TwseStockBean {
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
	@Persistent
	private Date updateDate;

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public TwseStockBean(String ISINcode){
		this.ISINcode = ISINcode;
	}
		
	public String getISINcode() {
		return ISINcode;
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
	
	public String getYahooFinanceStockID(){
		if(getMarketType().equals("上市")){
			return getNumber()+".TW";
		}else if(getMarketType().equals("上櫃")){
			return getNumber()+".TWO";
		}else if(getMarketType().equals("興櫃")){
			return getNumber()+".TWO";
		}else{
			throw new IllegalArgumentException("unknown market type: "+getMarketType());
		}
	}

	@Override
	public String toString() {
		return "TwseStockBean [ISINcode=" + ISINcode + ", number=" + number + ", name=" + name + ", startDate="
				+ startDate + ", marketType=" + marketType + ", industryType=" + industryType + ", CFICode=" + CFICode
				+ ", updateDate=" + updateDate + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((CFICode == null) ? 0 : CFICode.hashCode());
		result = prime * result + ((ISINcode == null) ? 0 : ISINcode.hashCode());
		result = prime * result + ((industryType == null) ? 0 : industryType.hashCode());
		result = prime * result + ((marketType == null) ? 0 : marketType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((number == null) ? 0 : number.hashCode());
		result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TwseStockBean other = (TwseStockBean) obj;
		if (CFICode == null) {
			if (other.CFICode != null)
				return false;
		} else if (!CFICode.equals(other.CFICode))
			return false;
		if (ISINcode == null) {
			if (other.ISINcode != null)
				return false;
		} else if (!ISINcode.equals(other.ISINcode))
			return false;
		if (industryType == null) {
			if (other.industryType != null)
				return false;
		} else if (!industryType.equals(other.industryType))
			return false;
		if (marketType == null) {
			if (other.marketType != null)
				return false;
		} else if (!marketType.equals(other.marketType))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (number == null) {
			if (other.number != null)
				return false;
		} else if (!number.equals(other.number))
			return false;
		if (startDate == null) {
			if (other.startDate != null)
				return false;
		} else if (!startDate.equals(other.startDate))
			return false;
		return true;
	}

}
