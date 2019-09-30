package dc.pay.base.processor;

import dc.pay.config.RunTimeInfo;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.PropertiesUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
public final class ChannelWrapper{
    private static final Logger log =  LoggerFactory.getLogger(ChannelWrapper.class);
    private static final String  webInterfaceRespControllerName = PropertiesUtil.getProperty("WEB_INTERFACE_LOTTERY_PAY.RESP_PAY_CONTROLLER");
    private static final String  webInterfaceRespDaifuControllerName = PropertiesUtil.getProperty("WEB_INTERFACE_LOTTERY_PAY.RESP_DAIFU_CONTROLLER");
    private String API_KEY;
    private String API_PUBLIC_KEY;
    private String API_MEMBERID;
    private String API_MEMBER_PLATFORMID;
    private String API_AMOUNT;
    private String API_ORDER_ID;
    private String API_OrDER_TIME;
    private String API_CHANNEL_BANK_URL;
    private String API_CHANNEL_BANK_NAME;
    private String API_CHANNEL_BANK_NAME_FlAG;
    private String API_CHANNEL_BANK_NOTIFYURL;
    private String API_CHANNEL_SIGN_PARAM_NAME ;
    private String API_PAY_ORDER_STATUS;
    private String API_JUMP_URL_PREFIX;
    private String API_WEB_URL;
    private String API_OTHER_PARAM;
    private String API_Client_IP;
    private String API_OID;
    private String API_ORDER_FROM;
    private String API_CUSTOMER_ACCOUNT;
    private String API_CUSTOMER_NAME;
    private String API_CUSTOMER_BANK_NAME;
    private String API_CUSTOMER_BANK_BRANCH;
    private String API_CUSTOMER_BANK_SUB_BRANCH;
    private String API_CUSTOMER_BANK_NUMBER;
    private  Map<String, String> resParams;

    public Map<String, String> getResParams() {
        return resParams;
    }

    public void setResParams(Map<String, String> resParams) {
        this.resParams = resParams;
    }

    public String getAPI_ORDER_FROM() {
        return API_ORDER_FROM;
    }

    public void setAPI_ORDER_FROM(String API_ORDER_FROM) {
        this.API_ORDER_FROM = API_ORDER_FROM;
    }

    public String getAPI_OID() {
        return API_OID;
    }

    public void setAPI_OID(String API_OID) {
        this.API_OID = API_OID;
    }

    public String getAPI_Client_IP() {
        return API_Client_IP;
    }

    public void setAPI_Client_IP(String API_Client_IP) {
        this.API_Client_IP = API_Client_IP;
    }

    public String getAPI_PUBLIC_KEY() {
        return API_PUBLIC_KEY;
    }

    public void setAPI_PUBLIC_KEY(String API_PUBLIC_KEY) {
        this.API_PUBLIC_KEY = StringUtils.isBlank(API_PUBLIC_KEY)?"":API_PUBLIC_KEY;
    }

    public String getAPI_JUMP_URL_PREFIX() {
        return HandlerUtil.subUrl(this.API_JUMP_URL_PREFIX);
    }

    public void setAPI_JUMP_URL_PREFIX(String API_JUMP_URL_PREFIX) {
        this.API_JUMP_URL_PREFIX = StringUtils.isBlank(API_JUMP_URL_PREFIX)?"":API_JUMP_URL_PREFIX;
    }

    public String getAPI_WEB_URL() {
        return "http://www.baidu.com";  //拒绝透露网站地址给第三方，防止第三方黑箱操作自己账号
    }

    public void setAPI_WEB_URL(String API_WEB_URL) {
        this.API_WEB_URL = StringUtils.isBlank(API_WEB_URL)?"":API_WEB_URL;
    }

    public String getAPI_OTHER_PARAM() {
        return API_OTHER_PARAM;
    }

    public void setAPI_OTHER_PARAM(String API_OTHER_PARAM) {
        this.API_OTHER_PARAM = StringUtils.isBlank(API_OTHER_PARAM)?"":API_OTHER_PARAM;
    }

    public String getAPI_PAY_ORDER_STATUS() {
        return API_PAY_ORDER_STATUS;
    }

    public void setAPI_PAY_ORDER_STATUS(String API_PAY_ORDER_STATUS) {
        this.API_PAY_ORDER_STATUS = API_PAY_ORDER_STATUS;
    }

    public String getAPI_MEMBER_PLATFORMID() {
        return API_MEMBER_PLATFORMID;
    }

    public void setAPI_MEMBER_PLATFORMID(String API_MEMBER_PLATFORMID) {
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
    }

    public void setAPI_CHANNEL_BANK_NOTIFYURL(String API_CHANNEL_BANK_NOTIFYURL) {
        this.API_CHANNEL_BANK_NOTIFYURL = API_CHANNEL_BANK_NOTIFYURL;
    }

    /**
     * [**通道]-个性化订单号
     * @param API_ORDER_ID 业务系统订单号
     */
    public  void setAPI_ORDER_ID(String API_ORDER_ID){
        this.API_ORDER_ID = API_ORDER_ID;
    };

    /**
     * [**通道]-个性化订单时间
     * @param API_OrDER_TIME 业务系统订单时间(毫秒数)
     */
    public  void setAPI_OrDER_TIME(String API_OrDER_TIME){
        this.API_OrDER_TIME =API_OrDER_TIME;
    }

    /**
     *  [**通道]-获取全局通道中-具体通道银行名称
     * @param API_CHANNEL_BANK_NAME  全局通道名称
     */
    public  void setAPI_CHANNEL_BANK_NAME_FlAG(String API_CHANNEL_BANK_NAME){
        this.API_CHANNEL_BANK_NAME_FlAG = API_CHANNEL_BANK_NAME;
    }

    /**
     *  [**通道]-获取全局通道中-具体通道回调通知URL
     * @param API_CHANNEL_BANK_NAME  全局通道名称  ("pay_notifyurl" -> "http://localhost:8080/respPayWeb/EBOOPAY_BANK_WX_SM/")
     */
    public void setAPI_CHANNEL_BANK_NOTIFYURL(String NOTIFY_URL_PREFIX,String API_CHANNEL_BANK_NAME) {
        this.API_CHANNEL_BANK_NOTIFYURL = HandlerUtil.subUrl(NOTIFY_URL_PREFIX).concat(webInterfaceRespControllerName).concat(API_CHANNEL_BANK_NAME+"/");
    }



    public void setAPI_CHANNEL_BANK_NOTIFYURL(ReqDaifuInfo reqDaifuInfo) throws PayException {
        if(null!=reqDaifuInfo && StringUtils.isNotBlank(reqDaifuInfo.getAPI_NOTIFY_URL_PREFIX())  &&  StringUtils.isNotBlank(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME())){
            this.API_CHANNEL_BANK_NOTIFYURL = HandlerUtil.subUrl(reqDaifuInfo.getAPI_NOTIFY_URL_PREFIX()).concat(webInterfaceRespDaifuControllerName).concat(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()+"/");
        }
    }






    /**
     *  [**通道]-获取全局通道中-具体通道签名名称
     * @param API_CHANNEL_BANK_NAME  全局通道名称
     */
    public void setAPI_CHANNEL_SIGN_PARAM_NAME(String API_CHANNEL_BANK_NAME) {
        this.API_CHANNEL_SIGN_PARAM_NAME = API_CHANNEL_BANK_NAME;
    }


    /**
     *  [**通道]-获取全局通道中-具体通道签名名称
     * @param API_CHANNEL_BANK_NAME  提交请求URL
     */
    public void setAPI_CHANNEL_BANK_URL(String API_CHANNEL_BANK_NAME) {
        this.API_CHANNEL_BANK_URL = API_CHANNEL_BANK_NAME;
    }



    public String getAPI_CHANNEL_BANK_NOTIFYURL() {
        return API_CHANNEL_BANK_NOTIFYURL.trim();
    }

    public void setAPI_CHANNEL_BANK_NAME(String API_CHANNEL_BANK_NAME) {
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME.trim();
    }

    public String getAPI_CHANNEL_BANK_NAME() {
        return API_CHANNEL_BANK_NAME;
    }



    public String getAPI_CHANNEL_SIGN_PARAM_NAME() {
        return  API_CHANNEL_SIGN_PARAM_NAME;
    }


    public void setAPI_KEY(String API_KEY) {
        this.API_KEY  = API_KEY;
    }

    public void setAPI_MEMBERID(String API_MEMBERID) {
        this.API_MEMBERID = API_MEMBERID;
    }

    public void setAPI_AMOUNT(String API_AMOUNT) {
        this.API_AMOUNT  =API_AMOUNT;
    }

    public  String getAPI_CHANNEL_BANK_URL() {
        return API_CHANNEL_BANK_URL;
    }

    public String getAPI_KEY() {
        return API_KEY;
    }

    public String getAPI_MEMBERID() {
        return API_MEMBERID;
    }
 
    /**
     * 单位分
     * @return
     */
    public String getAPI_AMOUNT() {
        return API_AMOUNT;
    }


    public String getAPI_ORDER_ID() {
        return API_ORDER_ID;
    }

    /**
     * 毫秒数
     * @return
     */
    public String getAPI_OrDER_TIME() {
        return API_OrDER_TIME;
    }

    public String getAPI_CHANNEL_BANK_NAME_FlAG() {
        return API_CHANNEL_BANK_NAME_FlAG;
    }


    public String getAPI_CUSTOMER_ACCOUNT() {
        return API_CUSTOMER_ACCOUNT;
    }

    public void setAPI_CUSTOMER_ACCOUNT(String API_CUSTOMER_ACCOUNT) {
        this.API_CUSTOMER_ACCOUNT = API_CUSTOMER_ACCOUNT;
    }

    public String getAPI_CUSTOMER_NAME() {
        return API_CUSTOMER_NAME;
    }

    public void setAPI_CUSTOMER_NAME(String API_CUSTOMER_NAME) {
        this.API_CUSTOMER_NAME = API_CUSTOMER_NAME;
    }

    public String getAPI_CUSTOMER_BANK_NAME() {
        return API_CUSTOMER_BANK_NAME;
    }

    public void setAPI_CUSTOMER_BANK_NAME(String API_CUSTOMER_BANK_NAME) {
        this.API_CUSTOMER_BANK_NAME = API_CUSTOMER_BANK_NAME;
    }

    public String getAPI_CUSTOMER_BANK_BRANCH() {
        return API_CUSTOMER_BANK_BRANCH;
    }

    public void setAPI_CUSTOMER_BANK_BRANCH(String API_CUSTOMER_BANK_BRANCH) {
        this.API_CUSTOMER_BANK_BRANCH = API_CUSTOMER_BANK_BRANCH;
    }

    public String getAPI_CUSTOMER_BANK_SUB_BRANCH() {
        return API_CUSTOMER_BANK_SUB_BRANCH;
    }

    public void setAPI_CUSTOMER_BANK_SUB_BRANCH(String API_CUSTOMER_BANK_SUB_BRANCH) {
        this.API_CUSTOMER_BANK_SUB_BRANCH = API_CUSTOMER_BANK_SUB_BRANCH;
    }

    public String getAPI_CUSTOMER_BANK_NUMBER() {
        return API_CUSTOMER_BANK_NUMBER;
    }

    public void setAPI_CUSTOMER_BANK_NUMBER(String API_CUSTOMER_BANK_NUMBER) {
        this.API_CUSTOMER_BANK_NUMBER = API_CUSTOMER_BANK_NUMBER;
    }

}
