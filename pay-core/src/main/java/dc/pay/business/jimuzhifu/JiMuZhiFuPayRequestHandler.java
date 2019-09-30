package dc.pay.business.jimuzhifu;

import java.util.*;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 积木支付
 */
@RequestPayHandler("JIMUZHIFU")
public final class JiMuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiMuZhiFuPayRequestHandler.class);

    private static final String mer_no             ="mer_no";            //    网商号       商户编号
    private static final String pay_type           ="pay_type";          //    支付方式     （支付宝--1）目前只支持支付宝
    private static final String order_no           ="order_no";          //    订单号       商户订单编号
    private static final String amt                ="amt";               //    订单金额     下单金额,单位分 上限5W 下限1元
    private static final String cur                ="cur";               //    货币代码     目前只支持人民币:CNY
    private static final String notify_url         ="notify_url";        //    异步通知地址  用户订单支付结果异步通知
    private static final String sync_call_back_url ="sync_call_back_url";//    同步回调地址  支付后页面跳转地址
    private static final String sign_type          ="sign_type";         //    签名类型     目前只支持MD5
//    private static final String sign               ="sign";            //    签名         数字签名

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mer_no, channelWrapper.getAPI_MEMBERID());
                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(amt,  channelWrapper.getAPI_AMOUNT());
                put(cur,"CNY");
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(sync_call_back_url,channelWrapper.getAPI_WEB_URL());
                put(sign_type,"MD5");
            }
        };
        log.debug("[积木]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         StringBuilder signSrc = new StringBuilder();
         Iterator<String> iter = api_response_params.keySet().iterator();
         while (iter.hasNext()) {
             String key = iter.next();
             if (!key.equals("sign")) {
                 if (api_response_params.get(key) != null) {
                     if (signSrc.toString().equals("")) {
                         signSrc.append(key + "=" + api_response_params.get(key));
                     } else {
                         signSrc.append("&" + key + "=" + api_response_params.get(key));
                     }
                 }
             }
         }

         String paramsStr = signSrc.toString();
         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr+channelWrapper.getAPI_KEY()).toLowerCase();
         log.debug("[积木]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
         return signMd5;

    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String, Map<String, String>> map = new HashMap<>();
        map.put("cont",payParam);
        HashMap<String, String> result = Maps.newHashMap();
        try {
            if (1==2 && HandlerUtil.isWEBWAPAPP(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map).toString());
            }else{
                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(),map);
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[积木]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                JSONObject resJson;
                try {
                    resJson = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[积木]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                //只取正确的值，其他情况抛出异常
                if (null != resJson && resJson.containsKey("rst") && "true".equalsIgnoreCase(resJson.getString("rst"))  && resJson.containsKey("cont") && StringUtils.isNotBlank(resJson.getString("cont"))) {
                    String cont = resJson.getString("cont");
                    resJson = JSONObject.parseObject(cont);
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("pay_url"));

                }else {
                    log.error("[积木]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
        } catch (Exception e) {
            log.error("[积木]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[积木]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

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
        log.debug("[积木]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}
