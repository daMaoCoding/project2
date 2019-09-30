package dc.pay.service.pay;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResService;
import dc.pay.base.processor.PaymentManager;
import dc.pay.business.ResponsePayResult;
import dc.pay.config.PayProps;
import dc.pay.constant.PayEumeration;
import dc.pay.dao.pay.RequestPayDao;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.pay.ResPayList;
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
public class ResponsePayService  extends PayResService<ResponsePayResult> {

    @Autowired
    public void init(ResPayListService resPayListService, PayProps payProps, PaymentManager paymentManager, @Qualifier("reqPayDao") RequestPayDao reqPayDao, HandlerUtil handlerUtil, PayJmsSender payJmsSender, CacheService cacheService){
        super.resPayListService=resPayListService;
        super.payProps=payProps;
        super.paymentManager=paymentManager;
        super.reqPayDao=reqPayDao;
        super.handlerUtil=handlerUtil;
        super.payJmsSender=payJmsSender;
        super.cacheService=cacheService;
    }



    @Override
    public  ResponsePayResult receive(String channelName, Map<String, String> responseMap){
        if(StringUtils.isBlank(channelName)){
            log.error("响应支付：receive：通道名或参数为空,通道名：{}，参数:{}",channelName, JSON.toJSONString(responseMap));
            return  new ResponsePayResult(PayEumeration.RESPONSE_PAY_CODE.ERROR, "[响应支付]通道名或参数为空");
        }
        long start = System.currentTimeMillis();
        String ResPayRemoteIp =responseMap.containsKey(ResPayRemoteIpKey)?responseMap.get(ResPayRemoteIpKey):null; responseMap.remove(ResPayRemoteIpKey);
        ResponsePayResult responsePayResult = paymentManager.responsePay(channelName, responseMap);
        ReqPayInfo reqPayInfo = getReqInfo(responsePayResult);
        long usedTime = System.currentTimeMillis()-start;
        responsePayResult.setResponsePayTotalTime(usedTime);
        /**************保存响应流水信息*********************/
        if(payProps.isEnableResPayinfoListInDb()){
                ResPayList respay = new ResPayList(responseMap, responsePayResult,reqPayInfo,0,null,null,ResPayRemoteIp);
                saveAndResDbMsgNextTime(respay,false); //响应数据库，保存数据，发消息
                 //cacheService.cacheTjResponsePayByLocation(respay);//redis 按地区统计缓存-响应支付(取消需求)-开启将报错，db 没传用户ip
        }
        /********************************************/
        return responsePayResult;
    }





    //@Async("asyncExecutorOne")  synchronized
    public   String  saveAndResDbMsgNextTime(ResPayList resPayList,boolean isBufa){ //补发
        String dbMsg=null;
        String restMsg=null;
        if(null!=resPayList && null!=resPayList.getResponsePayResult() &&resPayList.getResponsePayResult().isPayed()){
             resPayListService.save(resPayList);  //响应成功订单保存数据库（强制入款后再手动回调）
        }else {
            if(null!=resPayList   && (respDbIsNotEnough(resPayList.getResDbCount())||isBufa) ){
                if(resPayList.getResponsePayResult().getResponsePayCode().equalsIgnoreCase("SUCCESS") && resPayList.getResponsePayResult().getResponseOrderState().equalsIgnoreCase("SUCCESS")){
                    dbMsg = responseForDbInterface(resPayList.getResponsePayResult());  //通知数据库
                    log.info("通知DB[支付]结果，数据库返回：{}",dbMsg);

                    String resDbResult = getResDbResult(dbMsg,"result","1")?"SUCCESS":"ERROR"; // TODO: 2018/1/10 db上线后打开
                    //String resDbResult =StringUtils.isBlank(dbMsg)?"SUCCESS":"ERROR"; //老系统 db无返回
                    resPayList.setResDbResult(resDbResult);

                    //通知DB成功后通知Rest，临时日志
                    if("SUCCESS".equalsIgnoreCase(resDbResult)){
                        restMsg = responseForRestInterface(resPayList.getResponsePayResult()); //通知Rest
                        log.info("[响应REST通知支付状态-结束]:,订单信息：{}，REST返回：{}",JSON.toJSONString(resPayList.getResponsePayResult()),restMsg);
                    }

                    //通知rest 成功后结束，否则继续补发
                    if(StringUtils.isNotBlank(restMsg) && restMsg.contains("200")){
                        resPayList.setResDbResult(resDbResult);
                    }else{
                        resPayList.setResDbResult("ERROR");
                        dbMsg = restMsg;
                        log.error("[响应REST通知支付状态-失败]:,订单信息：{}，REST返回：{}",JSON.toJSONString(resPayList.getResponsePayResult()),restMsg);
                    }

                    //数据库字段非text
                    if(StringUtils.isNotBlank(dbMsg)&& dbMsg.length()>250){
                        resPayList.setResDbMsg(dbMsg.substring(0,250));
                    }else{
                        resPayList.setResDbMsg(dbMsg);
                    }
                    resPayList.setResDbCount(resPayList.getResDbCount()+1); //修改通知次数
                }

                resPayListService.save(resPayList); //修改通知
                resDbMsgNextTime(resPayList);//再次发送通知
            }
        }
       if(null!=resPayList) resPayList = null;
        return dbMsg;
    }






    //响应数据库
    @Override
    public String  responseForDbInterface(ResponsePayResult responsePayResult) {
        if(responsePayResult.getResponsePayCode().equalsIgnoreCase("SUCCESS") && responsePayResult.getResponseOrderState().equalsIgnoreCase("SUCCESS")){
            return handlerUtil.responseForDbInterface(responsePayResult,1);
        }else{
            log.error("[充值回调验证失败，无需通知数据库]：{}", JSON.toJSONString(responsePayResult));
        }
        return null;
    }



    //响应REST
    @Override
    public String  responseForRestInterface(ResponsePayResult responsePayResult) {
        try{
            if(responsePayResult.getResponsePayCode().equalsIgnoreCase("SUCCESS") && responsePayResult.getResponseOrderState().equalsIgnoreCase("SUCCESS")){
                Map<String,String> spHeader = new HashMap<String,String>() {
                    {
                        put("REQUEST_CLIENT","PAY_SERVER");
                    }
                };

                Map<String,String> spResult = new HashMap<String,String>() {
                    {
                        put("oid",responsePayResult.getResponsePayOid());
                        put("orderNo",responsePayResult.getResponseOrderID());
                    }
                };
                return  handlerUtil.sendToMS(payProps.getSendPayresultToRest(),spHeader, JSON.toJSONString(spResult), HttpMethod.POST);
            }else{
                log.error("[充值回调验证失败，无需通知REST]：{}", JSON.toJSONString(responsePayResult));
            }
        }catch (Exception e){ }
        return null;
    }





    public  ReqPayInfo getReqInfo(ResponsePayResult responsePayResult) {
        try {
            String responseOrderID = responsePayResult.getResponseOrderID();
            if(StringUtils.isBlank(responseOrderID)) return null;
             return  reqPayDao.getReqPayInfo(responseOrderID);
        } catch (PayException e) {
           return null;
        }
    }




}
