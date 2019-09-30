package dc.pay.service.pay;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PaymentManager;
import dc.pay.business.RequestPayResult;
import dc.pay.config.PayProps;
import dc.pay.dao.pay.RequestPayDao;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.pay.ReqPayList;
import dc.pay.service.cache.CacheService;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Service
public class RequestPayService {
    private static final Logger log =  LoggerFactory.getLogger(RequestPayService.class);

    @Autowired
    @Qualifier("reqPayDao")
    private RequestPayDao reqPayDao ;

    @Autowired
    private ReqPayListService reqPayService;

    @Autowired
    private PayProps payProps;

    @Autowired
    private PaymentManager paymentManager;

    @Autowired
    HandlerUtil handlerUtil;

    @Autowired
    CacheService cacheService;



    /**
     * 查询订单号支付信息
     * @param orderId
     * @return
     * @throws PayException
     */
    public ReqPayInfo getReqPayInfo(String orderId,Map<String,String> resParams) throws PayException{
        String sysOrderId =orderId ;
        if(StringUtils.isNotBlank(orderId)){
            ReqPayInfo reqPayInfo =  reqPayDao.getReqPayInfo(sysOrderId,resParams);
            // if(null!=reqPayInfo && orderId.equalsIgnoreCase(reqPayInfo.getAPI_ORDER_ID())){  todo 解锁
                return reqPayInfo;
           // }
           // throw  new PayException(SERVER_MSG.REQUEST_PAY_GET_REQPAYINFO_ID_ERROR);
        }else{return null;}
    }



    /**
     * 获取订单支付信息
     * @param orderId
     * @return
     */
    public RequestPayResult requestPayResultByIdSaveDB(String orderId, Map<String,String> resParams){
        ReqPayInfo reqPayInfo = null;
        long start = getNowMilliSecond();
        try {
            reqPayInfo = getReqPayInfo(orderId,resParams);
        } catch (PayException e) {
            reqPayInfo = new ReqPayInfo(orderId);
        }
        if(null!=reqPayInfo && MapUtils.isNotEmpty(resParams)){
            reqPayInfo.setResParams(resParams);
        }
        long end  = (getNowMilliSecond())-start;  //订单支付信息耗时

        //反扫跳转
        if(null!=reqPayInfo && HandlerUtil.isNotDirectFS(reqPayInfo.getAPI_CHANNEL_BANK_NAME()) && HandlerUtil.isFS(reqPayInfo) && !HandlerUtil.sourceFromPayRest(resParams)){
            //http://localhost:8081/fs/ZHITONGBAO_BANK_WAP_QQ_FS/fs/{ZHITONGBAO_QQ_FS-mYPcE}/{30000}
            String jumpUrl = "/fs/".concat(reqPayInfo.getAPI_CHANNEL_BANK_NAME()).concat("/").concat(reqPayInfo.getAPI_ORDER_ID()).concat("/").concat(reqPayInfo.getAPI_AMOUNT()).concat("/").concat(reqPayInfo.getAPI_Client_IP());
            return  new RequestPayResult(reqPayInfo,jumpUrl);
        }
        RequestPayResult requestPayResult = getRequestPayResultByReqPayInfoSaveDB(reqPayInfo,end);
        return requestPayResult;
    }


    /**
     * 获取订单支付信息
     * @param reqPayInfo
     * @return
     */
    public RequestPayResult getRequestPayResultByReqPayInfoSaveDB(ReqPayInfo reqPayInfo,long payInfoTime){
             long start = getNowMilliSecond();
             RequestPayResult requestPayResult = getRequestPayResultByReqPayInfo(reqPayInfo);
             long channelTime = (getNowMilliSecond())-start;
             requestPayResult.setRequestPayTotalTime(payInfoTime+channelTime);
             requestPayResult.setRequestPayChannelTime(channelTime);
             requestPayResult.setRequestPayGetReqpayinfoTime(payInfoTime);
            /**************保存请求流水信息*********************/
            saveReqPayInfoListInDb(reqPayInfo,payProps,reqPayService,requestPayResult);//数据库 //
            //cacheService.cacheTjRequestPayByLocation(new ReqPayList(reqPayInfo, requestPayResult));//redis 按地区统计缓存-请求支付(取消需求)
            /********************************************/
            mergeHtmlContentJumpUrl(requestPayResult);
            return  requestPayResult;
    }


    /**
     * 获取订单支付信息
     * @param reqPayInfo
     * @return
     */
    public RequestPayResult getRequestPayResultByReqPayInfo(ReqPayInfo reqPayInfo) {
        RequestPayResult requestPayResult= null;//请求支付结果
        try {
            if(ValidateUtil.valdataReqPayInfo(reqPayInfo)){
                reqPayInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqPayInfo.getAPI_KEY())); //解密并缓存KEY original
//            try {
//                reqPayInfo.setAPI_KEY(RsaPrv.decryptAndCache(reqPayInfo.getAPI_KEY())); // mq-pay
//            } catch (com.ddg.mq.pay.PayException e) {
//                throw new PayException("使用mq-pay解密出错");
//            }
                requestPayResult = paymentManager.requestPay(reqPayInfo);
            }
        } catch (PayException e) {
            log.error("获取订单支付信息失败：{}",e.getMessage(), e.getException());
            return new RequestPayResult(e.getMessage(), reqPayInfo.getAPI_ORDER_ID());
        }
        return requestPayResult;
    }




    public RequestPayResult mergeHtmlContentJumpUrl(RequestPayResult requestPayResult) {
        if(null!=requestPayResult && requestPayResult.getRequestPayCode().equalsIgnoreCase("SUCCESS")
                && ( StringUtils.isNotBlank(requestPayResult.getRequestPayJumpToUrl())  ||  StringUtils.isNotBlank(requestPayResult.getRequestPayQRcodeContent()) ) && StringUtils.isNotBlank(requestPayResult.getRequestPayHtmlContent())){
            requestPayResult.setRequestPayHtmlContent("");
        }
        return requestPayResult;
    }




    /**保存请求流水信息*/
    private void  saveReqPayInfoListInDb(ReqPayInfo reqPayInfo,PayProps payProps, ReqPayListService reqPayService,RequestPayResult requestPayResult)  {
        if(payProps.isEnableReqPayinfoListInDb()){
            try {
                if(StringUtils.isNotBlank(reqPayInfo.getAPI_KEY())){
                    reqPayInfo.setAPI_KEY(RsaUtil.encryptAndCache(reqPayInfo.getAPI_KEY())); //加密存储
                }
                ReqPayList reqpay = new ReqPayList(reqPayInfo, requestPayResult);
                reqPayService.save(reqpay);
                //List<ReqPayList> all = reqPayService.getAll(null);
            } catch (Exception ex) {
                log.error("保存请求流水信息出错：{}",ex.getMessage(), ex);
            }
        }
    }


    /*当前时间*/
    private long getNowMilliSecond() {
        return System.nanoTime()/1000000L;
    }

}
