package dc.pay.entity.po;

public class SortedChannel implements Comparable<SortedChannel> {
    int sn;  //排序
    String channelName;  //通道名称
    String channelCName;   //通道名称
    String successPayPercent;  //成功支付率
    String resSuccessAmount;  //成功金额
    String oid; //业主oid
    String memberId;//商户号

    //新增需求需要增加 响应成功数，请求成功数
    String reqSuccessSum;
    String resSuccessSum;

    public SortedChannel(int sn, String channelName, String channelCName, String successPayPercent,String resSuccessAmount,String oid,String memberId,String reqSuccessSum,String resSuccessSum) {
        this.sn = sn;
        this.channelName = channelName;
        this.channelCName = channelCName;
        this.successPayPercent = successPayPercent;
        this.resSuccessAmount = resSuccessAmount;
        this.oid = oid;
        this.memberId = memberId;
        this.reqSuccessSum = reqSuccessSum;
        this.resSuccessSum = resSuccessSum;
    }

    public void setResSuccessAmount(String resSuccessAmount) {
        this.resSuccessAmount = resSuccessAmount;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public int getSn() {
        return sn;
    }

    public void setSn(int sn) {
        this.sn = sn;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelCName() {
        return channelCName;
    }

    public void setChannelCName(String channelCName) {
        this.channelCName = channelCName;
    }

    public String getSuccessPayPercent() {
        return successPayPercent;
    }

    public void setSuccessPayPercent(String successPayPercent) {
        this.successPayPercent = successPayPercent;
    }



    @Override
    public int compareTo(SortedChannel o) {
        Double obj1 = Double.parseDouble(this.getSuccessPayPercent());
        Double obj2 =  Double.parseDouble(o.getSuccessPayPercent());
        return   obj2.compareTo(obj1);
    }

    public String getResSuccessAmount() {
        return resSuccessAmount;
    }


    public String getReqSuccessSum() {
        return reqSuccessSum;
    }

    public void setReqSuccessSum(String reqSuccessSum) {
        this.reqSuccessSum = reqSuccessSum;
    }

    public String getResSuccessSum() {
        return resSuccessSum;
    }

    public void setResSuccessSum(String resSuccessSum) {
        this.resSuccessSum = resSuccessSum;
    }
}
