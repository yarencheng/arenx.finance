package arenx.finance.yql;

import java.util.Date;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class YQLBean {
	private Query query;
	
	public final Query getQuery() {
		return query;
	}
	
	@Override
	public String toString() {
		return "YQLBean [query=" + query + "]";
	}

	public static final class Query{
		private long count;
		private Date created;
		@JsonProperty(value="lang")
		private Locale locale;
		@JsonProperty(value="results")
		private JsonNode results;
		public final long getCount() {
			return count;
		}
		public final Date getCreated() {
			return created;
		}
		public final Locale getLocale() {
			return locale;
		}
		public JsonNode getResults() {
			return results;
		}
		@Override
		public String toString() {
			return "Query [count=" + count + ", created=" + created + ", locale=" + locale + ", results=" + results
					+ "]";
		}
	}
}
