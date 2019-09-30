package dc.pay.entity.po;

public class SortedChannelStatus {

    private String   channelCoCName;           //  = "W付"
    private String   channelCNameShort;        //  = "微信"
    private double   reqSuccessDivReqSum;      //   = "0.00"
    private double   resSuccessDivReqSuccess;  //  = "0.00"
    private boolean  reqWarning = false;      //  = "0" / "1"  报警亮红灯true,不报警亮绿灯false
    private boolean  resWarning = false;      //  = "0" / "1"

    public SortedChannelStatus(String channelCoCName, String channelCNameShort, double reqSuccessDivReqSum, double resSuccessDivReqSuccess, boolean reqWarning, boolean resWarning) {
        this.channelCoCName = channelCoCName;
        this.channelCNameShort = channelCNameShort;
        this.reqSuccessDivReqSum = reqSuccessDivReqSum;
        this.resSuccessDivReqSuccess = resSuccessDivReqSuccess;
        this.reqWarning = reqWarning;
        this.resWarning = resWarning;
    }


    public String getChannelCoCName() {
        return channelCoCName;
    }

    public void setChannelCoCName(String channelCoCName) {
        this.channelCoCName = channelCoCName;
    }

    public String getChannelCNameShort() {
        return channelCNameShort;
    }

    public void setChannelCNameShort(String channelCNameShort) {
        this.channelCNameShort = channelCNameShort;
    }

    public double getReqSuccessDivReqSum() {
        return reqSuccessDivReqSum;
    }

    public void setReqSuccessDivReqSum(double reqSuccessDivReqSum) {
        this.reqSuccessDivReqSum = reqSuccessDivReqSum;
    }

    public double getResSuccessDivReqSuccess() {
        return resSuccessDivReqSuccess;
    }

    public void setResSuccessDivReqSuccess(double resSuccessDivReqSuccess) {
        this.resSuccessDivReqSuccess = resSuccessDivReqSuccess;
    }

    public boolean isReqWarning() {
        return reqWarning;
    }

    public void setReqWarning(boolean reqWarning) {
        this.reqWarning = reqWarning;
    }

    public boolean isResWarning() {
        return resWarning;
    }

    public void setResWarning(boolean resWarning) {
        this.resWarning = resWarning;
    }


}
