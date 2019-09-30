package dc.pay.utils.ipUtil.qqwry.qqwry3;

import dc.pay.utils.ipUtil.qqwry.entry.IPEntry;
import dc.pay.utils.ipUtil.qqwry.qqwry3.ipparse.IPSeeker;
import dc.pay.utils.ipUtil.qqwry.qqwry3.util.AreaSplit;

import java.io.IOException;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class IpHelperCZ {
    private static String ipdataPath="/ipdata/qqwry.dat";
    public static IPSeeker ipData =new IPSeeker(ipdataPath);

    //带有详细地址
    public static String   findStrAddressWithAreas(String ipNumStr){
        String country = ipData.getIPLocation(ipNumStr).getCountry();
        String areas  = ipData.getIPLocation(ipNumStr).getArea();
        IPEntry ipEntry =  new AreaSplit().getArea(country);
        return ipEntry.getAreaSplited().concat("|").concat(areas);
        // System.out.println("flage : " +ipEntry.getFlage());
    }

    //带有详细地址
    public static String[]   findArrayAddressWithAreas(String ipNumStr){
        return findStrAddressWithAreas(ipNumStr).split("\\|");
    }


    //NO-带有详细地址
    public static String   findStrAddress(String ipNumStr){
        String country = ipData.getIPLocation(ipNumStr).getCountry();
        return new AreaSplit().getArea(country).getAreaSplited();
    }


    //取省或国家，或者全部
    public static String getAddresByip(String ip){
        String strAddress = IpHelperCZ.findStrAddress(ip);
        switch (strAddress.split("\\|").length) {
            case 1:
                return strAddress.split("\\|")[0];
            case 2:
                return strAddress.split("\\|")[1];
            case 3:
                return strAddress.split("\\|")[1];
            case 4:
                return strAddress.split("\\|")[1];
            case 5:
                return strAddress.split("\\|")[1];
            default:
                return strAddress;
        }
    }


    //NO-带有详细地址
    public static String[]   findArrayAddress(String ipNumStr){
        return findStrAddress(ipNumStr).split("\\|");
    }


    public static void main(String[] args) {
        System.out.println(IpHelperCZ.class.getResource(ipdataPath).getPath());
        System.out.println(findStrAddress("10.10.1.1"));
    }



}
