package dc.pay.business.kuailianqianbao;

import java.util.*;
import java.util.regex.Pattern;

import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
 *
 */
@RequestPayHandler("KUAILIANQIANBAO")
public final class KuaiLianQianBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiLianQianBaoPayRequestHandler.class);

    private static final String merchantNo           ="merchantNo";  //必填 商户编号
    private static final String orderNo              ="orderNo";     //必填 商户订单号
    private static final String orderAmount          ="orderAmount"; //必填 商户订单金额，单位分
    private static final String payType              ="payType";     //必填 支付方式见附录
    private static final String bankName             ="bankName";    //非必填	 payType=11时必填,银行简码。如中国建设银行：CCB
    private static final String notifyUrl            ="notifyUrl";   //必填 异步通知：服务器端处理通知接口格式：如：http://biz.domain.com/notify
    private static final String callbackUrl          ="callbackUrl"; //必填 页面回调：支付成功后会向该地址发送通知，该地址可以带参数,注意：如不填callbackUrl的参数值支付成功后您的浏览器页面将得不到支付成功的通知
    private static final String accountNo            ="accountNo";   //accountNo String(50) 非必填 payType=6快捷支付必填，银行卡号
    private static final String sign                 ="sign";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantNo, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(orderAmount, channelWrapper.getAPI_AMOUNT());
                Pattern pattern = Pattern.compile("[a-z A-Z]*");
                boolean matches = pattern.matcher(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()).matches();
                put(payType, matches ? "11":channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                if (matches){
                    put(bankName,  channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callbackUrl,channelWrapper.getAPI_WEB_URL());
                if (payType.equalsIgnoreCase("6")){
                    put(accountNo,channelWrapper.getAPI_WEB_URL());
                }
            }
        };
        log.debug("[快联支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[快联支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[快联支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (!resultStr.contains("{") || !resultStr.contains("}")) {
                   log.error("[快联支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                   throw new PayException(resultStr);
                }
                JSONObject resJson;
                try {
                    resJson = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[快联支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                //只取正确的值，其他情况抛出异常
                if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("payUrl") && StringUtils.isNotBlank(resJson.getString("payUrl"))) {
                    String code_url = resJson.getString("payUrl");
                    result.put( HandlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL , code_url);
                }else {
                    log.error("[快联支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
        } catch (Exception e) {
            log.error("[快联支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[快联支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[快联支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}