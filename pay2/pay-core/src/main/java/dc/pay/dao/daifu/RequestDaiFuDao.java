package dc.pay.dao.daifu;

import dc.pay.base.processor.PayException;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.pay.ReqPayInfoList;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Repository
@Qualifier("reqDaiFuDao")
public class RequestDaiFuDao {

    @Autowired
    HandlerUtil handlerUtil;

    public ReqDaifuInfo getReqDaifuInfo(String orderId, Map<String,String> resParams) throws PayException {
        ReqDaifuInfo daifuInfo = handlerUtil.getReqDaifuInfoByOrderIdNoCache(orderId,resParams); //订单代付信息
        return daifuInfo;
    }


    public ReqDaifuInfo getReqDaifuInfo(String orderId) throws PayException {
        ReqDaifuInfo info = handlerUtil.getReqDaifuInfoByOrderIdNoCache(orderId,null); //订单代付信息
        return info;
    }













  //   @Cacheable(cacheNames="reqpayinfo",key = "#orderId") //caches[0].name  {reqpayinfo}
    public ReqPayInfoList getReqPayInfoList(String orderId) throws PayException {
        ReqPayInfoList payInfoList = handlerUtil.getReqPayinfoByOrderIdBatchNoCache(orderId); //订单支付信息
        return payInfoList;
    }
}
