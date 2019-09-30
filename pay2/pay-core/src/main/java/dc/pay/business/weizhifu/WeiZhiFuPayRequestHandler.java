package dc.pay.business.weizhifu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestPayHandler("WEIZHIFU")
public final class WeiZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

     private static final String   payType = "payType";   //  支付方式
     private static final String   totalAmount = "totalAmount";   //  订单总金额
     private static final String   outTradeNo = "outTradeNo";   //  商户订单号
     private static final String   merchantNumber = "merchantNumber";   //  商户号
     private static final String   subject = "subject";   //  订单标题
     private static final String   body = "body";   //  订单详情
     private static final String   timeStamp = "timeStamp";   //  时间戳
     private static final String   notifyUrl = "notifyUrl";   //  商户回调地
     //secondPayType    String  是   8   ZFB 二级支付方式 ZFB:支付宝WX:微信
     private static final String   secondPayType = "secondPayType";   //  商户回调地






    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
//            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(payType,"xypay");
            payParam.put(totalAmount,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(merchantNumber,channelWrapper.getAPI_MEMBERID());
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
//            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(timeStamp,System.currentTimeMillis()+"" );
            payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(secondPayType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            
        }

        log.debug("[薇支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //body=pay15374299621005394&merchantNumber=MQ913252&notifyUrl=http://www.shizhuqun.com/weizhifupay&outTradeNo=15374299621005394&payType=AliPay&subject=15374299621005394&timeStamp=1537586768416&totalAmount=10&key=e10adc3949ba59abbe56e057f20f883e
        //body=%s&merchantNumber=%s&notifyUrl=%s&outTradeNo=%s&payType=%s&subject=%s&timeStamp=%s&totalAmount=%s&key=%s
        //=%s&=%s&=%s&=%s&=%s&=%s&=%s&=%s&key=%s
//        String paramsStr = String.format("body=%s&merchantNumber=%s&notifyUrl=%s&outTradeNo=%s&payType=%s&secondPayType=%s&subject=%s&timeStamp=%s&totalAmount=%s&key=%s",
//                params.get(body),
        String paramsStr = String.format("merchantNumber=%s&notifyUrl=%s&outTradeNo=%s&payType=%s&secondPayType=%s&subject=%s&timeStamp=%s&totalAmount=%s&key=%s",
//                params.get(body),
                params.get(merchantNumber),
                params.get(notifyUrl),
                params.get(outTradeNo),
                params.get(payType),
                params.get(secondPayType),
                params.get(subject),
                params.get(timeStamp),
                params.get(totalAmount),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[薇支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
            result.put(HTMLCONTEXT,resultStr);
        }else if(StringUtils.isNotBlank(resultStr) ){
            JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "1".equalsIgnoreCase(jsonResultStr.getString("status"))
                    && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data") && jsonResultStr.getJSONObject("data").containsKey("qrCodeAddress")
                    &&StringUtils.isNotBlank(jsonResultStr.getJSONObject("data").getString("qrCodeAddress"))){
                result.put(HandlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT,jsonResultStr.getJSONObject("data").getString("qrCodeAddress"));
            }else {throw new PayException(resultStr); }
        }else{ throw new PayException(EMPTYRESPONSE);}
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[薇支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[薇支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}