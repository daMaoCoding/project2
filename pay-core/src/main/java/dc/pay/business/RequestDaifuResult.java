package dc.pay.business;

/**
 * Created by admin on 2017/5/18.
 */

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.ReqPayInfo;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class RequestDaifuResult implements Serializable {

    private String requestDaifuCode;               //结果 SUCCESS /ERROR
    private String requestDaifuOid;               // OID
    private String requestDaifuErrorMsg="";           //信息
    private String requestDaifuAmount;             //金额分
    private String requestDaifuOrderId;            //单号
    private String requestDaifuOrderCreateTime;    //创单时间
    private String requestDaifuChannelBankName;    //通道名
    private String requestDaifuOtherParam;         //其他参数
    private long   requestDaifuTotalTime;          //总耗时
    private long   requestDaifuChannelTime;        //通道耗时
    private long   requestDaifuGetReqDaifuInfoTime;  //到DB查单代付信息耗时

    private String requestDaifuOrderState; //订单状态


    private  Map<String,String>  params  = Maps.newLinkedHashMap();  //第三方请求参数
    private  Map<String,String>  details = Maps.newLinkedHashMap();  //第三方请求过程中需要记录的内容


    public RequestDaifuResult(boolean result, ReqDaifuInfo reqDaifuInfo, String   errorMsg,PayEumeration.DAIFU_RESULT orderState) {
        if(null!=reqDaifuInfo)  this.requestDaifuCode =result==true?"SUCCESS":"ERROR";
        if(StringUtils.isNotBlank(errorMsg))  this.requestDaifuErrorMsg = errorMsg;
        if(null!=reqDaifuInfo){
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_AMOUNT()))   this.requestDaifuAmount = reqDaifuInfo.getAPI_AMOUNT();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_ORDER_ID()))  this.requestDaifuOrderId = reqDaifuInfo.getAPI_ORDER_ID();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_OrDER_TIME()))    this.requestDaifuOrderCreateTime = reqDaifuInfo.getAPI_OrDER_TIME();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()))    this.requestDaifuChannelBankName = reqDaifuInfo.getAPI_CHANNEL_BANK_NAME();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_OTHER_PARAM()))    this.requestDaifuOtherParam = reqDaifuInfo.getAPI_OTHER_PARAM();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_OID()))    this.requestDaifuOid = reqDaifuInfo.getAPI_OID();
            if(null!=orderState) this.requestDaifuOrderState = orderState.getCodeValue();
        }
        if(null==orderState)   this.requestDaifuOrderState = PayEumeration.DAIFU_RESULT.UNKNOW.getCodeValue();

    }


    public RequestDaifuResult() { }

    public RequestDaifuResult(boolean result,String   errorMsg,PayEumeration.DAIFU_RESULT daifuResult) {
         this.requestDaifuCode =result==true?"SUCCESS":"ERROR";
         if(StringUtils.isNotBlank(errorMsg))  this.requestDaifuErrorMsg = errorMsg;
         if(null!=daifuResult) this.requestDaifuOrderState = daifuResult.getCodeValue();
    }

    public String getRequestDaifuCode() {
        return requestDaifuCode;
    }

    public void setRequestDaifuCode(boolean result) {
        this.requestDaifuCode =result==true?"SUCCESS":"ERROR";
    }

    public String getRequestDaifuErrorMsg() {
        return requestDaifuErrorMsg;
    }

    public void setRequestDaifuErrorMsg(String requestDaifuErrorMsg) {
        this.requestDaifuErrorMsg = requestDaifuErrorMsg;
    }

    public String getRequestDaifuAmount() {
        return requestDaifuAmount;
    }

    public void setRequestDaifuAmount(String requestDaifuAmount) {
        this.requestDaifuAmount = requestDaifuAmount;
    }

    public String getRequestDaifuOrderId() {
        return requestDaifuOrderId;
    }

    public void setRequestDaifuOrderId(String requestDaifuOrderId) {
        this.requestDaifuOrderId = requestDaifuOrderId;
    }

    public long getRequestDaifuTotalTime() {
        return requestDaifuTotalTime;
    }

    public void setRequestDaifuTotalTime(long requestDaifuTotalTime) {
        this.requestDaifuTotalTime = requestDaifuTotalTime;
    }

    public long getRequestDaifuChannelTime() {
        return requestDaifuChannelTime;
    }

    public void setRequestDaifuChannelTime(long requestDaifuChannelTime) {
        this.requestDaifuChannelTime = requestDaifuChannelTime;
    }

    public String getRequestDaifuOtherParam() {
        return requestDaifuOtherParam;
    }

    public void setRequestDaifuOtherParam(String requestDaifuOtherParam) {
        this.requestDaifuOtherParam = requestDaifuOtherParam;
    }

    public String getRequestDaifuOrderCreateTime() {
        return requestDaifuOrderCreateTime;
    }

    public void setRequestDaifuOrderCreateTime(String requestDaifuOrderCreateTime) {
        this.requestDaifuOrderCreateTime = requestDaifuOrderCreateTime;
    }

    public String getRequestDaifuChannelBankName() {
        return requestDaifuChannelBankName;
    }

    public void setRequestDaifuChannelBankName(String requestDaifuChannelBankName) {
        this.requestDaifuChannelBankName = requestDaifuChannelBankName;
    }

    public long getRequestDaifuGetReqDaifuInfoTime() {
        return requestDaifuGetReqDaifuInfoTime;
    }

    public void setRequestDaifuGetReqDaifuInfoTime(long requestDaifuGetReqDaifuInfoTime) {
        this.requestDaifuGetReqDaifuInfoTime = requestDaifuGetReqDaifuInfoTime;
    }

    public void setRequestDaifuCode(String requestDaifuCode) {
        this.requestDaifuCode = requestDaifuCode;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public String getRequestDaifuOid() {
        return requestDaifuOid;
    }

    public void setRequestDaifuOid(String requestDaifuOid) {
        this.requestDaifuOid = requestDaifuOid;
    }

    public String getRequestDaifuOrderState() {
        return requestDaifuOrderState;
    }

    public void setRequestDaifuOrderState(String requestDaifuOrderState) {
        this.requestDaifuOrderState = requestDaifuOrderState;
    }
}
