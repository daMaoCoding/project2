package dc.pay.utils.ipUtil.qqwry.qqwry3.test;

import dc.pay.utils.ipUtil.qqwry.entry.IPEntry;
import dc.pay.utils.ipUtil.qqwry.qqwry3.ipparse.IPSeeker;
import dc.pay.utils.ipUtil.qqwry.qqwry3.util.AreaSplit;
import junit.framework.TestCase;

public class IPTest extends TestCase {

    private static String ipdataPath="/ipdata/qqwry.dat";
    public static IPSeeker ipData =new IPSeeker(IPTest.class.getResource(ipdataPath).getPath());

    public void testIp(){
        //指定纯真数据库的文件名，所在文件夹
        String ipStr = "223.104.146.32";
                ipStr = "211.21.55.11";
                ipStr = "112.224.2.128";
                ipStr = "202.202.0.1";
        String country = ipData.getIPLocation(ipStr).getCountry();
        String areas  = ipData.getIPLocation(ipStr).getArea();
        System.out.println("国家： "+country);
        System.out.println("地区： "+areas);

        System.out.println("-------------------------------");


        IPEntry ipEntry =  new AreaSplit().getArea(country);
        System.out.println(ipEntry.getAreaSplited());
        System.out.println("flage : " +ipEntry.getFlage());


    }





}
