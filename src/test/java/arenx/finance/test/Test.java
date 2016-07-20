package arenx.finance.test;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

public class Test {

	public static void main(String[] args) {
		System.out.println("start ");
		// TODO Auto-generated method stub
		
		List<Integer>l=new ArrayList();

		for (int i = 0; i < 10; i++) {
			l.add(i);
		}
		
		l.parallelStream()
			
			.filter((i)->{
				System.out.println("filter 1 start i="+i);
				try {
					if(i%3==0)Thread.sleep(000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("filter 1 end i="+i);
				return true;
			})
			
			.flatMap(a->{
				
				return Lists.newArrayList(1).stream();
				})
			
//			.collect(ArrayList<Integer>::new,
//					(a,i)->{
//						System.out.println("collect 1 add start i="+i);
//						a.add(i);
//					},
//                    ArrayList::addAll)
//			.parallelStream()
			.filter((i)->{
				System.out.println("filter 2 start i="+i);
				try {
					if(i%2==0)Thread.sleep(1000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("filter 2 end i="+i);
				return true;
			})
			.collect(ArrayList<Integer>::new,
					(a,i)->{
						System.out.println("collect 2 add start i="+i);
						a.add(i);
					},
                    ArrayList::addAll);
		
	}

}
