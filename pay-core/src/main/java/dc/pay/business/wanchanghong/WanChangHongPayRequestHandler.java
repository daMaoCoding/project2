package dc.pay.business.wanchanghong;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 13, 2018
 */
@RequestPayHandler("WANCHANGHONG")
public class WanChangHongPayRequestHandler extends PayRequestHandler{
    private static final Logger log = LoggerFactory.getLogger(WanChangHongPayRequestHandler.class);
    
    private static final String merchantId            ="merchantId";
    private static final String orderId               ="orderId";   
    private static final String payType               ="payType";
    private static final String amount                ="amount";
    private static final String userIp                ="userIp";
    private static final String notifyUrl             ="notifyUrl";

    @Override
    protected Map<String, String> buildPayParam() throws PayException, UnsupportedEncodingException {
        Map<String, String> payParam = new TreeMap<String, String>(){
            {
                put(merchantId, channelWrapper.getAPI_MEMBERID());
                put(orderId,channelWrapper.getAPI_ORDER_ID());
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(userIp,channelWrapper.getAPI_Client_IP());
            }
        };
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toUpperCase();
        log.debug("[万昌红]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
             String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
             if (StringUtils.isBlank(resultStr)) {
                 log.error("[万昌红]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 throw new PayException(resultStr);
                 //log.error("[万昌红]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                 //throw new PayException("返回空,参数："+JSON.toJSONString(map));
             }
             if (!resultStr.contains("{") || !resultStr.contains("}")) {
                 log.error("[万昌红]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 throw new PayException(resultStr);
             }
             JSONObject jsonResult = null;
             try {
//                 jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
                 jsonResult = JSONObject.parseObject(resultStr);
             } catch (Exception e) {
                 log.error("[万昌红]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                 throw new PayException(e.getMessage(),e);
             }
             if (null != jsonResult && jsonResult.containsKey("code") && "0".equalsIgnoreCase(jsonResult.getString("code")) && jsonResult.containsKey("codeUrl") && StringUtils.isNotBlank(jsonResult.getString("codeUrl"))) {
                 String codeUrl = jsonResult.getString("codeUrl");
                 result.put(QRCONTEXT, codeUrl);
             }else {
                 log.error("[万昌红]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 throw new PayException(resultStr);
             }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[万昌红]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
         RequestPayResult requestPayResult = new RequestPayResult();
            if (null != resultListMap && !resultListMap.isEmpty()) {
                if (resultListMap.size() == 1) {
                    Map<String, String> resultMap = resultListMap.get(0);
                    requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
                }
                if (ValidateUtil.requestesultValdata(requestPayResult)) {
                    requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
                } else {
                    throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
                }
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
            }
            log.debug("[万昌红]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
            return requestPayResult;
    }

}
