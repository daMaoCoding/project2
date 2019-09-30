package dc.pay.service.daifu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PaymentManager;
import dc.pay.business.RequestDaifuResult;
import dc.pay.business.RequestPayResult;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.config.PayProps;
import dc.pay.config.RunTimeInfo;
import dc.pay.constant.PayEumeration;
import dc.pay.dao.daifu.RequestDaiFuDao;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.ReqDaifuQueryBalance;
import dc.pay.entity.bill.Bill;
import dc.pay.entity.daifu.ReqDaiFuList;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.service.bill.BillService;
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
public class RequestDaiFuService {
    private static final Logger log =  LoggerFactory.getLogger(RequestDaiFuService.class);

    @Autowired
    @Qualifier("reqDaiFuDao")
    private RequestDaiFuDao reqDaiFuDao;

    @Autowired
    private ReqDaiFuListService reqDaiFuListService;

    @Autowired
    private ResponseDaiFuService responseDaiFuService;

    @Autowired
    private PayProps payProps;

    @Autowired
    private PaymentManager paymentManager;

    @Autowired
    HandlerUtil handlerUtil;

    @Autowired
    RunTimeInfo runTimeInfo;



    /**获取订单代付信息*/
    public RequestDaifuResult requestDaifuResultByIdSaveDB(String orderId, Map<String,String> resParams){
        ReqDaifuInfo reqDaifuInfo = null;
        long start = getNowMilliSecond();
        try {
            reqDaifuInfo = getReqDaifuInfo(orderId,resParams);
        } catch (PayException e) {
            reqDaifuInfo = new ReqDaifuInfo(orderId);
        }
        if(null!=reqDaifuInfo && MapUtils.isNotEmpty(resParams)){
            reqDaifuInfo.setResParams(resParams);
        }
        long end  = (getNowMilliSecond())-start;  //订单支付信息耗时
        RequestDaifuResult requestDaifuResult = getRequesDaifuResultByReqPayInfoSaveDB(reqDaifuInfo,end);
        return requestDaifuResult;
    }




    /**订单号代付信息*/
    public ReqDaifuInfo getReqDaifuInfo(String orderId, Map<String,String> resParams) throws PayException{
        if(StringUtils.isNotBlank(orderId)){
            ReqDaifuInfo reqPayInfo =  reqDaiFuDao.getReqDaifuInfo(orderId,resParams);
            return reqPayInfo;
        }else{return null;}
    }




    /**获取订单代付信息 */
    public RequestDaifuResult getRequesDaifuResultByReqPayInfoSaveDB(ReqDaifuInfo reqDaiFuInfo,long daifuInfoTime){
             long start = getNowMilliSecond();
             RequestDaifuResult requestDaifuResult = getRequestDaifuResultByReqDaifuInfo(reqDaiFuInfo);
             long channelTime = (getNowMilliSecond())-start;
             requestDaifuResult.setRequestDaifuTotalTime(daifuInfoTime+channelTime);
             requestDaifuResult.setRequestDaifuChannelTime(channelTime);
             requestDaifuResult.setRequestDaifuGetReqDaifuInfoTime(daifuInfoTime);
            /**************保存请求流水信息*********************/
            saveReqDaifuInfoListInDb(reqDaiFuInfo,payProps,reqDaiFuListService,requestDaifuResult);//数据库 //
            //cacheService.cacheTjRequestPayByLocation(new ReqPayList(reqPayInfo, requestPayResult));//redis 按地区统计缓存-请求支付(取消需求)
            /********************************************/
            return  requestDaifuResult;
    }




    /**保存请求代付流水信息*/
    private void saveReqDaifuInfoListInDb(ReqDaifuInfo reqDaifuInfo, PayProps payProps, ReqDaiFuListService reqDaiFuListService, RequestDaifuResult requestDaifuResult)  {
        if(payProps.isEnableReqDaifuInfoListToDb()){
            try {
                if(StringUtils.isNotBlank(reqDaifuInfo.getAPI_KEY())){
                    reqDaifuInfo.setAPI_KEY(RsaUtil.encryptAndCache(reqDaifuInfo.getAPI_KEY())); //加密存储
                }
                ReqDaiFuList reqDaiFuList = new ReqDaiFuList(reqDaifuInfo, requestDaifuResult);
                reqDaiFuListService.save(reqDaiFuList);
                //List<ReqPayList> all = reqPayService.getAll(null);
            } catch (Exception ex) {
                log.error("保存请求[代付]流水信息出错：{}",ex.getMessage(), ex);
            }
        }
    }


    /** 获取订单代付信息*/
    public RequestDaifuResult getRequestDaifuResultByReqDaifuInfo(ReqDaifuInfo reqDaifuInfo) {
        return paymentManager.requestDaifu(reqDaifuInfo); //请求代付结果
    }



    /**查询代付结果 */
    public ResponseDaifuResult queryDaifuResultById(String orderId, Map<String,String> resParams){
        ReqDaifuInfo reqDaifuInfo = null;
        String queryDaifuFrom =( resParams!=null&&resParams.containsKey(PayEumeration.queryDaifuFromKey))?resParams.get(PayEumeration.queryDaifuFromKey):PayEumeration.queryDaifuFromAdmin;
        long start = getNowMilliSecond();
        try {
            reqDaifuInfo = getReqDaifuInfo(orderId,resParams);
        } catch (PayException e) {
            reqDaifuInfo = new ReqDaifuInfo(orderId);
        }
        reqDaifuInfo.setAPI_ORDER_ID(orderId);
        reqDaifuInfo.setQueryFrom(queryDaifuFrom);
        if(null!=reqDaifuInfo && MapUtils.isNotEmpty(resParams)){
            reqDaifuInfo.setResParams(resParams);
        }
        long end  = (getNowMilliSecond())-start;  //订单支付信息耗时
        ResponseDaifuResult responseDaifuResult = queryDaifuResultByReqDaifuInfo(reqDaifuInfo,end);
        return responseDaifuResult;
    }


    /**查询代付结果*/
    public ResponseDaifuResult queryDaifuResultByReqDaifuInfo(ReqDaifuInfo reqDaiFuInf,long daifuInfoTime){
        long start = getNowMilliSecond();
        ResponseDaifuResult responseDaifuResult = queryResponseDaifuResultByReqDaifuInfo(reqDaiFuInf);
        long channelTime = (getNowMilliSecond())-start;
        responseDaifuResult.setResponseDaifuTotalTime(daifuInfoTime+channelTime);
        return  responseDaifuResult;
    }



    /** 查询订单代付结果*/
    public ResponseDaifuResult queryResponseDaifuResultByReqDaifuInfo(ReqDaifuInfo reqDaifuInfo) {
        ResponseDaifuResult responseDaifuResult= null;
        ResDaiFuList resDaiFuList = null;
        boolean valReqDaifuInfoResult = false;

//        当查询不到代付信息，表示db生成的代付订单网络问题没传递过来，订单直接取消。（兼容本地开发）
//        ReqDaiFuList reqDaiFuList = reqDaiFuListService.getByOrderId(reqDaifuInfo.getAPI_ORDER_ID());
//
//        if(null==reqDaiFuList ){
//            log.error("查询[代付]无此订单号，支付通道："+reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()+"订单号："+reqDaifuInfo.getAPI_ORDER_ID()+",详情："+JSON.toJSONString(reqDaifuInfo));
//            responseDaifuResult =  new ResponseDaifuResult(true, reqDaifuInfo,"XC:本地流水表无此订单号,可能升级中错过该笔请求。",PayEumeration.DAIFU_RESULT.ERROR);
//            resDaiFuList = new ResDaiFuList(responseDaifuResult, null, null, 0, reqDaifuInfo);
//        }


        //验证代付信息不通过，不进行查询，订单支付状态未知
        try {
//            if(null!=reqDaiFuList){
                  valReqDaifuInfoResult = ValidateUtil.valdataReqDaifuInfo(reqDaifuInfo);
//            }
        }catch (Exception e){
            log.error("查询[代付]参数不正确，支付通道："+reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()+"订单号："+reqDaifuInfo.getAPI_ORDER_ID()+",详情："+JSON.toJSONString(reqDaifuInfo));
            responseDaifuResult =  new ResponseDaifuResult(false, reqDaifuInfo,e.getMessage(),PayEumeration.DAIFU_RESULT.UNKNOW);
            resDaiFuList = new ResDaiFuList(responseDaifuResult, null, null, 0, reqDaifuInfo);
        }


        //查询第三方订单状态，查询失败，订单状态未知
        try {
            if(valReqDaifuInfoResult){
                reqDaifuInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqDaifuInfo.getAPI_KEY())); //解密并缓存KEY original
                resDaiFuList = paymentManager.queryDaifu(reqDaifuInfo);
            }
        } catch (PayException e) {
            log.error("查询[代付]出错，支付通道："+reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()+"订单号："+reqDaifuInfo.getAPI_ORDER_ID()+",详情："+JSON.toJSONString(reqDaifuInfo));
            responseDaifuResult = new ResponseDaifuResult(false, reqDaifuInfo,e.getMessage(),PayEumeration.DAIFU_RESULT.UNKNOW);  //查询失败，订单状态未知
            resDaiFuList = new ResDaiFuList(responseDaifuResult, null, null, 0, reqDaifuInfo);
        }


        //查不出结果，订单状态未知
        if(null==responseDaifuResult)
            responseDaifuResult = new ResponseDaifuResult(false, reqDaifuInfo, "代付查询结果为空", PayEumeration.DAIFU_RESULT.UNKNOW);



        //通知db订单结果。
        if(null!=resDaiFuList && null!=resDaiFuList.getResponseDaifuResult()){
            responseDaifuResult = resDaiFuList.getResponseDaifuResult();
            resDaiFuList.setResDaifuRemoteIp(reqDaifuInfo.getQueryFrom());
            responseDaiFuService.saveAndResDbMsgNextTime(resDaiFuList,false); //查询后发送通知
        }

        return responseDaifuResult;
    }






    /**查询商户第三方余额*/
    public ReqDaifuQueryBalance getQueryDaifuBalance(ReqDaifuInfo reqDaifuInfo) {
        ReqDaifuQueryBalance reqDaifuQueryBalance = null ;
        try {
            if(ValidateUtil.valdataReqDaifuInfoForQueryBalance(reqDaifuInfo)){
                reqDaifuInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqDaifuInfo.getAPI_KEY())); //解密并缓存KEY original
                reqDaifuQueryBalance = paymentManager.getQueryDaifuBalance(reqDaifuInfo);
            }
        } catch (PayException e) {
            return    new ReqDaifuQueryBalance(false,e.getMessage(),reqDaifuInfo);
        }
        if(null==reqDaifuQueryBalance)  return  new ReqDaifuQueryBalance(false,"查询[代付/余额]错误,数据为空",reqDaifuInfo);
        return reqDaifuQueryBalance;
    }







    /**当前时间*/
    private long getNowMilliSecond() {
        return System.nanoTime()/1000000L;
    }



}
