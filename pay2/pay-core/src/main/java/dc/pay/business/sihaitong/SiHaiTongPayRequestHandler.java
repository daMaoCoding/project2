package dc.pay.business.sihaitong;

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
import java.util.List;
import java.util.Map;

@RequestPayHandler("SIHAITONGZHIFU")
public final class SiHaiTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SiHaiTongPayRequestHandler.class);

    private static final String      sign	   = "sign" ;                //是	string	签名

     private static final String     	userId = "userId"; //	    商户ID    N	Y	商户ID
     private static final String     	channel = "channel"; //	    产品类型    N	Y	详见支付产品列表说明
     private static final String     	money = "money"; //	    金额    N	Y	单位元（人民币）
     private static final String     	ordernumber = "ordernumber"; //	    商户订单号    N	Y	商户系统订单号
     private static final String     	return_url = "return_url"; //	    下行同步通知地址    N	Y	同步通知地址
     private static final String     	notify_url = "notify_url"; //	    下行异步通知地址    N	Y	异步通知地址，需要以http://开头且没有任何参数
     private static final String     	request_time = "request_time"; //	    下单时间    N	Y	格式：yyyyMMddHHmmss
     private static final String     	request_ip = "request_ip"; //	    下单IP    N	Y	下单IP
     private static final String     	goods_name = "goods_name"; //	    商品名称    N	Y	商品名称





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(userId,channelWrapper.getAPI_MEMBERID());
            payParam.put(channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(ordernumber,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(request_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(request_ip,channelWrapper.getAPI_Client_IP());
            payParam.put(goods_name,channelWrapper.getAPI_ORDER_ID());
        }

        log.debug("[四海通支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        String signStr = sb.toString().replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[四海通支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();

                if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){ //有时候返回自动提交的form <script>document.forms['demosubmit'].submit();</script>
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("rescode") && "0000".equalsIgnoreCase(jsonResultStr.getString("rescode")) && jsonResultStr.containsKey("qrcode")){
                        if(StringUtils.isNotBlank(jsonResultStr.getString("qrcode"))){
                            if(HandlerUtil.isWapOrApp(channelWrapper)){
                                result.put(JUMPURL, jsonResultStr.getString("qrcode"));
                            }else{
                                result.put(QRCONTEXT, jsonResultStr.getString("qrcode"));
                            }
                            payResultList.add(result);
                        }
                    }else {
                        throw new PayException(resultStr);
                    }
                }




                 
            }
        } catch (Exception e) {
             log.error("[四海通支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[四海通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[四海通支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}