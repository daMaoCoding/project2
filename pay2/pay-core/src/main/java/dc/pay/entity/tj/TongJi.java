package dc.pay.entity.tj;

import dc.pay.base.BaseEntity;
import org.apache.commons.lang.StringUtils;

import java.util.Comparator;
import java.util.Date;

/**
 * ************************
 *
 * @author tony 3556239829
 */

public class TongJi  extends BaseEntity  implements Comparable<TongJi> {

//    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
//    Date API_ORDER_TIME;

    String channelCName; //通道中文名称(全)  掌托支付 / 网银 / 中国建设银行

    String channelCoCName; //通道服务商中文名  HandlerUtil.getAllPayChannelAndCoCname().get(tongJi.getPageChannelName())  掌托支付
    String channelCNameShort; //通道中文名称(短)  "网银 / 中国建设银行
    String channelId;//通道前缀  HandlerUtil.getChannelAndCoId().get(tongJi.getPageChannelName())   AIMISEN


    //数据库查询结果
    String reqSum;//请求支付数
    String reqAmount;//请求支付金额
    String reqResult;//请求支付结果
    String reqChannel;//请求支付通道
    String resSum; //响应支付数
    String resAmount;//响应支付金额
    String resResult;//响应支付结果
    String resChannel;//响应支付通道

    //统计数据库字段
    String reqTimeStmp;  //2017101616 按时间统计
    String resTimeStmp;  //2017101616 按时间统计


    //查询
    String riQiFanWei; //页面日期范围
    String channelName;//页面通道名称
    String tongJiType; //统计类型
    String channelPrefix;//页面第三方前缀
    String oid;//业主OID


    //详细分组统计
    String  reqOid; //业主OID
    String  reqMemberID ; //商户号


    //页面显示结果
    String pageChannelName;//通道名称
    String pageReqSum; //请求总数-bean中计算
    String pageReqSuccessSum;//请求成功数
    String pageReqErrorSum;//请求失败数

    String pageResSum; //响应总数-bean中计算
    String pageResSuccessSum;//响应成功数
    String pageResErrorSum;//响应失败数

    String reqSuccessDivReqSum;//请求成功÷请求总数%
    String resSuccessDivReqSuccess;//响应成功÷请求成功%

    String pageResSuccessAmount;//成功入款数
    String pageReqSuccessAmount;//成功请求款


    //页面排序
    String orderBy;  //排序字段
    String  sort;  //排序方法，asc desc

    double  reqSuccessDivReqSumD;       //请求成功率
    double  resSuccessDivReqSuccessD;  //支付成功率
    long pageResSuccessSumL;       //成功次数
    long pageResSuccessAmountL;    //成功次数


    //定时统计汇总
    private Date timeStmp; //统计操作时间
    private Date tjTimeStmp; //统计结果日期



    //  return StringUtils.isBlank(getReqSuccessDivReqSum())?0:Integer.parseInt(getReqSuccessDivReqSum());


    public double getReqSuccessDivReqSumD() {
        return StringUtils.isBlank(getReqSuccessDivReqSum())?0:Double.parseDouble(getReqSuccessDivReqSum());
    }

    public double getResSuccessDivReqSuccessD() {
        return StringUtils.isBlank(getResSuccessDivReqSuccess())?0:Double.parseDouble(getResSuccessDivReqSuccess());
    }

    public long getPageResSuccessSumL() {
        return StringUtils.isBlank(getPageResSuccessSum())?0:Long.parseLong(getPageResSuccessSum());
    }

    public long getPageResSuccessAmountL() {
        return StringUtils.isBlank(getPageResSuccessAmount())?0:Long.parseLong(getPageResSuccessAmount());
    }



    public String getOrderBy() {
        return orderBy;
    }

    public String getSort() {
        return sort;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }


    public String getPageReqErrorSum() {
        return pageReqErrorSum;
    }

    public void setPageReqErrorSum(String pageReqErrorSum) {
        this.pageReqErrorSum = pageReqErrorSum;
    }

    public String getPageResSum() {
        return pageResSum;
    }


    public void setPageResSum(String pageResSum) {
        this.pageResSum = pageResSum;
    }

    public void setPageResSum() {
        int resS = StringUtils.isNotBlank(pageResSuccessSum)?Integer.parseInt(pageResSuccessSum):0;
        int resE = StringUtils.isNotBlank(pageResErrorSum)?Integer.parseInt(pageResErrorSum):0;
        this.pageResSum = (resS+ resE)+"";
    }


    public String getPageResErrorSum() {
        return pageResErrorSum;
    }

    public void setPageResErrorSum(String pageResErrorSum) {
        this.pageResErrorSum = pageResErrorSum;
    }

    public String getReqTimeStmp() {
        return reqTimeStmp;
    }

    public void setReqTimeStmp(String reqTimeStmp) {
        this.reqTimeStmp = reqTimeStmp;
    }

    public String getResTimeStmp() {
        return resTimeStmp;
    }

    public void setResTimeStmp(String resTimeStmp) {
        this.resTimeStmp = resTimeStmp;
    }

    public String getTongJiType() {
        return StringUtils.isBlank(tongJiType)?"ALL":tongJiType;  //默认统计类型通道
    }

    public void setTongJiType(String tongJiType) {
        this.tongJiType = tongJiType;
    }

    public TongJi() {
    }

    public String getPageResSuccessAmount() {
        return StringUtils.isBlank(pageResSuccessAmount)?"0":pageResSuccessAmount;
    }

    public void setPageResSuccessAmount(String pageResSuccessAmount) {
        this.pageResSuccessAmount = pageResSuccessAmount;
    }

    public String getPageReqSuccessAmount() {
        return StringUtils.isNotBlank(pageReqSuccessAmount)?pageReqSuccessAmount:"0";
    }

    public void setPageReqSuccessAmount(String pageReqSuccessAmount) {
        this.pageReqSuccessAmount = pageReqSuccessAmount;
    }

    public String getReqSum() {
        return reqSum;
    }

    public void setReqSum(String reqSum) {
        this.reqSum = reqSum;
    }

    public String getReqAmount() {
        return reqAmount;
    }

    public void setReqAmount(String reqAmount) {
        this.reqAmount = reqAmount;
    }

    public String getReqResult() {
        return reqResult;
    }

    public void setReqResult(String reqResult) {
        this.reqResult = reqResult;
    }

    public String getReqChannel() {
        return reqChannel;
    }

    public void setReqChannel(String reqChannel) {
        this.reqChannel = reqChannel;
    }

    public String getResSum() {
        return resSum;
    }

    public void setResSum(String resSum) {
        this.resSum = resSum;
    }

    public String getResAmount() {
        return resAmount;
    }

    public void setResAmount(String resAmount) {
        this.resAmount = resAmount;
    }

    public String getResResult() {
        return resResult;
    }

    public void setResResult(String resResult) {
        this.resResult = resResult;
    }

    public String getResChannel() {
        return resChannel;
    }

    public void setResChannel(String resChannel) {
        this.resChannel = resChannel;
    }

    public String getRiQiFanWei() {
        return riQiFanWei;
    }

    public void setRiQiFanWei(String riQiFanWei) {
        this.riQiFanWei = riQiFanWei;
    }

    public String getChannelName() {
        return StringUtils.isBlank(channelName)?"":channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelPrefix() {
        return StringUtils.isBlank(channelPrefix)?"":channelPrefix;
    }

    public void setChannelPrefix(String channelPrefix) {
        this.channelPrefix = channelPrefix;
    }

    public String getPageChannelName() {
        return pageChannelName;
    }

    public void setPageChannelName(String pageChannelName) {
        this.pageChannelName = pageChannelName;
    }

    public String getPageReqSum() {
        return StringUtils.isBlank(pageReqSum)?"0":pageReqSum;
    }

    public void setPageReqSum(String pageReqSum) {
        this.pageReqSum = pageReqSum;
    }


    public void setPageReqSum() {
        int reqS = StringUtils.isNotBlank(pageReqSuccessSum)?Integer.parseInt(pageReqSuccessSum):0;
        int reqE = StringUtils.isNotBlank(pageReqErrorSum)?Integer.parseInt(pageReqErrorSum):0;
        this.pageReqSum =  (reqS+ reqE)+"";
    }


    public String getChannelCName() {
        return channelCName;
    }

    public void setChannelCName(String channelCName) {
        this.channelCName = channelCName;
    }

    public String getPageReqSuccessSum() {
        return StringUtils.isBlank(pageReqSuccessSum)?"0":pageReqSuccessSum;
    }

    public void setPageReqSuccessSum(String pageReqSuccessSum) {
        this.pageReqSuccessSum = pageReqSuccessSum;
    }

    public String getPageResSuccessSum() {
        return StringUtils.isBlank(pageResSuccessSum)?"0":pageResSuccessSum;
    }

    public void setPageResSuccessSum(String pageResSuccessSum) {
        this.pageResSuccessSum = pageResSuccessSum;
    }

    public String getReqSuccessDivReqSum() {
        return reqSuccessDivReqSum;
    }

    public void setReqSuccessDivReqSum(String reqSuccessDivReqSum) {
        this.reqSuccessDivReqSum = reqSuccessDivReqSum;
    }

    public void setReqSuccessDivReqSum() {
        double reqS =  StringUtils.isNotBlank(pageReqSuccessSum)?Double.parseDouble(pageReqSuccessSum):0;
        double reqSum =  Double.parseDouble(getPageReqSum());
        if(reqSum<=0){
            this.reqSuccessDivReqSum = "0.00";
        }else{
            this.reqSuccessDivReqSum =String.format("%.2f", reqS/reqSum*100) ;
        }
    }



    public String getResSuccessDivReqSuccess() {
        return resSuccessDivReqSuccess;
    }

    public void setResSuccessDivReqSuccess(String resSuccessDivReqSuccess) {
        this.resSuccessDivReqSuccess = resSuccessDivReqSuccess;
    }

    public void setResSuccessDivReqSuccess() {
        double resS =  StringUtils.isNotBlank(pageResSuccessSum)?Double.parseDouble(pageResSuccessSum):0;
        double reqS =  StringUtils.isNotBlank(pageReqSuccessSum)?Double.parseDouble(pageReqSuccessSum):0;
        if(reqS<=0){
            this.resSuccessDivReqSuccess = "0.00";
        }else{
            this.resSuccessDivReqSuccess =String.format("%.2f", resS/reqS*100) ;
        }
    }




    public void initTongji(){
        setPageResSum();
        setPageReqSum();
        setReqSuccessDivReqSum();
        setResSuccessDivReqSuccess();
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

    @Override
    public int compareTo(TongJi o) {
        Double obj1 = Double.parseDouble(this.getResSuccessDivReqSuccess());
        Double obj2 =  Double.parseDouble(o.getResSuccessDivReqSuccess());
        return   obj2.compareTo(obj1);
    }

    public String getOid() {
        return StringUtils.isBlank(oid)?"ALL":oid;  //默认统计OID
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public static class Comparators {
        public static final Comparator<TongJi> ChengGongLv = (TongJi o1, TongJi o2) -> Double.compare(Double.parseDouble(o2.getResSuccessDivReqSuccess()),Double.parseDouble(o1.getResSuccessDivReqSuccess()));
        public static final Comparator<TongJi> RuJuanJinEr = (TongJi o1, TongJi o2) ->  Long.compare(Long.parseLong(o2.getPageResSuccessAmount()), Long.parseLong(o1.getPageResSuccessAmount()));


        public static final Comparator<TongJi> channelStatus = (TongJi o1, TongJi o2) -> {
            int compare = Double.compare(Double.parseDouble(o1.getReqSuccessDivReqSum()), Double.parseDouble(o2.getReqSuccessDivReqSum()));
            if(compare==0){
                compare = Double.compare(Double.parseDouble(o1.getResSuccessDivReqSuccess()),Double.parseDouble(o2.getResSuccessDivReqSuccess()));
            }
            return  compare;
        };


    }


    public String getReqOid() {
        return reqOid;
    }

    public void setReqOid(String reqOid) {
        this.reqOid = reqOid;
    }

    public String getReqMemberID() {
        return reqMemberID;
    }

    public void setReqMemberID(String reqMemberID) {
        this.reqMemberID = reqMemberID;
    }

    public Date getTimeStmp() {
        return timeStmp;
    }

    public void setTimeStmp(Date timeStmp) {
        this.timeStmp = timeStmp;
    }

    public Date getTjTimeStmp() {
        return tjTimeStmp;
    }

    public void setTjTimeStmp(Date tjTimeStmp) {
        this.tjTimeStmp = tjTimeStmp;
    }
}


