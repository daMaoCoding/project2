package dc.pay.dao.pay;

import dc.pay.base.processor.PayException;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.pay.ReqPayInfoList;
import dc.pay.utils.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */

@Repository
@Qualifier("reqPayDao")
public class RequestPayDao {

    private static final Logger log =  LoggerFactory.getLogger(RequestPayDao.class);

    @Autowired
    HandlerUtil handlerUtil;



   //  @Cacheable(cacheNames="reqpayinfo",key = "#orderId") //caches[0].name  {reqpayinfo}
    public ReqPayInfo getReqPayInfo(String orderId) throws PayException {
        ReqPayInfo payInfo = handlerUtil.getReqPayinfoByOrderIdNoCache(orderId,null); //订单支付信息
        return payInfo;
    }

  //  @Cacheable(cacheNames="reqpayinfo",key = "#orderId") //caches[0].name  {reqpayinfo}
    public ReqPayInfo getReqPayInfo(String orderId,Map<String,String> resParams) throws PayException {
        ReqPayInfo payInfo = handlerUtil.getReqPayinfoByOrderIdNoCache(orderId,resParams); //订单支付信息
        return payInfo;
    }

  //   @Cacheable(cacheNames="reqpayinfo",key = "#orderId") //caches[0].name  {reqpayinfo}
    public ReqPayInfoList getReqPayInfoList(String orderId) throws PayException {
        ReqPayInfoList payInfoList = handlerUtil.getReqPayinfoByOrderIdBatchNoCache(orderId); //订单支付信息
        return payInfoList;
    }
}
