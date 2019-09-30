package dc.pay.entity.pay;

import dc.pay.base.processor.PayException;
import dc.pay.constant.PayEumeration;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.comparator.ComparatorReqPayInfo;
import dc.pay.utils.RsaUtil;

import java.util.Collections;
import java.util.List;

/**
 * 批量请求支付
 */
public class ReqPayInfoList {
    List<ReqPayInfo> reqPayInfoList; //请求支付信息
    private String timeOut;  //总超时时间


    public List<ReqPayInfo> getReqPayInfoList() {
        return reqPayInfoList;
    }

    public void setReqPayInfoList(List<ReqPayInfo> reqPayInfoList) {
        this.reqPayInfoList = reqPayInfoList;
    }

    public String getTimeOut() {
        return timeOut == null || "0".equalsIgnoreCase(timeOut) ? String.valueOf(PayEumeration.DEFAULT_TIME_OUT_REQPAY) : timeOut;
    }

    public void setTimeOut(String timeOut) {
        this.timeOut = timeOut;
    }

    public ReqPayInfoList(List<ReqPayInfo> reqPayInfoList, String timeOut) {
        this.reqPayInfoList = reqPayInfoList;
        this.timeOut = timeOut;
    }

    public ReqPayInfoList() {
    }

    public void sortReqpayInfoList(){
        ComparatorReqPayInfo comparator = new ComparatorReqPayInfo();
        Collections.sort(this.reqPayInfoList, comparator);
    }

    public void setAPI_KEY() throws PayException{
        for(ReqPayInfo item : reqPayInfoList){
             item.setAPI_KEY(RsaUtil.decryptAndCache(item.getAPI_KEY()));
            /*try {
                item.setAPI_KEY(RsaPrv.decryptAndCache(item.getAPI_KEY()));
            } catch (com.ddg.mq.pay.PayException e) {
                throw new PayException("使用mq-pay解密出错(Batch)");
            }*/
        }
    }
}
