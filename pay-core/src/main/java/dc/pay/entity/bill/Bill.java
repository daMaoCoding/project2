package dc.pay.entity.bill;/**
 * Created by admin on 2017/6/19.
 */

import dc.pay.base.BaseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import java.util.Date;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class Bill extends BaseEntity {

    @Column(name = "API_JUMP_URL_PREFIX")
    String API_JUMP_URL_PREFIX;


    @Column(name = "API_WEB_URL")
    String API_WEB_URL;


    @Column(name = "API_OTHER_PARAM")
    String API_OTHER_PARAM;


    @Column(name = "API_ORDER_FROM")
    String API_ORDER_FROM;


    @Column(name = "API_Client_IP")
    String API_Client_IP;



    @Column(name = "API_OID")
    String API_OID;

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

    @Column(name = "API_PUBLIC_KEY")
    String API_PUBLIC_KEY;

    @Column(name = "API_KEY")
    String API_KEY;

    @Column(name = "API_MEMBERID")
    String API_MEMBERID;

    @Column(name = "API_MEMBER_PLATFORMID")
    String API_MEMBER_PLATFORMID;

    @Column(name = "API_AMOUNT")
    Long API_AMOUNT;

    @Column(name = "API_ORDER_ID")
    String API_ORDER_ID;

    @Column(name = "API_CHANNEL_BANK_NAME")
    String API_CHANNEL_BANK_NAME;

    @Column(name = "API_TIME_OUT")
    Long API_TIME_OUT;

    @Column(name = "API_ORDER_STATE")
    String API_ORDER_STATE;

    @Column(name = "API_NOTIFY_URL_PREFIX")
    String API_NOTIFY_URL_PREFIX;

    @Column(name = "API_ORDER_TIME")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    Date API_ORDER_TIME;

    @Column(name = "API_CUSTOMER_ACCOUNT")
     String API_CUSTOMER_ACCOUNT;

    @Column(name = "API_CUSTOMER_NAME")
     String API_CUSTOMER_NAME;

    @Column(name = "API_CUSTOMER_BANK_NAME")
     String API_CUSTOMER_BANK_NAME;

    @Column(name = "API_CUSTOMER_BANK_BRANCH")
     String API_CUSTOMER_BANK_BRANCH;

    @Column(name = "API_CUSTOMER_BANK_SUB_BRANCH")
     String API_CUSTOMER_BANK_SUB_BRANCH;

    @Column(name = "API_CUSTOMER_BANK_NUMBER")
     String API_CUSTOMER_BANK_NUMBER;




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

    public String getAPI_KEY() {
        return API_KEY;
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

    public Long getAPI_AMOUNT() {
        return API_AMOUNT;
    }

    public void setAPI_AMOUNT(Long API_AMOUNT) {
        this.API_AMOUNT = API_AMOUNT;
    }

    public String getAPI_ORDER_ID() {
        return API_ORDER_ID;
    }

    public void setAPI_ORDER_ID(String API_ORDER_ID) {
        this.API_ORDER_ID = API_ORDER_ID;
    }

    public String getAPI_CHANNEL_BANK_NAME() {
        return API_CHANNEL_BANK_NAME;
    }

    public void setAPI_CHANNEL_BANK_NAME(String API_CHANNEL_BANK_NAME) {
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
    }

    public Long getAPI_TIME_OUT() {
        return API_TIME_OUT;
    }

    public void setAPI_TIME_OUT(Long API_TIME_OUT) {
        this.API_TIME_OUT = API_TIME_OUT;
    }

    public String getAPI_ORDER_STATE() {
        return API_ORDER_STATE;
    }

    public void setAPI_ORDER_STATE(String API_ORDER_STATE) {
        this.API_ORDER_STATE = API_ORDER_STATE;
    }

    public String getAPI_NOTIFY_URL_PREFIX() {
        return API_NOTIFY_URL_PREFIX;
    }

    public void setAPI_NOTIFY_URL_PREFIX(String API_NOTIFY_URL_PREFIX) {
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
    }

    public Date getAPI_ORDER_TIME() {
        return API_ORDER_TIME;
    }

    public void setAPI_ORDER_TIME(Date API_ORDER_TIME) {
        this.API_ORDER_TIME = API_ORDER_TIME;
    }

    public Bill() {
    }

    public String getAPI_MEMBER_PLATFORMID() {
        return API_MEMBER_PLATFORMID;
    }

    public void setAPI_MEMBER_PLATFORMID(String API_MEMBER_PLATFORMID) {
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
    }

    public String getAPI_PUBLIC_KEY() {
        return API_PUBLIC_KEY;
    }

    public void setAPI_PUBLIC_KEY(String API_PUBLIC_KEY) {
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
    }

    public String getAPI_ORDER_FROM() {
        return API_ORDER_FROM;
    }

    public void setAPI_ORDER_FROM(String API_ORDER_FROM) {
        this.API_ORDER_FROM = API_ORDER_FROM;
    }

    public Bill(String API_KEY, String API_MEMBERID, String API_MEMBER_PLATFORMID, Long API_AMOUNT, String API_ORDER_ID, String API_CHANNEL_BANK_NAME, Long API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX, Date API_ORDER_TIME) {
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_ORDER_TIME = API_ORDER_TIME;
    }

    public Bill(String API_KEY, String API_MEMBERID, Long API_AMOUNT, String API_ORDER_ID, String API_CHANNEL_BANK_NAME, Long API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX, Date API_ORDER_TIME) {
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_ORDER_TIME = API_ORDER_TIME;
    }


    public Bill(String API_PUBLIC_KEY, String API_KEY, String API_MEMBERID, String API_MEMBER_PLATFORMID, Long API_AMOUNT, String API_ORDER_ID, String API_CHANNEL_BANK_NAME, Long API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX, Date API_ORDER_TIME) {
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_ORDER_TIME = API_ORDER_TIME;
    }


    public Bill(String API_JUMP_URL_PREFIX,String API_WEB_URL,String API_OTHER_PARAM,String API_PUBLIC_KEY, String API_KEY, String API_MEMBERID, String API_MEMBER_PLATFORMID, Long API_AMOUNT, String API_ORDER_ID, String API_CHANNEL_BANK_NAME, Long API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX, Date API_ORDER_TIME) {
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_ORDER_TIME = API_ORDER_TIME;
        this.API_JUMP_URL_PREFIX = API_JUMP_URL_PREFIX;
        this.API_WEB_URL = API_WEB_URL;
        this.API_OTHER_PARAM = API_OTHER_PARAM;
    }

    public Bill(String API_ORDER_FROM,String API_Client_IP,String API_JUMP_URL_PREFIX,String API_WEB_URL,String API_OTHER_PARAM,String API_PUBLIC_KEY, String API_KEY, String API_MEMBERID, String API_MEMBER_PLATFORMID, Long API_AMOUNT, String API_ORDER_ID, String API_CHANNEL_BANK_NAME, Long API_TIME_OUT, String API_ORDER_STATE, String API_NOTIFY_URL_PREFIX, Date API_ORDER_TIME) {
        this.API_PUBLIC_KEY = API_PUBLIC_KEY;
        this.API_KEY = API_KEY;
        this.API_MEMBERID = API_MEMBERID;
        this.API_MEMBER_PLATFORMID = API_MEMBER_PLATFORMID;
        this.API_AMOUNT = API_AMOUNT;
        this.API_ORDER_ID = API_ORDER_ID;
        this.API_CHANNEL_BANK_NAME = API_CHANNEL_BANK_NAME;
        this.API_TIME_OUT = API_TIME_OUT;
        this.API_ORDER_STATE = API_ORDER_STATE;
        this.API_NOTIFY_URL_PREFIX = API_NOTIFY_URL_PREFIX;
        this.API_ORDER_TIME = API_ORDER_TIME;
        this.API_JUMP_URL_PREFIX = API_JUMP_URL_PREFIX;
        this.API_WEB_URL = API_WEB_URL;
        this.API_OTHER_PARAM = API_OTHER_PARAM;
        this.API_Client_IP = API_Client_IP;
        this.API_ORDER_FROM = API_ORDER_FROM;
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
