package dc.pay.entity.daifu;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import dc.pay.base.BaseEntity;
import dc.pay.base.ResListI;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.config.RunTimeInfo;
import dc.pay.entity.ReqDaifuInfo;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
@Table(name = "res_daifu_list")
public class ResDaiFuList extends BaseEntity implements ResListI {

    private  String orderId;
    private  String amount;
    private  String channel;

    @Transient
    private  String channelCName;
    private String channelMemberId;
    private Date   timeStmp;
    private String result ;

    private String orderState;

    private String oid;

    private  int    resDbCount;
    private  String resDbResult;
    private  String resDbMsg;
    private  String serverId ;
    private  Date   reqDaifuTimeStmp;
    private  String resDaifuRemoteIp;

    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private Map<String,String> responseDaifuParams;

    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private ResponseDaifuResult responseDaifuResult;

    @ColumnType(typeHandler = dc.pay.utils.mybatis.typeHandler.JsonTypeHandler.class)
    private ReqDaifuInfo reqDaifuInfo;


    //响应代付
    public ResDaiFuList(Map<String, String> responseDaifuParams,  ResponseDaifuResult responseDaifuResult, ReqDaifuInfo reqDaifuInfo, int resDbCount, String resDbMsg, String resDbResult, String resPayRemoteIp) {
        this(responseDaifuResult,resDbCount,reqDaifuInfo);
        this.resDbResult = resDbResult;
        this.resDbMsg = resDbMsg;
        this.resDaifuRemoteIp = resPayRemoteIp;
        this.responseDaifuParams = !MapUtils.isEmpty(responseDaifuParams)?responseDaifuParams:null;
    }


    //查询代付
    public ResDaiFuList(ResponseDaifuResult responseDaifuResult, Map<String,String> params,  String  detail, int resDbCount, ReqDaifuInfo reqDaifuInfo) {
        this(responseDaifuResult,resDbCount,reqDaifuInfo);
        this.responseDaifuParams = Maps.newHashMap();
        if(!MapUtils.isEmpty(params))this.responseDaifuParams.put("查询代付参数",JSON.toJSONString(params));
        if(StringUtils.isNotBlank(detail))this.responseDaifuParams.put("查询代付结果",detail);
        if(null!=responseDaifuResult)this.responseDaifuParams.put("响应调用者",JSON.toJSONString(responseDaifuResult));
    }


    public ResDaiFuList(ResponseDaifuResult responseDaifuResult,int resDbCount, ReqDaifuInfo reqDaifuInfo){
        this.timeStmp = new Date();
        this.resDbCount = resDbCount;
        this.serverId = RunTimeInfo.startInfo.getServerID().concat(":").concat( RunTimeInfo.startInfo.getProfiles());
        this.responseDaifuResult = responseDaifuResult!=null?responseDaifuResult:null;
        this.reqDaifuInfo =  reqDaifuInfo!=null?reqDaifuInfo:null;
        this.reqDaifuTimeStmp = reqDaifuInfo!=null&& StringUtils.isNotBlank(reqDaifuInfo.getAPI_OID())?new Date(Long.parseLong(reqDaifuInfo.getAPI_OrDER_TIME())):null;
        if(null!=responseDaifuResult){
            this.orderId = responseDaifuResult.getResponseOrderID();
            this.amount = responseDaifuResult.getResponseDaifuAmount();
            this.channel = responseDaifuResult.getResponseDaifuChannel();
            this.channelMemberId = responseDaifuResult.getResponseDaifuMemberId();
            this.result = responseDaifuResult.getResponseDaifuCode();
            this.orderState = responseDaifuResult.getResponseOrderState();
            this.oid = responseDaifuResult.getResponseDaifuOid();
        }
    }



    public String getOrderState() {
        return orderState;
    }

    public void setOrderState(String orderState) {
        this.orderState = orderState;
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

    public ResDaiFuList() {
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



    public Map<String, String> getResponseDaifuParams() {
        return responseDaifuParams;
    }

    public void setResponseDaifuParams(Map<String, String> responseDaifuParams) {
        this.responseDaifuParams = responseDaifuParams;
    }


    public ResponseDaifuResult getResponseDaifuResult() {
        return responseDaifuResult;
    }

    public void setResponseDaifuResult(ResponseDaifuResult responseDaifuResult) {
        this.responseDaifuResult = responseDaifuResult;
    }

    public ReqDaifuInfo getReqDaifuInfo() {
        return reqDaifuInfo;
    }

    public void setReqDaifuInfo(ReqDaifuInfo reqDaifuInfo) {
        this.reqDaifuInfo = reqDaifuInfo;
    }


    public Date getReqDaifuTimeStmp() {
        return reqDaifuTimeStmp;
    }

    public void setReqDaifuTimeStmp(Date reqDaifuTimeStmp) {
        this.reqDaifuTimeStmp = reqDaifuTimeStmp;
    }

    public String getResDaifuRemoteIp() {
        return resDaifuRemoteIp;
    }

    public void setResDaifuRemoteIp(String resDaifuRemoteIp) {
        this.resDaifuRemoteIp = resDaifuRemoteIp;
    }
}
