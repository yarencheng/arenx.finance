package arenx.finance.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import com.google.common.collect.Lists;

public class Test {

	public static void main(String[] args) throws InterruptedException {
		System.out.println("start ");
		
		
		RealMatrix m = new Array2DRowRealMatrix(4,4);
		
		
		m.setRow(0, new double[]{1, 1, 1, 1});
		m.setRow(1, new double[]{2, 2, 2, 2});
		m.setRow(2, new double[]{3, 3, 3, 3});
		m.setRow(3, new double[]{4, 4, 4, 5});
		
		System.out.println("m="+m);
		System.out.println("c="+new PearsonsCorrelation().computeCorrelationMatrix(m));
		
		
		m = new Array2DRowRealMatrix(3,4);
		
		
		m.setRow(0, new double[]{1, 1, 1, 1});
		m.setRow(1, new double[]{2, 2, 2, 2});
		m.setRow(2, new double[]{3, 3, 3, 3});
		
		System.out.println("m="+m);
		System.out.println("c="+new PearsonsCorrelation().computeCorrelationMatrix(m));
		
		Date d1 = new Date();
		Thread.sleep(1000);
		Date d2 = new Date();
		
		List<Date>l=new ArrayList();
		l.add(d1);
		l.add(d2);
//		l.stream().mapToDouble(d->0.0).s
		
		System.out.println(
				l.stream().sorted((dd1,dd2)->dd1.getTime()>dd2.getTime()?1:-1).collect(Collectors.toList())
				);
		
	}

}
