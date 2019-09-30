package dc.pay.business.xingyunri;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XINGYUNRIZHIFU")
public final class XingYunRiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XingYunRiZhiFuPayRequestHandler.class);

     private static final String    shop_id = "shop_id";    //	字符串	商家ID
     private static final String    user_id = "user_id";    //	字符串	商家用户ID
     private static final String    money = "money";    //	字符串	订单金额，单位元，如：0.01表示一分钱；
     private static final String    type = "type";    //	字符串	微信：wechat，支付宝：alipay
     private static final String    sign = "sign";    //	字符串	验签字符串，
     private static final String    shop_no = "shop_no";    //	字符串	商家订单号，长度不超过40；
     private static final String    notify_url = "notify_url";    //	字符串	订单支付成功回调地址（具体参数详见接口2，如果为空，平台会调用商家在WEB端设置的订单回调地址；否则，平台会调用该地址，WEB端设置的地址不会被调用）；
     private static final String    return_url = "return_url";    //	字符串	二维码扫码支付模式下：支付成功页面‘返回商家端’按钮点击后的跳转地址；如果商家采用自有界面，则可忽略该参数；


      private static final String    order_no = "order_no";    // "J2GLL2N1537263842268",
      private static final String    pay_url = "pay_url" ;    //"HTTPS://QR.ALIPAY.COM/FKX086858VBGHIX72SO709?t=1537263843764",
      private static final String    qrcode_url = "qrcode_url" ;    //"https://www.2311s.com/apiShop?orderId=c0657ec7a0129ac69a12eba70681caf7"
      //private static final String    user_id = "user_id";    // "fjOVsPmzsL",
      //private static final String    money = "money";    // "300.00",
      //private static final String    type = "type";    // "alipay",
      //private static final String    sign = "sign";    // "59ba79b25121576bed1f7405ebb9cb3a",
      //private static final String    shop_no = "shop_no";    // "20180918174359",


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(shop_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(user_id,HandlerUtil.getRandomStr(10));
            payParam.put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(shop_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
        }
        log.debug("[幸运日支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //shop_id + user_id + money + type +sign_key
        String paramsStr = String.format("%s%s%s%s%s",
                params.get(shop_id),
                params.get(user_id),
                params.get(money),
                params.get(type),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[幸运日支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper)  && HandlerUtil.isYLKJ(channelWrapper)   &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey(order_no)  && jsonResultStr.containsKey(shop_no) && jsonResultStr.containsKey(pay_url)){
                            if(StringUtils.isNotBlank(jsonResultStr.getString(pay_url))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString(pay_url));
                                }else {
                                    result.put(QRCONTEXT, jsonResultStr.getString(pay_url));
                                }
                                payResultList.add(result);
                            }else { throw new PayException(resultStr); }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[幸运日支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[幸运日支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[幸运日支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}