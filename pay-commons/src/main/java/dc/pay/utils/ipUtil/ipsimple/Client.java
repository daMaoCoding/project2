package dc.pay.utils.ipUtil.ipsimple;


public class Client {
    public static void main(String[] args) {
        String ip = "58.30.15.255";
        String region = IpHelper.findRegionByIp(ip);
        System.out.println(region);
    }

}
