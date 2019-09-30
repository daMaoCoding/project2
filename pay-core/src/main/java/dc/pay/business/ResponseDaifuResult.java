package dc.pay.business;

/**
 * Created by admin on 2017/5/20.
 */

import com.alibaba.fastjson.annotation.JSONField;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.entity.ReqDaifuInfo;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * ************************
 * @author tony 3556239829
 */
public class ResponseDaifuResult implements Serializable {
    private  String responseDaifuCode;
    private  String responseOrderID;
    private  String responseOrderState;
    private  String responseDaifuErrorMsg="";
    private  long   responseDaifuTotalTime;
    private  String responseDaifuMsg;
    private  String responseDaifuChannel;
    private  String responseDaifuAmount;
    private  String responseDaifuMemberId;
    private  String responseDaifuOtherParam;
    private  String responseDaifuOid;
    private  String responseDaifuSign;
    private  String responseDaifuOrderCreateTime;

    @JSONField(serialize = false)
    private  ReqDaifuInfo reqDaifuInfo;




    public ResponseDaifuResult(boolean result, ReqDaifuInfo reqDaifuInfo, String   errorMsg, PayEumeration.DAIFU_RESULT daifuResult) {
        this(result,reqDaifuInfo,errorMsg);
        this.responseOrderState=null!=daifuResult?daifuResult.getCodeValue():PayEumeration.DAIFU_RESULT.UNKNOW.getCodeValue();
    }


    public ResponseDaifuResult(boolean result, ReqDaifuInfo reqDaifuInfo, String   errorMsg) {
        if(null!=reqDaifuInfo) this.reqDaifuInfo = reqDaifuInfo;
        setResponseDaifuCodeWithBoolean(result);
        if(StringUtils.isNotBlank(errorMsg))  this.responseDaifuErrorMsg =  StringUtils.isNotBlank(errorMsg) ? errorMsg : SERVER_MSG.RESPONSE_PAY_VALDATA_SIGN_ERROR.getMsg();
        if(null!=reqDaifuInfo){
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_OID()))   this.responseDaifuOid = reqDaifuInfo.getAPI_OID();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_MEMBERID()))   this.responseDaifuMemberId = reqDaifuInfo.getAPI_MEMBERID();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_AMOUNT()))   this.responseDaifuAmount = reqDaifuInfo.getAPI_AMOUNT();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_ORDER_ID()))  this.responseOrderID = reqDaifuInfo.getAPI_ORDER_ID();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_OrDER_TIME()))    this.responseDaifuOrderCreateTime = reqDaifuInfo.getAPI_OrDER_TIME();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()))    this.responseDaifuChannel = reqDaifuInfo.getAPI_CHANNEL_BANK_NAME();
            if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_OTHER_PARAM()))    this.responseDaifuOtherParam = reqDaifuInfo.getAPI_OTHER_PARAM();
        }
    }




    public ResponseDaifuResult(boolean result,String   errorMsg) {
        this.responseDaifuCode =result==true?"SUCCESS":"ERROR";
        if(StringUtils.isNotBlank(errorMsg))  this.responseDaifuErrorMsg = errorMsg;
    }

    public ResponseDaifuResult() {
    }


    public String getResponseDaifuCode() {
        return responseDaifuCode;
    }

    public void setResponseDaifuCode(String responseDaifuCode) {
        this.responseDaifuCode = responseDaifuCode;

    }


    public void setResponseDaifuCodeWithBoolean(boolean result) {
        this.responseDaifuCode =result==true?"SUCCESS":"ERROR";
    }


    public String getResponseOrderID() {
        return responseOrderID;
    }

    public void setResponseOrderID(String responseOrderID) {
        this.responseOrderID = responseOrderID;
    }

    public String getResponseOrderState() {
        return responseOrderState;
    }

    public void setResponseOrderState(String responseOrderState) {
        this.responseOrderState = responseOrderState;
    }


    public void setResponseOrderStateWithBoolean(boolean daifuCanceled) {
        this.responseOrderState =daifuCanceled==true?"ERROR":"SUCCESS";
    }


    public String getResponseDaifuErrorMsg() {
        return responseDaifuErrorMsg;
    }

    public void setResponseDaifuErrorMsg(String responseDaifuErrorMsg) {
        this.responseDaifuErrorMsg = responseDaifuErrorMsg;
    }

    public long getResponseDaifuTotalTime() {
        return responseDaifuTotalTime;
    }

    public void setResponseDaifuTotalTime(long responseDaifuTotalTime) {
        this.responseDaifuTotalTime = responseDaifuTotalTime;
    }

    public String getResponseDaifuMsg() {
        return responseDaifuMsg;
    }

    public void setResponseDaifuMsg(String responseDaifuMsg) {
        this.responseDaifuMsg = responseDaifuMsg;
    }

    public String getResponseDaifuChannel() {
        return responseDaifuChannel;
    }

    public void setResponseDaifuChannel(String responseDaifuChannel) {
        this.responseDaifuChannel = responseDaifuChannel;
    }

    public String getResponseDaifuAmount() {
        return responseDaifuAmount;
    }

    public void setResponseDaifuAmount(String responseDaifuAmount) {
        this.responseDaifuAmount = responseDaifuAmount;
    }

    public String getResponseDaifuMemberId() {
        return responseDaifuMemberId;
    }

    public void setResponseDaifuMemberId(String responseDaifuMemberId) {
        this.responseDaifuMemberId = responseDaifuMemberId;
    }

    public String getResponseDaifuOtherParam() {
        return responseDaifuOtherParam;
    }

    public void setResponseDaifuOtherParam(String responseDaifuOtherParam) {
        this.responseDaifuOtherParam = responseDaifuOtherParam;
    }

    public String getResponseDaifuOid() {
        return responseDaifuOid;
    }

    public void setResponseDaifuOid(String responseDaifuOid) {
        this.responseDaifuOid = responseDaifuOid;
    }

    public String getResponseDaifuSign() {
        return responseDaifuSign;
    }

    public void setResponseDaifuSign(String responseDaifuSign) {
        this.responseDaifuSign = responseDaifuSign;
    }

    public String getResponseDaifuOrderCreateTime() {
        return responseDaifuOrderCreateTime;
    }

    public void setResponseDaifuOrderCreateTime(String responseDaifuOrderCreateTime) {
        this.responseDaifuOrderCreateTime = responseDaifuOrderCreateTime;
    }


    public ReqDaifuInfo getReqDaifuInfo() {
        return reqDaifuInfo;
    }

    public void setReqDaifuInfo(ReqDaifuInfo reqDaifuInfo) {
        this.reqDaifuInfo = reqDaifuInfo;
    }


}
