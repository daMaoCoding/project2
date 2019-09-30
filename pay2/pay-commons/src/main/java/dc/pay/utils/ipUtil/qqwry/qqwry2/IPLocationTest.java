package dc.pay.utils.ipUtil.qqwry.qqwry2;

import dc.pay.utils.ipUtil.qqwry.entry.Location;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class IPLocationTest extends TestCase {
	private static String ipdataPath="/ipdata/qqwry.dat";
    public void testThreadSafe() throws Exception {
    	final IPLocation ipl = new IPLocation(IPLocation.class.getResource(ipdataPath).getPath());
		int num = 4;
		ExecutorService es = Executors.newFixedThreadPool(num);
		long start = System.currentTimeMillis();
		for (int i = 0; i < num;i++) {
			es.execute(new Runnable(){
				final Random rd = new Random();
				public void run(){
					int n = 10000;
					for(int i = 0;i < n;i++){
						String ip = (rd.nextInt(253) + 1) + "." + rd.nextInt(255) + "." + rd.nextInt(255) + "." + (rd.nextInt(253) + 1);	
						ipl.fetchIPLocation(ip);
					}
				}
			});
		}
		es.shutdown();
		while(!es.isTerminated()){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(System.currentTimeMillis() - start);
	}
    
    public void testLocation() throws Exception { 
    	final IPLocation ipLocation = new IPLocation(IPLocation.class.getResource(ipdataPath).getPath());
    	 Location loc = ipLocation.fetchIPLocation("255.92.240.50");
				  loc = ipLocation.fetchIPLocation("112.224.2.128");
				  loc = ipLocation.fetchIPLocation("124.104.195.61");
    	Assert.assertNotNull(loc);
		System.out.printf("%s %s",loc.country,loc.area);
    }
}
