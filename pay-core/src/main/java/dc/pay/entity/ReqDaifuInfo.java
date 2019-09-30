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

public class ReqDaifuInfo implements Serializable {

    private String API_OID;
    private String API_KEY;
    private String API_PUBLIC_KEY;
    private String API_MEMBERID;
    private String API_CHANNEL_BANK_NAME;
    private String API_ORDER_ID;
    private volatile String API_AMOUNT;
    private String API_OrDER_TIME;
    private String API_Client_IP;
    private String API_NOTIFY_URL_PREFIX;
    private String API_OTHER_PARAM;
    private String API_ORDER_STATE;

    private String API_CUSTOMER_ACCOUNT;
    private String API_CUSTOMER_NAME;
    private String API_CUSTOMER_BANK_NAME;
    private String API_CUSTOMER_BANK_BRANCH;
    private String API_CUSTOMER_BANK_SUB_BRANCH;
    private String API_CUSTOMER_BANK_NUMBER;

    @JSONField(serialize = false)
    private Map<String, String> resParams = Maps.newHashMap();

    @Transient
    private String queryFrom;



    public ReqDaifuInfo() { }


    public ReqDaifuInfo(String API_ORDER_ID) {
        this.API_ORDER_ID = API_ORDER_ID;
    }

    public String getAPI_OID() {
        return API_OID;
    }

    public void setAPI_OID(String API_OID) {
        this.API_OID = API_OID;
    }

    public String getAPI_KEY() {
        return API_KEY;
    }

    public void setAPI_KEY(String API_KEY) {
        this.API_KEY = API_KEY;
    }

    public String getAPI_PUBLIC_KEY() {
        return API_PUBLIC_KEY;
    }

    public void setAPI_PUBLIC_KEY(String API_PUBLIC_KEY) {
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
    }

    public String getAPI_MEMBERID() {
        return API_MEMBERID;
    }

    public void setAPI_MEMBERID(String API_MEMBERID) {
        this.API_MEMBERID = API_MEMBERID;
    }

    public String getAPI_CHANNEL_BANK_NAME() {
        return API_CHANNEL_BANK_NAME;
    }

    public void setAPI_CHANNEL_BANK_NAME(String API_CHANNEL_BANK_NAME) {
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
    }

    public String getAPI_ORDER_ID() {
        return API_ORDER_ID;
    }

    public void setAPI_ORDER_ID(String API_ORDER_ID) {
        this.API_ORDER_ID = API_ORDER_ID;
    }

    public String getAPI_AMOUNT() {
        return API_AMOUNT;
    }

    public void setAPI_AMOUNT(String API_AMOUNT) {
        this.API_AMOUNT = API_AMOUNT;
    }

    public String getAPI_OrDER_TIME() {
        return API_OrDER_TIME;
    }

    public void setAPI_OrDER_TIME(String API_OrDER_TIME) {
        this.API_OrDER_TIME = API_OrDER_TIME;
    }

    public String getAPI_Client_IP() {
        return API_Client_IP;
    }

    public void setAPI_Client_IP(String API_Client_IP) {
        this.API_Client_IP = API_Client_IP;
    }

    public String getAPI_NOTIFY_URL_PREFIX() {
        return API_NOTIFY_URL_PREFIX;
    }

    public void setAPI_NOTIFY_URL_PREFIX(String API_NOTIFY_URL_PREFIX) {
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
    }

    public String getAPI_OTHER_PARAM() {
        return API_OTHER_PARAM;
    }

    public void setAPI_OTHER_PARAM(String API_OTHER_PARAM) {
        this.API_OTHER_PARAM = API_OTHER_PARAM;
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

    public Map<String, String> getResParams() {
        return resParams;
    }

    public void setResParams(Map<String, String> resParams) {
        this.resParams = resParams;
    }

    public String getAPI_ORDER_STATE() {
        return API_ORDER_STATE;
    }

    public void setAPI_ORDER_STATE(String API_ORDER_STATE) {
        this.API_ORDER_STATE = API_ORDER_STATE;
    }

    public String getQueryFrom() {
        return queryFrom;
    }

    public void setQueryFrom(String queryFrom) {
        this.queryFrom = queryFrom;
    }
}
