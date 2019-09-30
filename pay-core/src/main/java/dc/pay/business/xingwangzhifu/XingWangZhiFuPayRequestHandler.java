package dc.pay.business.xingwangzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 * @author Cobby
 * Apr 17, 2019
 */
@RequestPayHandler("XINGWANGZHIFU")
public final class XingWangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XingWangZhiFuPayRequestHandler.class);

    private static final String tradeno              ="tradeno";      //商户订单编号
    private static final String tradename            ="tradename";    //商品名称
    private static final String amount               ="amount";       //金额，单位为分
    private static final String partner              ="partner";      //商户 ID
    private static final String paytype              ="paytype";      //通道
    private static final String inttime              ="inttime";      //时间戳：秒
    private static final String paynotifyurl         ="paynotifyurl"; //异步回调地址
    private static final String key        ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(tradeno,channelWrapper.getAPI_ORDER_ID());
                put(tradename,"name");
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(inttime,System.currentTimeMillis()/1000+"");
                put(paynotifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[星网支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //  小写 MD5   signinfo=md5(tradeno=tradeno&tradename=tradename&amount=amount&partner=partner&
        //             paytype=paytype&inttime=inttime&paynotifyurl=paynotifyurl&key=key)
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(tradeno+"=").append(api_response_params.get(tradeno)).append("&");
        signSrc.append(tradename+"=").append(api_response_params.get(tradename)).append("&");
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
        signSrc.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
        signSrc.append(inttime+"=").append(api_response_params.get(inttime)).append("&");
        signSrc.append(paynotifyurl+"=").append(api_response_params.get(paynotifyurl)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[星网支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[星网支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.parseObject(resultStr).toJSONString() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.parseObject(resultStr).toJSONString());
            }
            if (null != jsonObject && jsonObject.containsKey("retcode") && "1".equalsIgnoreCase(jsonObject.getString("retcode"))
                    && jsonObject.containsKey("payurl") && StringUtils.isNotBlank(jsonObject.getString("payurl"))) {
                String code_url = jsonObject.getString("payurl");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
            }else {
                log.error("[星网支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.parseObject(resultStr).toJSONString());
            }

        } catch (Exception e) {
            log.error("[星网支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[星网支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[星网支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}