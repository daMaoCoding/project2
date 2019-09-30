package dc.pay.payrest;

import java.util.Set;
import java.util.TreeSet;

public class PayRestDevConFig {

    private Set<String> blackIpList = new TreeSet<>();

    private PayRestDevConFig(){}
    public static PayRestDevConFig getInstance(){
        return PayRestDevConFigHolder.sInstance;
    }

    private static class PayRestDevConFigHolder {
        private static final PayRestDevConFig sInstance = new PayRestDevConFig();
    }

    public Set<String> getBlackIpList() {
        return this.blackIpList;
    }

    public void addBlackIp(String ip) {
        this.blackIpList.add(ip);
    }

    public void cleanBlackIp(){
        this.blackIpList.clear();
    }

    public boolean isBlackIp(String ip){
        return getBlackIpList().contains(ip);
    }

    
}