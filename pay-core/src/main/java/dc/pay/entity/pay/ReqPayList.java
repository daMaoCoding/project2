package dc.pay.entity.pay;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseEntity;
import dc.pay.business.RequestPayResult;
import dc.pay.config.RunTimeInfo;
import dc.pay.entity.ReqPayInfo;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Transient;
import java.util.Date;

/**
 * ************************
 * @author tony 3556239829
 *
 */
public class ReqPayList extends BaseEntity {

    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private ReqPayInfo reqPayInfo;
    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private RequestPayResult requestPayResult;
    private String orderId;
    private String amount;
    private String channel;
    private  int  restView;

    private String serverId ;

    @Transient
    private String channelCName;

    private String oid;
    private String clientIp;
    private String orderForm;

    private String channelMemberId;
    private Date timeStmp;
    private String result ;

    private String jumpUrl;
    private String webUrl;
    private String notifyUrl;

    public String getOrderForm() {
        return orderForm;
    }

    public void setOrderForm(String orderForm) {
        this.orderForm = orderForm;
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

    public ReqPayList() {}


    public ReqPayList(ReqPayInfo reqPayInfo, RequestPayResult requestPayResult) {
        this.reqPayInfo = reqPayInfo;
        this.requestPayResult = requestPayResult;
        this.orderId = null==reqPayInfo?"":reqPayInfo.getAPI_ORDER_ID();
        this.amount = null==reqPayInfo?"":reqPayInfo.getAPI_AMOUNT();
        this.channel = null==reqPayInfo?"":reqPayInfo.getAPI_CHANNEL_BANK_NAME();
        this.channelMemberId = null==reqPayInfo?"":reqPayInfo.getAPI_MEMBERID();
        this.timeStmp = new Date();
        this.result = null==requestPayResult?"ERROR":requestPayResult.getRequestPayCode();
        this.oid = null==reqPayInfo?"":reqPayInfo.getAPI_OID();
        this.clientIp = null==reqPayInfo?"":reqPayInfo.getAPI_Client_IP();
        this.orderForm =null==reqPayInfo?"":reqPayInfo.getAPI_ORDER_FROM();
        this.serverId = RunTimeInfo.startInfo.getServerID().concat(":").concat( RunTimeInfo.startInfo.getProfiles());
        this.jumpUrl =null==reqPayInfo?"":reqPayInfo.getAPI_JUMP_URL_PREFIX();
        this.webUrl =null==reqPayInfo?"":reqPayInfo.getAPI_WEB_URL();
        this.notifyUrl =null==reqPayInfo?"":reqPayInfo.getAPI_NOTIFY_URL_PREFIX();
    }

    public ReqPayInfo getReqPayInfo() {
        return reqPayInfo;
    }

    public void setReqPayInfo(ReqPayInfo reqPayInfo) {
        this.reqPayInfo = reqPayInfo;
    }

    public RequestPayResult getRequestPayResult() {
        return requestPayResult;
    }

    public void setRequestPayResult(RequestPayResult requestPayResult) {
        this.requestPayResult = requestPayResult;
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

    public int getRestView() {
        return restView;
    }

    public void setRestView(int restView) {
        this.restView = restView;
    }

    public String getJumpUrl() {
        return jumpUrl;
    }

    public void setJumpUrl(String jumpUrl) {
        this.jumpUrl = jumpUrl;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }
}
