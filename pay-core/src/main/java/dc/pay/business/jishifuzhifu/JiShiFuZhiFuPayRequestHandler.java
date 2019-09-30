package dc.pay.business.jishifuzhifu;

import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author cobby
 * Jan 21, 2019
 */
@RequestPayHandler("JISHIFUZHIFU")
public final class JiShiFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiShiFuZhiFuPayRequestHandler.class);
// authno	授权号	40	是	商户扫描到的授权号
// merchno	商户号	15	是	商户签约时，本系统分配给商家的唯一标识。
// amount	交易金额	12	是	以元为单位
// ip	客户端IP	15	否	微信WAP支付必填,且必须上送终端用户手机上的公网IP
// traceno	商户流水号	32	是	商户网站唯一订单号，由商户系统生成，保证其唯一性。
// payType	支付方式	1	是	1-支付宝2-微信
// goodsName	商品名称	30	否	默认取商户名称
// cust1	自定义域1	100	否	商户网站自定义，系统原样返回给商户网站。
// cust2	自定义域2	100	否	商户网站自定义，系统原样返回给商户网站。
// cust3	自定义域3	100	否	商户网站自定义，系统原样返回给商户网站。
// notifyUrl	通知地址	50	否	交易成功，则给商户发送异步通知。
// returnUrl	返回地址	50	否	交易成功，则返回到商家的地址。
// settleType	结算方式	1	否	默认为T+1-T+1结算
// signature	数据签名	32	是	对签名数据进行MD5加密的结果。

    private static final String interType              ="interType";//接口类型 固定值2 H5快捷必传
    private static final String merchno                ="merchno";
    private static final String authno                 ="authno";
    private static final String amount                 ="amount";
    private static final String ip                     ="ip";
    private static final String traceno                ="traceno";
    private static final String paytype                ="payType";
    private static final String goodsname              ="goodsName";
    private static final String notifyurl              ="notifyUrl";
    private static final String returnUrl              ="returnUrl";
    private static final String settleType             ="settleType";// 结算类型 2 T+1结算
    private static final String cardType               ="cardType";//卡类型 1-借记卡 2-信用卡
    private static final String channel                ="channel"; //连接方式  2-直联银行
    private static final String bankCode               ="bankCode"; //当连接方式选择2的时候，该域必填。参见3.2银行代码。

    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[即时付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&授权号" );
            throw new PayException("[即时付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&授权号" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchno, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(traceno, channelWrapper.getAPI_ORDER_ID());
                put(notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl, channelWrapper.getAPI_WEB_URL());
                if (HandlerUtil.isYLKJ(channelWrapper)) {
                    put(interType, "2");
                    put(settleType, "2");
                    put(cardType, "1");
                } else if (HandlerUtil.isWY(channelWrapper)) {
                    put(channel, "2");
                    put(settleType, "2");
                } else if(HandlerUtil.isWEBWAPAPP(channelWrapper)){
//                    put(paytype, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                    put(paytype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(settleType,"1" );
                }else {
                    put(paytype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    if (channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("2")){
                        put(ip, channelWrapper.getAPI_Client_IP());
                    }
                }
                }
             };
        log.debug("[即时付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[即时付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
            if (HandlerUtil.isWY(channelWrapper)){
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else {
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                resultStr = new String(resultStr.getBytes("ISO-8859-1"), "GBK");
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[即时付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                    //log.error("[即时付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
                }
                if (!resultStr.contains("{") || !resultStr.contains("}")) {
                    log.error("[即时付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                //JSONObject resJson = JSONObject.parseObject(resultStr);
                JSONObject resJson;
                try {
                    resJson = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[即时付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                //只取正确的值，其他情况抛出异常
                if (HandlerUtil.isYLKJ(channelWrapper) && null != resJson  && resJson.containsKey("url") && StringUtils.isNotBlank(resJson.getString("url"))) {
                    String code_url = resJson.getString("url");
                    result.put( JUMPURL , code_url);
                }else if (null != resJson
                        && resJson.containsKey("respCode") && StringUtils.isNotBlank(resJson.getString("respCode"))&& "00".equalsIgnoreCase(resJson.getString("respCode"))
                        && resJson.containsKey("barCode") && StringUtils.isNotBlank(resJson.getString("barCode"))){
	                String barCode = resJson.getString("barCode");
	                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, barCode);
                } else {
                    log.error("[即时付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }


            }
        } catch (Exception e) {
            log.error("[即时付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[即时付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[即时付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}