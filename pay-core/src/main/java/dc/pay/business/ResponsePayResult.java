package dc.pay.business;

/**
 * Created by admin on 2017/5/20.
 */

import dc.pay.constant.PayEumeration;

import java.io.Serializable;

/**
 * ************************
 * @author tony 3556239829
 */
public class ResponsePayResult implements Serializable {
    private String responsePayCode;
    private String responseOrderID;
    private String responseOrderState;
    private String responsePayErrorMsg;
    private long responsePayTotalTime;
    private String responsePayMsg;
    private String responsePayChannel;
    private String responsePayAmount;
    private String responsePayMemberId;
    private String responsePayOtherParam;
    private  String responsePayOid;
    private  String responsePaySign;

    private boolean isPayed;

    public String getResponsePayOid() {
        return responsePayOid;
    }

    public void setResponsePayOid(String responsePayOid) {
        this.responsePayOid = responsePayOid;
    }

    public String getResponsePaySign() {
        return responsePaySign;
    }

    public void setResponsePaySign(String responsePaySign) {
        this.responsePaySign = responsePaySign;
    }

    public String getResponsePayMemberId() {
        return responsePayMemberId;
    }

    public void setResponsePayMemberId(String responsePayMemberId) {
        this.responsePayMemberId = responsePayMemberId;
    }

    public String getResponsePayChannel() {
        return responsePayChannel;
    }

    public void setResponsePayChannel(String responsePayChannel) {
        this.responsePayChannel = responsePayChannel;
    }

    public String getResponsePayAmount() {
        return responsePayAmount;
    }

    public void setResponsePayAmount(String responsePayAmount) {
        this.responsePayAmount = responsePayAmount;
    }

    public String getResponsePayMsg() {
        return responsePayMsg;
    }

    public void setResponsePayMsg(String responsePayMsg) {
        this.responsePayMsg = responsePayMsg;
    }

    public String getResponseOrderID() {
        return responseOrderID;
    }

    public void setResponseOrderID(String responseOrderID) {
        this.responseOrderID = responseOrderID;
    }

    public ResponsePayResult(PayEumeration.RESPONSE_PAY_CODE response_pay_code, String responsePayErrorMsg) {
        this.responsePayCode = response_pay_code.getCodeValue();
        this.responsePayErrorMsg = responsePayErrorMsg;
    }

    public String getResponsePayCode() {
        return responsePayCode;
    }

    public void setResponsePayCode(String responsePayCode) {
        this.responsePayCode = responsePayCode;
    }

    public String getResponseOrderState() {
        return responseOrderState;
    }

    public void setResponseOrderState(String responseOrderState) {
        this.responseOrderState = responseOrderState;
    }

    public String getResponsePayErrorMsg() {
        return responsePayErrorMsg;
    }

    public void setResponsePayErrorMsg(String responsePayErrorMsg) {
        this.responsePayErrorMsg = responsePayErrorMsg;
    }

    public long getResponsePayTotalTime() {
        return responsePayTotalTime;
    }

    public void setResponsePayTotalTime(long responsePayTotalTime) {
        this.responsePayTotalTime = responsePayTotalTime;
    }

    public ResponsePayResult() {

    }

    public boolean isPayed() {
        return isPayed;
    }

    public void setPayed(boolean payed) {
        isPayed = payed;
    }

    public ResponsePayResult(String responsePayCode, String responseOrderID, String responseOrderState) {
        this.responsePayCode = responsePayCode;
        this.responseOrderID = responseOrderID;
        this.responseOrderState = responseOrderState;
    }

    public String getResponsePayOtherParam() {
        return responsePayOtherParam;
    }

    public void setResponsePayOtherParam(String responsePayOtherParam) {
        this.responsePayOtherParam = responsePayOtherParam;
    }

}
