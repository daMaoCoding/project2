package dc.pay.entity.po;

import java.io.Serializable;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class AutoQueryDaifu implements Serializable {
    String serverId;
    String serverUrl;
    String key;
    String orderId;
    Long queryTime;
    int times;
    int totalTimes;
    Map<String,String> params;
    boolean mqInProcess;

    AutoQueryDaifu(){}

    public AutoQueryDaifu(String orderId,String key,String serverId, String serverUrl, Long queryTime, int times,int totalTimes ,Map<String, String> params, boolean mqInProcess) {
        this.serverId = serverId;
        this.serverUrl = serverUrl;
        this.queryTime = queryTime;
        this.times = times;
        this.params = params;
        this.mqInProcess = mqInProcess;
        this.key = key;
        this.orderId = orderId;
        this.totalTimes = totalTimes;
    }

    public Long getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(Long queryTime) {
        this.queryTime = queryTime;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public boolean isMqInProcess() {
        return mqInProcess;
    }

    public void setMqInProcess(boolean mqInProcess) {
        this.mqInProcess = mqInProcess;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getTotalTimes() {
        return totalTimes;
    }

    public void setTotalTimes(int totalTimes) {
        this.totalTimes = totalTimes;
    }
}
