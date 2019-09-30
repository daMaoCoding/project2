package dc.pay.business;

/**
 * Created by admin on 2017/5/18.
 */

import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.entity.ReqPayInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class RequestPayResult implements Serializable {

    private String requestPayCode;
    private String requestPayQRcodeURL;
    private String requestPayQRcodeContent;
    private String requestPayHtmlContent;
    private String requestPayJumpToUrl;   //指令浏览器跳转地址，todo
    private String requestPayErrorMsg;
    private String requestPayamount;
    private String requestPayOrderId;
    private String requestPayOrderCreateTime;
    private String requestPayChannelBankName;
    private String requestPayOtherParam;
    private long requestPayTotalTime;
    private long requestPayChannelTime;
    private long requestPayGetReqpayinfoTime;
    private List<Map<String, String>> detail;
    public RequestPayResult() {
    }

    public RequestPayResult(SERVER_MSG requestPayErrorMsg, String requestPayOrderId) {
        this.requestPayCode = PayEumeration.REQUEST_PAY_CODE.ERROR.getCodeValue();
        this.requestPayErrorMsg = null != requestPayErrorMsg ? requestPayErrorMsg.getMsg() : null;
        this.requestPayOrderId = requestPayOrderId;
    }

    public RequestPayResult(String requestPayErrorMsg, String requestPayOrderId) {
        this.requestPayCode = PayEumeration.REQUEST_PAY_CODE.ERROR.getCodeValue();
        this.requestPayErrorMsg = requestPayErrorMsg;
        this.requestPayOrderId = requestPayOrderId;
    }
    public RequestPayResult(SERVER_MSG requestPayErrorMsg, String requestPayOrderId, String requestPayamount, String requestPayChannelBankName) {
        this.requestPayErrorMsg = null != requestPayErrorMsg ? requestPayErrorMsg.getMsg() : null;
        this.requestPayCode = PayEumeration.REQUEST_PAY_CODE.ERROR.getCodeValue();
        this.requestPayOrderId = requestPayOrderId;
        this.requestPayamount = requestPayamount;
        this.requestPayChannelBankName = requestPayChannelBankName;
    }

    public RequestPayResult(String requestPayErrorMsg, String requestPayOrderId, String requestPayamount, String requestPayChannelBankName) {
        this.requestPayErrorMsg = requestPayErrorMsg;
        this.requestPayCode = PayEumeration.REQUEST_PAY_CODE.ERROR.getCodeValue();
        this.requestPayOrderId = requestPayOrderId;
        this.requestPayamount = requestPayamount;
        this.requestPayChannelBankName = requestPayChannelBankName;
    }

    public RequestPayResult(Exception requestPayErrorMsg, String requestPayOrderId, String requestPayamount, String requestPayChannelBankName) {
        this.requestPayErrorMsg = null != requestPayErrorMsg ? requestPayErrorMsg.getMessage() : null;
        this.requestPayCode = PayEumeration.REQUEST_PAY_CODE.ERROR.getCodeValue();
        this.requestPayOrderId = requestPayOrderId;
        this.requestPayamount = requestPayamount;
        this.requestPayChannelBankName = requestPayChannelBankName;
    }

    public String getRequestPayOtherParam() {
        return requestPayOtherParam;
    }

    public void setRequestPayOtherParam(String requestPayOtherParam) {
        this.requestPayOtherParam = requestPayOtherParam;
    }

    public boolean adddetail(Map<String, String> map) {
        if (null == detail)
            detail = new ArrayList<>();
        return this.detail.add(map);
    }

    public String getRequestPayChannelBankName() {
        return requestPayChannelBankName;
    }

    public void setRequestPayChannelBankName(String requestPayChannelBankName) {
        this.requestPayChannelBankName = requestPayChannelBankName;
    }

    public List<Map<String, String>> getDetail() {
        return detail;
    }

    public void setDetail(List<Map<String, String>> detail) {
        this.detail = detail;
    }

    public String getRequestPayCode() {
        return requestPayCode;
    }

    public void setRequestPayCode(String requestPayCode) {
        this.requestPayCode = requestPayCode;
    }

    public String getRequestPayErrorMsg() {
        return requestPayErrorMsg;
    }

    public void setRequestPayErrorMsg(String requestPayErrorMsg) {
        this.requestPayErrorMsg = requestPayErrorMsg;
    }

    public String getRequestPayamount() {
        return requestPayamount;
    }

    public void setRequestPayamount(String requestPayamount) {
        this.requestPayamount = requestPayamount;
    }

    public String getRequestPayOrderId() {
        return requestPayOrderId;
    }

    public void setRequestPayOrderId(String requestPayOrderId) {
        this.requestPayOrderId = requestPayOrderId;
    }

    public String getRequestPayOrderCreateTime() {
        return requestPayOrderCreateTime;
    }

    public void setRequestPayOrderCreateTime(String requestPayOrderCreateTime) {
        this.requestPayOrderCreateTime = requestPayOrderCreateTime;
    }

    public String getRequestPayQRcodeContent() {
        return requestPayQRcodeContent;
    }

    public void setRequestPayQRcodeContent(String requestPayQRcodeContent) {
        this.requestPayQRcodeContent = requestPayQRcodeContent;
    }

    public String getRequestPayQRcodeURL() {
        return requestPayQRcodeURL;
    }

    public void setRequestPayQRcodeURL(String requestPayQRcodeURL) {
        this.requestPayQRcodeURL = requestPayQRcodeURL;
    }

    public long getRequestPayTotalTime() {
        return requestPayTotalTime;
    }

    public void setRequestPayTotalTime(long requestPayTotalTime) {
        this.requestPayTotalTime = requestPayTotalTime;
    }

    public String getRequestPayHtmlContent() {
        return requestPayHtmlContent;
    }

    public void setRequestPayHtmlContent(String requestPayHtmlContent) {
        this.requestPayHtmlContent = requestPayHtmlContent;
    }

    public String getRequestPayJumpToUrl() {
        return requestPayJumpToUrl;
    }

    public void setRequestPayJumpToUrl(String requestPayJumpToUrl) {
        this.requestPayJumpToUrl = requestPayJumpToUrl;
    }

    public long getRequestPayChannelTime() {
        return requestPayChannelTime;
    }

    public void setRequestPayChannelTime(long requestPayChannelTime) {
        this.requestPayChannelTime = requestPayChannelTime;
    }

    public long getRequestPayGetReqpayinfoTime() {
        return requestPayGetReqpayinfoTime;
    }

    public void setRequestPayGetReqpayinfoTime(long requestPayGetReqpayinfoTime) {
        this.requestPayGetReqpayinfoTime = requestPayGetReqpayinfoTime;
    }



    public RequestPayResult(ReqPayInfo reqPayInfo ,String jumpUrl) {
       this.requestPayCode ="SUCCESS";
       this.requestPayOrderId = reqPayInfo.getAPI_ORDER_ID();
       this.requestPayamount = reqPayInfo.getAPI_AMOUNT();
       this.requestPayChannelBankName = reqPayInfo.getAPI_CHANNEL_BANK_NAME();
       this.requestPayOrderCreateTime = reqPayInfo.getAPI_OrDER_TIME();
       this.requestPayJumpToUrl = reqPayInfo.getAPI_JUMP_URL_PREFIX().concat(jumpUrl);  //建议/开头
    }


    public RequestPayResult(boolean result,ReqPayInfo reqPayInfo ,String errorMsg) {
        this.requestPayCode =result==true?"SUCCESS":"ERROR";
        this.requestPayOrderId = reqPayInfo.getAPI_ORDER_ID();
        this.requestPayamount = reqPayInfo.getAPI_AMOUNT();
        this.requestPayChannelBankName = reqPayInfo.getAPI_CHANNEL_BANK_NAME();
        this.requestPayOrderCreateTime = reqPayInfo.getAPI_OrDER_TIME();
        this.requestPayErrorMsg = errorMsg;
    }

}
