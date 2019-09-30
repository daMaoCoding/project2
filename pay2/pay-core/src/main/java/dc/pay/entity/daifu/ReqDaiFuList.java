package dc.pay.entity.daifu;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseEntity;
import dc.pay.business.RequestDaifuResult;
import dc.pay.config.RunTimeInfo;
import dc.pay.constant.PayEumeration;
import dc.pay.entity.ReqDaifuInfo;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

/**
 * ************************
 * @author tony 3556239829
 *
 */
@Table(name = "req_daifu_list")
public class ReqDaiFuList extends BaseEntity {
    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private ReqDaifuInfo reqDaifuInfo;
    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private RequestDaifuResult requestDaifuResult;
    private String orderId;
    private String amount;
    private String channel;
    private String channelMemberId;
    private Date timeStmp;
    private String result ;
    private String oid;
    private String clientIp;
    private String notifyUrl;
    private String serverId ;
    private String orderStatus;


    @Transient
    private String channelCName;


    private String account;
    private String customerName;
    private String bankName;
    private String bankBranch;
    private String bankSubBranch;
    private String bankNumber;



    public ReqDaifuInfo getReqDaifuInfo() {
        return reqDaifuInfo;
    }

    public void setReqDaifuInfo(ReqDaifuInfo reqDaifuInfo) {
        this.reqDaifuInfo = reqDaifuInfo;
    }




    public String getChannelCName() {
         return channelCName;
    }

    public void setChannelCName(String channelCName) {
        this.channelCName = channelCName;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelMemberId() {
        return channelMemberId;
    }

    public void setChannelMemberId(String channelMemberId) {
        this.channelMemberId = channelMemberId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public ReqDaiFuList() {}


    public ReqDaiFuList(ReqDaifuInfo reqDaifuInfo, RequestDaifuResult requestDaifuResult) {
        this.reqDaifuInfo = reqDaifuInfo;
        this.requestDaifuResult = requestDaifuResult;
        this.orderId = null==reqDaifuInfo?"":reqDaifuInfo.getAPI_ORDER_ID();
        this.amount = null==reqDaifuInfo?"":reqDaifuInfo.getAPI_AMOUNT();
        this.channel = null==reqDaifuInfo?"":reqDaifuInfo.getAPI_CHANNEL_BANK_NAME();
        this.channelMemberId = null==reqDaifuInfo?"":reqDaifuInfo.getAPI_MEMBERID();
        this.timeStmp = new Date();
        this.result = null==requestDaifuResult?"ERROR":requestDaifuResult.getRequestDaifuCode();
        this.orderStatus=null==requestDaifuResult?PayEumeration.DAIFU_RESULT.UNKNOW.getCodeValue():requestDaifuResult.getRequestDaifuOrderState();
        this.oid = null==reqDaifuInfo?"":reqDaifuInfo.getAPI_OID();
        this.clientIp = null==reqDaifuInfo?"":reqDaifuInfo.getAPI_Client_IP();
        this.serverId = RunTimeInfo.startInfo.getServerID().concat(":").concat( RunTimeInfo.startInfo.getProfiles());
        this.notifyUrl =null==reqDaifuInfo?"":reqDaifuInfo.getAPI_NOTIFY_URL_PREFIX();
         this.account = null== reqDaifuInfo?"":reqDaifuInfo.getAPI_CUSTOMER_ACCOUNT();
         this.customerName = null== reqDaifuInfo?"":reqDaifuInfo.getAPI_CUSTOMER_NAME();
         this.bankName = null== reqDaifuInfo?"":reqDaifuInfo.getAPI_CUSTOMER_BANK_NAME();
         this.bankBranch = null== reqDaifuInfo?"":reqDaifuInfo.getAPI_CUSTOMER_BANK_BRANCH();
         this.bankSubBranch = null== reqDaifuInfo?"":reqDaifuInfo.getAPI_CUSTOMER_BANK_SUB_BRANCH();
         this.bankNumber = null== reqDaifuInfo?"":reqDaifuInfo.getAPI_CUSTOMER_BANK_NUMBER();

    }

    public RequestDaifuResult getRequestDaifuResult() {
        return requestDaifuResult;
    }

    public void setRequestDaifuResult(RequestDaifuResult requestDaifuResult) {
        this.requestDaifuResult = requestDaifuResult;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public Date getTimeStmp() {
        return timeStmp;
    }

    public void setTimeStmp(Date timeStmp) {
        this.timeStmp = timeStmp;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }



    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getBankSubBranch() {
        return bankSubBranch;
    }

    public void setBankSubBranch(String bankSubBranch) {
        this.bankSubBranch = bankSubBranch;
    }

    public String getBankNumber() {
        return bankNumber;
    }

    public void setBankNumber(String bankNumber) {
        this.bankNumber = bankNumber;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
