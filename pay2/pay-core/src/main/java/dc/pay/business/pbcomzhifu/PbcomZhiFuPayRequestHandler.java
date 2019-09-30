package dc.pay.business.pbcomzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
 * @author Cobby
 * Mar 6, 2019
 */
@RequestPayHandler("PBCOMZHIFU")
public final class PbcomZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PbcomZhiFuPayRequestHandler.class);

//    {
//        "data":base64({
//            "mid":"10000",//商户号 必填
//            "type":"0",//订单类型 必填 (0非固定金额订单 1固定金额订单)
//            "oid":"3",//商户订单号 必填
//            "amt":"2",//订单金额 必填
//            "way":"3",//交易方式 必填 (1微信支付,2支付宝支付,3微信WAP,4支付宝WAP)
//            "back":"4",//支付返回商户地址 必填
//            "notify":"5",//支付成功通知商户地址 必填
//            "remark":"6"//备注 可填
//    }),
//        "sign":md5(data+key)
//    }
    private static final String mid                ="mid";//商户号
    private static final String type               ="type";//订单类型(0非固定金额订单 1固定金额订单)
    private static final String oid                ="oid";//商户订单号
    private static final String amt                ="amt";//订单金额
    private static final String way                ="way";//交易方式 必填 (1微信支付,2支付宝支付,3微信WAP,4支付宝WAP)
    private static final String back               ="back";//支付返回商户地址
    private static final String notify             ="notify";//支付成功通知商户地址 必填
    private static final String remark             ="remark";//备注 可填

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mid, channelWrapper.getAPI_MEMBERID());
                put(type,"0");
                put(oid,channelWrapper.getAPI_ORDER_ID());
                put(amt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(back,channelWrapper.getAPI_WEB_URL());
                put(notify,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(remark,"remark");
            }
        };
        log.debug("[pbcom支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> payParam) throws PayException {
         String data = new String(Base64.encodeBase64(JSON.toJSONString(payParam).getBytes()));
        String signMd5 = HandlerUtil.getMD5UpperCase(data+channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[pbcom支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> api_response_params, String pay_md5sign) throws PayException {
        Map<String,String> payParam = new LinkedHashMap<>();
        String data = new String(Base64.encodeBase64(JSON.toJSONString(api_response_params).getBytes()));

        payParam.put("data",data);
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[pbcom支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("error_code") && "0".equalsIgnoreCase(jsonObject.getString("error_code"))
                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    String data_url = jsonObject.getString("data");
                    JSONObject code_url = JSONObject.parseObject(data_url);
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT , code_url.getString("pay_url").replace("\\", ""));
                }else {
                    log.error("[pbcom支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[pbcom支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[pbcom支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[pbcom支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}