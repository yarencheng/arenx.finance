package arenx.finance.test;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class TestJDO {
	
	@PrimaryKey
	public String key;
	
	@Persistent
	public String value;

	@Override
	public String toString() {
		return "TestJDO [key=" + key + ", value=" + value + "]";
	}
}
