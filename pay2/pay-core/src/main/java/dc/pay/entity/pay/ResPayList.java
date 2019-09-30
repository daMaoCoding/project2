package dc.pay.entity.pay;
import com.alibaba.fastjson.JSON;
import dc.pay.base.BaseEntity;
import dc.pay.base.ResListI;
import dc.pay.business.ResponsePayResult;
import dc.pay.config.RunTimeInfo;
import dc.pay.entity.ReqPayInfo;
import org.apache.commons.lang.StringUtils;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Transient;
import java.util.Date;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
public class ResPayList extends BaseEntity implements ResListI {

    private  String orderId;
    private  String amount;
    private  String channel;
    @Transient
    private  String channelCName;
    private String channelMemberId;
    private Date   timeStmp;
    private String result ;

    private String oid;

    private  int resDbCount;
    private  String resDbResult;
    private  String resDbMsg;
    private String serverId ;
    private Date   reqPayTimeStmp;
    private String resPayRemoteIp;

    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private Map<String,String> responsePayParams;

    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private ResponsePayResult responsePayResult;

    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private ReqPayInfo reqPayInfo;


    public ResPayList(Map<String, String> responsePayParams, ResponsePayResult responsePayResult,ReqPayInfo reqPayInfo,int resDbCount,String resDbMsg,String resDbResult,String resPayRemoteIp) {
        this.responsePayParams = responsePayParams;
        this.responsePayResult = responsePayResult;
        this.orderId = responsePayResult.getResponseOrderID();
        this.amount = responsePayResult.getResponsePayAmount();
        this.channel = responsePayResult.getResponsePayChannel();
        this.channelMemberId = responsePayResult.getResponsePayMemberId();
        this.timeStmp = new Date();
        this.result = responsePayResult.getResponsePayCode();
        this.reqPayInfo =  reqPayInfo!=null?reqPayInfo:null;
        this.oid = reqPayInfo!=null&& StringUtils.isNotBlank(reqPayInfo.getAPI_OID())?reqPayInfo.getAPI_OID():null;
        this.resDbCount = resDbCount;
        this.resDbResult = resDbResult;
        this.resDbMsg = resDbMsg;
        this.serverId = RunTimeInfo.startInfo.getServerID().concat(":").concat( RunTimeInfo.startInfo.getProfiles());
        this.reqPayTimeStmp = reqPayInfo!=null&& StringUtils.isNotBlank(reqPayInfo.getAPI_OID())?new Date(Long.parseLong(reqPayInfo.getAPI_OrDER_TIME())):null;
        this.resPayRemoteIp = resPayRemoteIp;
    }

    public Date getReqPayTimeStmp() {
        return reqPayTimeStmp;
    }

    public void setReqPayTimeStmp(Date reqPayTimeStmp) {
        this.reqPayTimeStmp = reqPayTimeStmp;
    }

    public int getResDbCount() {
        return resDbCount;
    }

    public void setResDbCount(int resDbCount) {
        this.resDbCount = resDbCount;
    }

    public String getResDbResult() {
        return resDbResult;
    }

    public void setResDbResult(String resDbResult) {
        this.resDbResult = resDbResult;
    }

    public ResPayList() {
    }

    public String getResDbMsg() {
        return resDbMsg;
    }

    public void setResDbMsg(String resDbMsg) {
            this.resDbMsg = resDbMsg;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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

    public Date getTimeStmp() {
        return timeStmp;
    }

    public void setTimeStmp(Date timeStmp) {
        this.timeStmp = timeStmp;
    }

    public Map<String, String> getResponsePayParams() {
        return responsePayParams;
    }

    public void setResponsePayParams(Map<String, String> responsePayParams) {
        this.responsePayParams = responsePayParams;
    }

    public ResponsePayResult getResponsePayResult() {
        return responsePayResult;
    }

    public void setResponsePayResult(ResponsePayResult responsePayResult) {
        this.responsePayResult = responsePayResult;
    }

    public String getChannelCName() {
        return channelCName;
    }

    public void setChannelCName(String channelCName) {
        this.channelCName = channelCName;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public ReqPayInfo getReqPayInfo() {
        return reqPayInfo;
    }

    public void setReqPayInfo(ReqPayInfo reqPayInfo) {
        this.reqPayInfo = reqPayInfo;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getResPayRemoteIp() {
        return resPayRemoteIp;
    }

    public void setResPayRemoteIp(String resPayRemoteIp) {
        this.resPayRemoteIp = resPayRemoteIp;
    }
}
