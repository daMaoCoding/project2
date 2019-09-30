package dc.pay.config;/**
 * Created by admin on 2017/6/6.
 */

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.HashMap;

/**
 * ************************
 * @author tony 3556239829
 */

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix="payProps")
public class PayProps {
    private static final Logger log =  LoggerFactory.getLogger(PayProps.class);
    @Value("${payProps.reqPayProxyServ:}")
    private  String  reqPayProxyServ;            //代理地址
    @Value("${payProps.enableReqPayinfoListToDb}")
    private boolean   enableReqPayinfoListInDb;  //开启请求支付记录
    @Value("${payProps.enableResPayinfoListToDb}")
    private boolean   enableResPayinfoListInDb;  //开启响应支付记录
    @Value("${payProps.enableReqDaifuInfoListToDb}")
    private boolean   enableReqDaifuInfoListToDb;  //开启请求代付记录
    @Value("${payProps.enableResDaiFuInfoListToDb}")
    private boolean   enableResDaiFuInfoListToDb;  //开启响应代付记录


    private   HashMap<String, String> dbInterfaceMap;

    private final String notifDbChannelSortUrl = "http://DATABASE_INTERFACE/payOwnerConfig/ownerChannelRateUpdate";
    private final String sendPayresultToRest="http://FORE_REST_INTERFACE/crk/toCrk";  //充值结果响应rest



    @PreDestroy
    public void  dostory(){
        log.error("======================PayStop================================");
        RunTimeInfo.printMemoryInfo();
    }

    public   HashMap<String, String> getDbInterfaceMap() {
        return dbInterfaceMap;
    }

    public  void setDbInterfaceMap(HashMap<String, String> dbInterfaceMap) {
        this.dbInterfaceMap = dbInterfaceMap;
    }

    public    String getReqPayInfoUrl(){
        return dbInterfaceMap.get("getReqpayInfo");
    }

    public    String getReqDaifuInfoUrl(){
        return dbInterfaceMap.get("getReqDaifuInfo");
    }

    public    String getNotifDbChannelSortUrl(){
        return StringUtils.isBlank(dbInterfaceMap.get("notifDbChannelSort"))?notifDbChannelSortUrl:dbInterfaceMap.get("notifDbChannelSort");   //通知数据库改变通道排序接口地址
    }

    public String getSendPayresultToRest() {
        return StringUtils.isBlank(dbInterfaceMap.get("sendPayresultToRest"))?sendPayresultToRest:dbInterfaceMap.get("sendPayresultToRest");   //通知rest充值成功
    }

    public  String getSendPayResultUrl(){
        return dbInterfaceMap.get("sendPayResult");
    }


    public  String getSendDaifuResultUrl(){
        return dbInterfaceMap.get("sendDaifuResult");
    }

    public  String getReqPayInfoHeader(){
        return dbInterfaceMap.get("getReqpayInfoHeader");
    }


    public  String getReqDaifuInfoHeader(){
        return dbInterfaceMap.get("getReqDaifuInfoHeader");
    }

    public boolean isEnableReqPayinfoListInDb() {
        return enableReqPayinfoListInDb;
    }

    public boolean isEnableResPayinfoListInDb() {
        return enableResPayinfoListInDb;
    }

    public String getReqPayProxyServ() {
        return reqPayProxyServ;
    }

    public boolean isEnableReqDaifuInfoListToDb() {
        return enableReqDaifuInfoListToDb;
    }

    public boolean isEnableResDaiFuInfoListToDb() {
        return enableResDaiFuInfoListToDb;
    }
}
