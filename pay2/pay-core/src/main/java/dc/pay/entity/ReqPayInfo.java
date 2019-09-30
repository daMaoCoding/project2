package dc.pay.entity;

/**
 * ************************
 *
 * @author tony 3556239829
 */

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Maps;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Map;

public class ReqPayInfo implements Serializable{

    private String API_KEY;
    private String API_PUBLIC_KEY;
    private String API_MEMBERID;
    private String API_MEMBER_PLATFORMID;
    private volatile String API_AMOUNT;
    private String API_ORDER_ID;
    private String API_OrDER_TIME;
    private String API_CHANNEL_BANK_NAME;
    private String API_TIME_OUT;
    private String API_ORDER_STATE;
    private String API_NOTIFY_URL_PREFIX;
    private String API_SEQUENCE_NUMBER;
    private String API_JUMP_URL_PREFIX;
    private String API_WEB_URL;
    private String API_OTHER_PARAM;
    private String API_Client_IP;
    private String API_OID;
    private String API_ORDER_FROM;

    @JSONField(serialize=false)
    private Map<String,String> resParams = Maps.newHashMap();

    private ReqPayInfo() {
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
        if(StringUtils.isNotBlank(API_Client_IP) && HandlerUtil.igonCliIP.contains(API_Client_IP)) return  HandlerUtil.getRandomIp(null);  // HandlerUtil.DEFAULT_API_Client_IP;
        return API_Client_IP;
    }

    public void setAPI_Client_IP(String API_Client_IP) {
        this.API_Client_IP = API_Client_IP;
    }

    public ReqPayInfo(String API_ORDER_ID) {
        this.API_ORDER_ID = API_ORDER_ID;
    }

    public ReqPayInfo(String API_KEY, String API_MEMBERID, String API_AMOUNT, String API_ORDER_ID, String API_OrDER_TIME, String API_CHANNEL_BANK_NAME, String API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX) {
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_OrDER_TIME = API_OrDER_TIME;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
    }


    public ReqPayInfo(String API_KEY, String API_MEMBERID, String API_AMOUNT, String API_ORDER_ID, String API_OrDER_TIME, String API_CHANNEL_BANK_NAME, String API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX,String API_MEMBER_PLATFORMID) {
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_OrDER_TIME = API_OrDER_TIME;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
    }




    public String getAPI_MEMBER_PLATFORMID() {
        return API_MEMBER_PLATFORMID;
    }

    public void setAPI_MEMBER_PLATFORMID(String API_MEMBER_PLATFORMID) {
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
    }

    public String getAPI_NOTIFY_URL_PREFIX() {
        return API_NOTIFY_URL_PREFIX;
    }

    public void setAPI_NOTIFY_URL_PREFIX(String API_NOTIFY_URL_PREFIX) {
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
    }

    public String getAPI_KEY() {
        return  StringUtils.isNotBlank(API_KEY)?API_KEY.replaceAll("\\s*", ""):"";
    }

    public void setAPI_KEY(String API_KEY) {
        this.API_KEY = API_KEY;
    }

    public String getAPI_MEMBERID() {
        return API_MEMBERID;
    }

    public void setAPI_MEMBERID(String API_MEMBERID) {
        this.API_MEMBERID = API_MEMBERID;
    }

    public String getAPI_AMOUNT() {
        return API_AMOUNT;
    }

    public void setAPI_AMOUNT(String API_AMOUNT) {
        this.API_AMOUNT = API_AMOUNT;
    }

    public String getAPI_ORDER_ID() {
        return API_ORDER_ID;
    }

    public void setAPI_ORDER_ID(String API_ORDER_ID) {
        this.API_ORDER_ID = API_ORDER_ID;
    }

    public String getAPI_OrDER_TIME() {
        return API_OrDER_TIME;
    }

    public void setAPI_OrDER_TIME(String API_OrDER_TIME) {
        this.API_OrDER_TIME = API_OrDER_TIME;
    }

    public String getAPI_CHANNEL_BANK_NAME() {
        return API_CHANNEL_BANK_NAME;
    }

    public void setAPI_CHANNEL_BANK_NAME(String API_CHANNEL_BANK_NAME) {
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
    }

    public String getAPI_TIME_OUT() {
        return API_TIME_OUT == null || "0".equalsIgnoreCase(API_TIME_OUT) ? String.valueOf(PayEumeration.DEFAULT_TIME_OUT_REQPAY) : API_TIME_OUT;
    }

    public void setAPI_TIME_OUT(String API_TIME_OUT) {
        this.API_TIME_OUT = API_TIME_OUT;
    }

    public String getAPI_ORDER_STATE() {
        return API_ORDER_STATE;
    }

    public void setAPI_ORDER_STATE(String API_ORDER_STATE) {
        this.API_ORDER_STATE = API_ORDER_STATE;
    }

    public String getAPI_PUBLIC_KEY() {
        return StringUtils.isNotBlank(API_PUBLIC_KEY)?API_PUBLIC_KEY.replaceAll("\\s*", ""):"";
    }

    public void setAPI_PUBLIC_KEY(String API_PUBLIC_KEY) {
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
    }

    public String getAPI_SEQUENCE_NUMBER() {
        return StringUtils.isBlank(API_SEQUENCE_NUMBER)?"0":API_SEQUENCE_NUMBER;
    }

    public void setAPI_SEQUENCE_NUMBER(String API_SEQUENCE_NUMBER) {
        this.API_SEQUENCE_NUMBER = API_SEQUENCE_NUMBER;
    }

    public String getAPI_JUMP_URL_PREFIX() {
        return API_JUMP_URL_PREFIX;
    }

    public void setAPI_JUMP_URL_PREFIX(String API_JUMP_URL_PREFIX) {
        this.API_JUMP_URL_PREFIX = API_JUMP_URL_PREFIX;
    }

    public String getAPI_WEB_URL() {
        return API_WEB_URL;
    }

    public void setAPI_WEB_URL(String API_WEB_URL) {
        this.API_WEB_URL = API_WEB_URL;
    }

    public String getAPI_OTHER_PARAM() {
        return API_OTHER_PARAM;
    }

    public void setAPI_OTHER_PARAM(String API_OTHER_PARAM) {
        this.API_OTHER_PARAM = API_OTHER_PARAM;
    }

    public Map<String, String> getResParams() {
        return resParams;
    }

    public void setResParams(Map<String, String> resParams) {
        this.resParams = resParams;
    }

    public ReqPayInfo(String API_KEY, String API_PUBLIC_KEY, String API_MEMBERID, String API_AMOUNT, String API_ORDER_ID, String API_OrDER_TIME, String API_CHANNEL_BANK_NAME, String API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX, String API_MEMBER_PLATFORMID) {
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_OrDER_TIME = API_OrDER_TIME;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
    }





    public ReqPayInfo(String API_KEY, String API_PUBLIC_KEY, String API_MEMBERID, String API_MEMBER_PLATFORMID, String API_AMOUNT, String API_ORDER_ID, String API_OrDER_TIME, String API_CHANNEL_BANK_NAME, String API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX, String API_SEQUENCE_NUMBER) {
        this.API_KEY = API_KEY;
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_OrDER_TIME = API_OrDER_TIME;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_SEQUENCE_NUMBER = API_SEQUENCE_NUMBER;
    }



    public ReqPayInfo(String API_JUMP_URL_PREFIX,String API_WEB_URL,String API_OTHER_PARAM,String API_KEY, String API_PUBLIC_KEY,String API_MEMBERID, String API_AMOUNT, String API_ORDER_ID, String API_OrDER_TIME, String API_CHANNEL_BANK_NAME, String API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX,String API_MEMBER_PLATFORMID) {
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_OrDER_TIME = API_OrDER_TIME;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
        this.API_JUMP_URL_PREFIX = API_JUMP_URL_PREFIX;
        this.API_WEB_URL = API_WEB_URL;
        this.API_OTHER_PARAM = API_OTHER_PARAM;
    }

    public ReqPayInfo(String  API_Client_IP,String API_JUMP_URL_PREFIX,String API_WEB_URL,String API_OTHER_PARAM,String API_KEY, String API_PUBLIC_KEY,String API_MEMBERID, String API_AMOUNT, String API_ORDER_ID, String API_OrDER_TIME, String API_CHANNEL_BANK_NAME, String API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX,String API_MEMBER_PLATFORMID) {
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_OrDER_TIME = API_OrDER_TIME;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
        this.API_JUMP_URL_PREFIX = API_JUMP_URL_PREFIX;
        this.API_WEB_URL = API_WEB_URL;
        this.API_OTHER_PARAM = API_OTHER_PARAM;
        this.API_Client_IP = API_Client_IP;
    }


    public ReqPayInfo(String API_ORDER_FROM,String API_OID,String  API_Client_IP,String API_JUMP_URL_PREFIX,String API_WEB_URL,String API_OTHER_PARAM,String API_KEY, String API_PUBLIC_KEY,String API_MEMBERID, String API_AMOUNT, String API_ORDER_ID, String API_OrDER_TIME, String API_CHANNEL_BANK_NAME, String API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX,String API_MEMBER_PLATFORMID) {
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_OrDER_TIME = API_OrDER_TIME;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
        this.API_JUMP_URL_PREFIX = API_JUMP_URL_PREFIX;
        this.API_WEB_URL = API_WEB_URL;
        this.API_OTHER_PARAM = API_OTHER_PARAM;
        this.API_Client_IP = API_Client_IP;
        this.API_OID  = API_OID;
        this.API_ORDER_FROM = API_ORDER_FROM;
    }

}
