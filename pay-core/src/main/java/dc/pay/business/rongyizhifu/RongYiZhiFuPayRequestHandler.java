package dc.pay.business.rongyizhifu;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("RONGYIZHIFU")
public final class RongYiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongYiZhiFuPayRequestHandler.class);

    private static final String      MerchantNo = "MerchantNo";
    private static final String      Data = "Data";
    private static final String      Sign = "Sign";

    private static final String   PayType = "PayType";       // "":2,
    private static final String   OrderTitle = "OrderTitle";       // "":"测试订单",
    private static final String   OrderDetails = "OrderDetails";       // "":"测试订单",
    private static final String   MerchantOrderNumber = "MerchantOrderNumber";       // "":"012516281845010002",
    private static final String   OrderAmount = "OrderAmount";       // "":1,  金额（分）
    private static final String   NotifyUrl = "NotifyUrl";       // "":"http://120.76.47.225:8002/Payment/CESHI",
    private static final String   BackUrl = "BackUrl";       // "":"http://120.76.47.225:8002",
    private static final String   UserNumber = "UserNumber";       // "":"1234"



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(PayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(OrderTitle,channelWrapper.getAPI_ORDER_ID());
            payParam.put(OrderDetails,channelWrapper.getAPI_ORDER_ID());
            payParam.put(MerchantOrderNumber,channelWrapper.getAPI_ORDER_ID());
            payParam.put(OrderAmount,channelWrapper.getAPI_AMOUNT());
            payParam.put(NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(BackUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(UserNumber,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[荣亿付支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        String dataStr = "";
        String encStr = JSON.toJSONString(payParam);
        String fullKey=channelWrapper.getAPI_KEY();
        String key = fullKey.substring(fullKey.length()-8);
        String iv = fullKey.substring(0,24);
        try {
            String data64Enc = DesUtil.desEncrypt64(encStr, key, iv);
            dataStr = URLEncoder.encode(data64Enc, "utf-8");
        }catch (Exception e){}
        String paramsStr = String.format("Data=%s&MerchantNo=%s&Key=%s",dataStr,channelWrapper.getAPI_MEMBERID(),channelWrapper.getAPI_KEY());
        String  signResult = HandlerUtil.getMD5UpperCase(paramsStr);

        HashMap<String, String> postParam = Maps.newHashMap();
        postParam.put(MerchantNo,channelWrapper.getAPI_MEMBERID());
        postParam.put(Data,dataStr);
        postParam.put(Sign,signResult);


        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper)  &&  HandlerUtil.isYLKJ(channelWrapper)   &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), postParam );
                resultStr= new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");

                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                String PrePay=null;
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("ErrCode") && "200".equalsIgnoreCase(jsonResultStr.getString("ErrCode")) && jsonResultStr.containsKey("Data")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("Data"))){
                                String qrData64Desc=URLDecoder.decode(jsonResultStr.getString("Data"), "utf-8");
                                String qrJsonStr=DesUtil.desDecrypt64(qrData64Desc, key, iv);
                                JSONObject qrJson = JSON.parseObject(qrJsonStr);
                                //判断对应操作方式：0表示浏览器直接跳转无论什么浏览器，1表示代码模式需要放到页面中自动跳转，2表示二维码链接，只能在指定环境中打开，例如支付宝扫码，微信扫码，1的内容我们已经转成了0的模式
                                if(qrJson!=null && qrJson.containsKey("ResultCode") && "200".equalsIgnoreCase(qrJson.getString("ResultCode")) &&qrJson.containsKey("PrePayType") &&qrJson.containsKey("PrePay") ){
                                            String PrePayType = qrJson.getString("PrePayType");
                                            PrePay = qrJson.getString("PrePay");
                                            if("0".equalsIgnoreCase(PrePayType)){
                                                result.put(JUMPURL, PrePay);
                                                payResultList.add(result);
                                            }else if("1".equalsIgnoreCase(PrePayType)){
                                                result.put(HTMLCONTEXT, PrePay);
                                                payResultList.add(result);
                                            }else if("2".equalsIgnoreCase(PrePayType)){
                                                result.put(QRCONTEXT, PrePay);
                                                payResultList.add(result);
                                            }
                                }else{
                                    throw new PayException(qrJsonStr);
                                }
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                    if(StringUtils.isBlank(PrePay)){throw new PayException(resultStr);}
                 
            }
        } catch (Exception e) { 
             log.error("[荣亿付支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[荣亿付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[荣亿付支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}