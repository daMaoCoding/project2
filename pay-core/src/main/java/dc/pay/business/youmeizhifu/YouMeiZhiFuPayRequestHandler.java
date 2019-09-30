package dc.pay.business.youmeizhifu;

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

import java.util.*;

/**
 * @author Cobby
 * July 09, 2019
 */
@RequestPayHandler("YOUMEIZHIFU")
public final class YouMeiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YouMeiZhiFuPayRequestHandler.class);

    private static final String pay_type   = "pay_type";   //接口类型        String(20)    0：支付宝转卡，1：支付宝二维码，2：微信扫码
    private static final String merchno    = "merchno";    //商户号        String(8)    分配给用户的商户号
    private static final String order_id   = "order_id";   //商户订单号        String(32)    商户生成的订单号，不可重复
    private static final String goodname   = "goodname";   //商品描述        String(40)    商品描述
    private static final String extra      = "extra";      //附加信息        String(128)    附加信息
    private static final String total_fee  = "total_fee";  //金额        Int    金额，单位为分，如1元传100
    private static final String notify_url = "notify_url"; //通知地址        String(255)    接收支付结果通知地址URL，确保能通过外网连接(不能带？号)如不填系统自动调用后台配置的
    private static final String return_url = "return_url"; //前台地址        String(255)    交易完成后的跳转地址(不能带？号)
    private static final String nonce_str  = "nonce_str";  //随机字符串        String(32)    随机字符串长度不超过32，可传时间戳

    private static final String key       = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(merchno, channelWrapper.getAPI_MEMBERID());
                put(order_id, channelWrapper.getAPI_ORDER_ID());
                put(goodname, "商品");
                put(extra, "extra");
                put(total_fee, channelWrapper.getAPI_AMOUNT());
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url, channelWrapper.getAPI_WEB_URL());
                put(nonce_str, HandlerUtil.randomStr(6));
            }
        };
        log.debug("[优美支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[优美支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[优美支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("code") && "200".equalsIgnoreCase(resJson.getString("code"))
                    && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
                resJson = JSONObject.parseObject(resJson.getString("data"));
                String code_url = resJson.getString("pay_info");
                result.put(JUMPURL, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            } else {
                log.error("[优美支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[优美支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[优美支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[优美支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}