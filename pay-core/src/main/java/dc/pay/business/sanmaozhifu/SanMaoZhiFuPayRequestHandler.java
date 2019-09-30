package dc.pay.business.sanmaozhifu;

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
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("SANMAOZHIFU")
public final class SanMaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SanMaoZhiFuPayRequestHandler.class);

    private static final String  mchId="mchId";  // 	商户编号	8	字符串	平台分配
    private static final String  appId="appId";  // 	应用编号	32	字符串	平台分配
    private static final String  mchOrderNo="mchOrderNo";  // 	商户订单号	32	字符串	商户生成的订单号，不能重复
    private static final String  amount="amount";  // 	订单金额	15	字符串	分为单位
    private static final String  currency="currency";  // 	币种单位	5	字符串	CNY，人民币
    private static final String  productId="productId";  // 	支付平台编号	4	字符串	8006:支付宝扫码支付
    private static final String  subject="subject";  // 	商品标题	64	字符串
    private static final String  body="body";  // 	订单描述	128	字符串
    private static final String  notifyUrl="notifyUrl";  // 	后台通知url	128	字符串
    private static final String  returnUrl="returnUrl";  // 	页面跳转url	128	字符串	支付成功后，从收银台跳到商户的页面
    private static final String  sign="sign";  // 	签名值	64	字符串	不参与签名计算
    private static final String  CNY="CNY";
    private static final String  params="params";





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接[商户ID]和[应用ID],如：商户ID&应用ID");
        }

        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mchId,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(appId,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(mchOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(currency,CNY);
            payParam.put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(body,DateUtil.curDateTimeStr());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
        }
        log.debug("[三猫支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[三猫支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        HashMap<String, String> reqParams = Maps.newHashMap();
        reqParams.put(params,JSON.toJSONString(payParam));

        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper)&&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),reqParams).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqParams, String.class, HttpMethod.POST).trim();

				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(jsonResultStr.getString("retCode")) && jsonResultStr.containsKey("payParams") && null!=jsonResultStr.getJSONObject("payParams")
                            && jsonResultStr.getJSONObject("payParams").containsKey("payUrl") && null!= jsonResultStr.getJSONObject("payParams").getJSONObject("payUrl")
                            && jsonResultStr.getJSONObject("payParams").getJSONObject("payUrl").containsKey("alipay_trade_precreate_response")){
                        JSONObject payParams = jsonResultStr.getJSONObject("payParams").getJSONObject("payUrl").getJSONObject("alipay_trade_precreate_response");

                        if(payParams.containsKey("code") && payParams.getString("code").equalsIgnoreCase("10000")  && payParams.containsKey("qr_code") && StringUtils.isNotBlank(payParams.getString("qr_code"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL,payParams.getString("qr_code"));
                                }else {
                                    result.put(QRCONTEXT,payParams.getString("qr_code"));
                                }
                                payResultList.add(result);
                            }else { throw new PayException(resultStr); }
                    }else { throw new PayException(resultStr); }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[三猫支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[三猫支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[三猫支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}