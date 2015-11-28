package arenx.finance.yql.yahoo.finance;

import java.util.Date;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.fasterxml.jackson.annotation.JsonProperty;

@PersistenceCapable(table="historicaldata")
public class HistoricaldataBean {
	public String getSymbol() {
		return symbol;
	}

	public Date getDate() {
		return date;
	}

	public double getOpen() {
		return open;
	}

	public double getHigh() {
		return high;
	}

	public double getLow() {
		return low;
	}

	public double getClose() {
		return close;
	}

	public long getVolume() {
		return volume;
	}

	public double getAdjClose() {
		return adjClose;
	}

	@JsonProperty(value = "Symbol")
	@PrimaryKey
	private String symbol;
	@PrimaryKey
	@JsonProperty(value = "Date")
	private Date date;
	@JsonProperty(value = "Open")
	@Persistent
	private Double open;
	@JsonProperty(value = "High")
	@Persistent
	private Double high;
	@JsonProperty(value = "Low")
	@Persistent
	private Double low;
	@JsonProperty(value = "Close")
	@Persistent
	private Double close;
	@JsonProperty(value = "Volume")
	@Persistent
	private Long volume;
	@JsonProperty(value = "Adj_Close")
	@Persistent
	private Double adjClose;

	@Override
	public String toString() {
		return "HistoricaldataBean [symbol=" + symbol + ", date=" + date + ", open=" + open + ", high=" + high
				+ ", low=" + low + ", close=" + close + ", volume=" + volume + ", adjClose=" + adjClose + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(adjClose);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(close);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		temp = Double.doubleToLongBits(high);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(low);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(open);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		result = prime * result + (int) (volume ^ (volume >>> 32));
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
		HistoricaldataBean other = (HistoricaldataBean) obj;
		if (Double.doubleToLongBits(adjClose) != Double.doubleToLongBits(other.adjClose))
			return false;
		if (Double.doubleToLongBits(close) != Double.doubleToLongBits(other.close))
			return false;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (Double.doubleToLongBits(high) != Double.doubleToLongBits(other.high))
			return false;
		if (Double.doubleToLongBits(low) != Double.doubleToLongBits(other.low))
			return false;
		if (Double.doubleToLongBits(open) != Double.doubleToLongBits(other.open))
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		if (volume != other.volume)
			return false;
		return true;
	}
}
