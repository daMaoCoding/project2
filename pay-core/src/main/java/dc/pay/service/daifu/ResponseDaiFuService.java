package dc.pay.service.daifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResService;
import dc.pay.base.processor.PaymentManager;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.config.PayProps;
import dc.pay.constant.PayEumeration;
import dc.pay.dao.daifu.RequestDaiFuDao;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.service.cache.CacheService;
import dc.pay.service.resDb.PayJmsSender;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */

@Slf4j
@Service
public class ResponseDaiFuService extends PayResService<ResponseDaifuResult>  {

    @Autowired
    public void init(ResDaiFuListService resDaiFuListService,PayProps payProps,PaymentManager paymentManager,  @Qualifier("reqDaiFuDao") RequestDaiFuDao requestDaiFuDao,HandlerUtil handlerUtil, PayJmsSender payJmsSender, CacheService cacheService){
        super.resDaiFuListService=resDaiFuListService;
        super.payProps=payProps;
        super.paymentManager=paymentManager;
        super.requestDaiFuDao=requestDaiFuDao;
        super.handlerUtil=handlerUtil;
        super.payJmsSender=payJmsSender;
        super.cacheService=cacheService;
    }



    @Override
    public ResponseDaifuResult receive(String channelName, Map<String, String> responseMap){
        if(StringUtils.isBlank(channelName)){
            log.error("响应代付：receive：通道名或参数为空,通道名：{}，参数:{}",channelName, JSON.toJSONString(responseMap));
            return  new ResponseDaifuResult(false, "[响应代付]通道名或参数为空");
        }
        long start = System.currentTimeMillis();
        String ResPayRemoteIp =responseMap.containsKey(ResPayRemoteIpKey)?responseMap.get(ResPayRemoteIpKey):null; responseMap.remove(ResPayRemoteIpKey);
        ResponseDaifuResult responseDaifuResult = paymentManager.responseDaifu(channelName, responseMap);
        ReqDaifuInfo reqDaifuInfo = getReqInfo(responseDaifuResult);
        long usedTime = System.currentTimeMillis()-start;
        responseDaifuResult.setResponseDaifuTotalTime(usedTime);

        /**************保存响应代付流水信息*********************/
        if(payProps.isEnableResDaiFuInfoListToDb()){
            ResDaiFuList resDaiFuList =  new ResDaiFuList(responseMap, responseDaifuResult, reqDaifuInfo, 0 , null, null, ResPayRemoteIp);
            saveAndResDbMsgNextTime(resDaiFuList,false); //响应数据库，保存数据，发消息
             //cacheService.cacheTjResponsePayByLocation(respay);//redis 按地区统计缓存-响应代付(取消需求)-开启将报错，db 没传用户ip
        }
        /********************************************/

        return responseDaifuResult;
    }





    //@Async("asyncExecutorOne")  synchronized
    public String saveAndResDbMsgNextTime(ResDaiFuList resDaiFuList,boolean isBufa){ //补发
        String dbMsg=null;
        String restMsg=null;
        boolean needSave =true;
        if(null!=resDaiFuList   && (respDbIsNotEnough(resDaiFuList.getResDbCount()) || isBufa)   ){
            if(null!=resDaiFuList.getResponseDaifuResult() && resDaiFuList.getResponseDaifuResult().getResponseDaifuCode().equalsIgnoreCase("SUCCESS") && !resDaiFuList.getOrderId().startsWith("T")
                    && (resDaiFuList.getResponseDaifuResult().getResponseOrderState().equalsIgnoreCase(PayEumeration.DAIFU_RESULT.ERROR.getCodeValue())    ||  resDaiFuList.getResponseDaifuResult().getResponseOrderState().equalsIgnoreCase(PayEumeration.DAIFU_RESULT.SUCCESS.getCodeValue()))){
                dbMsg = responseForDbInterface(resDaiFuList.getResponseDaifuResult());  //通知数据库
                log.info("通知DB[代付]结果，数据库返回：{}",dbMsg);
                String resDbResult = getResDbResult(dbMsg,"result","1")?"SUCCESS":"ERROR";
                resDaiFuList.setResDbResult(resDbResult);

                //通知DB成功后通知Rest，临时日志
                //if("SUCCESS".equalsIgnoreCase(resDbResult)){
                    //restMsg = responseForRestInterface(resDaiFuList.getResponseDaifuResult()); //通知Rest
                   // log.info("[响应REST通知代付状态-结束]:,订单信息：{}，REST返回：{}",JSON.toJSONString(resDaiFuList.getResponseDaifuResult()),restMsg);
                //}

                //通知rest 成功后结束，否则继续补发
                //if(StringUtils.isNotBlank(restMsg) && restMsg.contains("200")){
                //    resDaiFuList.setResDbResult(resDbResult);
               // }else{
                  //  resDaiFuList.setResDbResult("ERROR");
                  //  dbMsg = restMsg;
                   // log.error("[响应REST通知代付状态-失败]:,订单信息：{}，REST返回：{}",JSON.toJSONString(resPayList.getResponsePayResult()),restMsg);
                  //  log.error("[响应REST通知代付状态-失败]:,订单信息：{}，DB返回：{}",JSON.toJSONString(resDaiFuList.getResponseDaifuResult()),restMsg);
                //}
                resDaiFuList.setResDbMsg(dbMsg);
                resDaiFuList.setResDbCount(resDaiFuList.getResDbCount()+1); //修改通知次数
            }

            //自动查询状态是支付中或未知，不保存流水记录。
            if(null!=resDaiFuList && resDaiFuList.getResDaifuRemoteIp().equalsIgnoreCase(PayEumeration.queryDaifuFromAuto) &&(  resDaiFuList.getOrderState().equalsIgnoreCase(PayEumeration.DAIFU_RESULT.PAYING.getCodeValue())  || resDaiFuList.getOrderState().equalsIgnoreCase(PayEumeration.DAIFU_RESULT.UNKNOW.getCodeValue()  ))){
                needSave = false;
            }

            if(null!=resDaiFuList && needSave ) resDaiFuListService.save(resDaiFuList); //修改通知
            resDbMsgNextTime(resDaiFuList);//再次发送通知
        }

       if(null!=resDaiFuList) resDaiFuList = null;
        return dbMsg;
    }




    //响应数据库
    @Override
    public String  responseForDbInterface(ResponseDaifuResult responseDaifuResult) {
        if(responseDaifuResult.getResponseDaifuCode().equalsIgnoreCase("SUCCESS")){  // && responsePayResult.getResponseOrderState().equalsIgnoreCase("SUCCESS")
            return handlerUtil.responseForDbInterface(responseDaifuResult,2);
        }else{
            log.error("[充值回调验证失败，无需通知数据库]：{}", JSON.toJSONString(responseDaifuResult));
        }
        return null;
    }


    //响应REST
    @Override
    public String  responseForRestInterface(ResponseDaifuResult responseDaifuResult) {
        try{
            if(responseDaifuResult.getResponseDaifuCode().equalsIgnoreCase("SUCCESS")){  // && responseDaifuResult.getResponseOrderState().equalsIgnoreCase("SUCCESS")
                Map<String,String> spHeader = new HashMap<String,String>() {
                    {
                        put("REQUEST_CLIENT","PAY_SERVER");
                    }
                };

                Map<String,String> spResult = new HashMap<String,String>() {
                    {
                        put("oid",responseDaifuResult.getResponseDaifuOid());
                        put("orderNo",responseDaifuResult.getResponseOrderID());
                    }
                };
                return  handlerUtil.sendToMS(payProps.getSendPayresultToRest(),spHeader, JSON.toJSONString(spResult), HttpMethod.POST);
            }else{
                log.error("[代付回调验证失败，无需通知REST]：{}", JSON.toJSONString(responseDaifuResult));
            }
        }catch (Exception e){ }
        return null;
    }


     public   ReqDaifuInfo getReqInfo(ResponseDaifuResult responseDaifuResult) {
        try {
            if(null!=responseDaifuResult && null!=responseDaifuResult.getReqDaifuInfo()){
                return responseDaifuResult.getReqDaifuInfo();
            }else{
                String responseOrderID = responseDaifuResult.getResponseOrderID();
                if(StringUtils.isBlank(responseOrderID)) return null;
                return  requestDaiFuDao.getReqDaifuInfo(responseOrderID);
            }
        } catch (PayException e) {
           return null;
        }
    }





}
