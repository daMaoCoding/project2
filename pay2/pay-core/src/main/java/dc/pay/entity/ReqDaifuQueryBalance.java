package dc.pay.entity;

import org.apache.commons.lang3.StringUtils;
import org.apache.xpath.operations.Bool;

import java.io.Serializable;
import java.util.Date;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class ReqDaifuQueryBalance implements Serializable {
    private String requestDaifuCode;               //结果 SUCCESS /ERROR
    private String requestDaifuErrorMsg;
    private String requestDaifuOtherParam;         //其他参数
    private String requestDaifuChannelId;
    private String reqyestDaifuChannelMemberId;
    private long   requestDaifuBalance;
    private Date   requestDaifuDateTime = new Date();


    public ReqDaifuQueryBalance(boolean result, String errorMsg) {
        setRequestDaifuCodeWithBoolean(result,errorMsg);
    }

    public ReqDaifuQueryBalance(boolean result) {
        setRequestDaifuCodeWithBoolean(result,null);
    }

    public ReqDaifuQueryBalance(boolean result, String errorMsg,ReqDaifuInfo reqDaifuInfo,long requestDaifuBalance) {
        this(result,errorMsg,reqDaifuInfo);
        this.requestDaifuBalance = requestDaifuBalance;
    }



    public ReqDaifuQueryBalance(boolean result, String errorMsg,ReqDaifuInfo reqDaifuInfo) {
        setRequestDaifuCodeWithBoolean(result,errorMsg);
        if(null!=reqDaifuInfo){
            this.requestDaifuOtherParam = reqDaifuInfo.getAPI_OTHER_PARAM()==null?"":reqDaifuInfo.getAPI_OTHER_PARAM();
            this.requestDaifuOtherParam = reqDaifuInfo.getAPI_OTHER_PARAM()==null?"":reqDaifuInfo.getAPI_OTHER_PARAM();
            this.requestDaifuChannelId = reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()==null?"":reqDaifuInfo.getAPI_CHANNEL_BANK_NAME();
            this.reqyestDaifuChannelMemberId = reqDaifuInfo.getAPI_MEMBERID()==null?"":reqDaifuInfo.getAPI_MEMBERID();
        }
    }


    public String getRequestDaifuCode() {
        return requestDaifuCode;
    }

    public void setRequestDaifuCode(String requestDaifuCode) {
        this.requestDaifuCode = requestDaifuCode;
    }

    public void setRequestDaifuCodeWithBoolean(boolean result,String errorMsg) {
        this.requestDaifuCode =result==true?"SUCCESS":"ERROR";
        if(StringUtils.isNotBlank(errorMsg))  this.requestDaifuErrorMsg = errorMsg;
    }


    public String getRequestDaifuErrorMsg() {
        return requestDaifuErrorMsg;
    }

    public void setRequestDaifuErrorMsg(String requestDaifuErrorMsg) {
        this.requestDaifuErrorMsg = requestDaifuErrorMsg;
    }

    public String getRequestDaifuOtherParam() {
        return requestDaifuOtherParam;
    }

    public void setRequestDaifuOtherParam(String requestDaifuOtherParam) {
        this.requestDaifuOtherParam = requestDaifuOtherParam;
    }

    public String getRequestDaifuChannelId() {
        return requestDaifuChannelId;
    }

    public void setRequestDaifuChannelId(String requestDaifuChannelId) {
        this.requestDaifuChannelId = requestDaifuChannelId;
    }

    public long getRequestDaifuBalance() {
        return requestDaifuBalance;
    }

    public void setRequestDaifuBalance(long requestDaifuBalance) {
        this.requestDaifuBalance = requestDaifuBalance;
    }

    public String getReqyestDaifuChannelMemberId() {
        return reqyestDaifuChannelMemberId;
    }

    public void setReqyestDaifuChannelMemberId(String reqyestDaifuChannelMemberId) {
        this.reqyestDaifuChannelMemberId = reqyestDaifuChannelMemberId;
    }

    public Date getRequestDaifuDateTime() {
        return requestDaifuDateTime;
    }

    public void setRequestDaifuDateTime(Date requestDaifuDateTime) {
        this.requestDaifuDateTime = requestDaifuDateTime;
    }
}
