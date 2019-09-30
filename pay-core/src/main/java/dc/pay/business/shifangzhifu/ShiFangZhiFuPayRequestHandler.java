package dc.pay.business.shifangzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
 * Mar 14, 2019
 */
@RequestPayHandler("SHIFANGZHIFU")
public final class ShiFangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShiFangZhiFuPayRequestHandler.class);


    private static final String merchantId       ="merchantId";   //    是    商户号
    private static final String orderSn          ="orderSn";      //    是    商家订单号，保证唯一性
    private static final String scanType         ="scanType";     //    是    扫码支付类型: [1:微信免签,2:支付宝免签,4:转卡支付,5:云闪付,6:支付宝红包,7:微信h5,8:当面付,9;支付宝wap]
//    private static final String scanSubType      ="scanSubType";  //    否    扫码支付子类型: scanType=6时，当scanSubType=1时是自动发红包，当scanSubType=2时是自动加好友转账
    private static final String currency         ="currency";     //    是    货币类型: [1:CNY,2:USD,3:HKD,4:BD]
    private static final String amount           ="amount";       //    是    订单总额: 分作为单位
    private static final String notifyUrl        ="notifyUrl";    //    是    异步通知回调地址（回调地址只能是域名形式，不能是IP形式）
//    private static final String extra            ="extra";        //    否    额外参数(选填)
    private static final String goodsName        ="goodsName";    //    是    商品标题
//    private static final String goodsDetail      ="goodsDetail";  //    否    商品描述(选填)
    private static final String type             ="type";         //    是    支付方式 [1:网银,2:扫码,3:快捷,4:代付]

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantId, channelWrapper.getAPI_MEMBERID());
                put(orderSn,channelWrapper.getAPI_ORDER_ID());
                put(scanType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(currency,"1");
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(goodsName,"name");
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            }
        };
        log.debug("[十方支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        // sign    是    签名
        // md5(merchantId+orderSn+currency+amount+notifyUrl+type+key)说明key 是商户秘钥
        String paramsStr = String.format("%s%s%s%s%s%s%s",
                api_response_params.get(merchantId),
                api_response_params.get(orderSn),
                api_response_params.get(currency),
                api_response_params.get(amount),
                api_response_params.get(notifyUrl),
                api_response_params.get(type),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[十方支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[十方支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("code") && "200".equalsIgnoreCase(jsonObject.getString("code"))  &&
                        jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    String code_url = jsonObject.getString("data");
                    jsonObject = JSONObject.parseObject(code_url);
                    result.put( "1".equalsIgnoreCase(jsonObject.getString("type")) ? JUMPURL : QRCONTEXT, jsonObject.getString("gopayUrl"));
                }else {
                    log.error("[十方支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[十方支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[十方支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[十方支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}