package dc.pay.business.wangfa;

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

@RequestPayHandler("WANGFA")
public final class WangFaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WangFaPayRequestHandler.class);


    private static final String  trx_key = "trx_key";   //	商户支付Key	String(32)	M	商户支付Key
    private static final String  ord_amount = "ord_amount";   //	订单金额	String(12)	M	订单金额,单位：元，保留小数点后两位
    private static final String  request_id = "request_id";   //	商户支付请求号，订单号	String(30)	M	商户支付请求号
    private static final String  request_ip = "request_ip";   //	下单IP	String(15)	M	下单IP
    private static final String  product_type = "product_type";   //	产品类型	String(8)	M	详见附录产品类型
    private static final String  request_time = "request_time";   //	下单时间	String(14)	M	格式yyyyMMddHHmmss
    private static final String  goods_name = "goods_name";   //	支付产品名称	String(200)	M	支付产品名称
    private static final String  return_url = "return_url";   //	页面通知地址	String(300)	M	页面通知地址
    private static final String  callback_url = "callback_url";   //	后台异步通知地址	String(300)	M	后台异步通知地址

    private static final String  sign = "sign";      //: "1377fb66a29b198383c83a9b5bb01ea2"



    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(trx_key,channelWrapper.getAPI_MEMBERID());
            payParam.put(ord_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(request_id,channelWrapper.getAPI_ORDER_ID());
            payParam.put(request_ip,channelWrapper.getAPI_Client_IP());
            payParam.put(product_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(request_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(goods_name,channelWrapper.getAPI_ORDER_ID());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[旺发支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        sb.append("secret_key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[旺发支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper) &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                
                    if(null!=jsonResultStr && jsonResultStr.containsKey("rsp_code") && "0000".equalsIgnoreCase(jsonResultStr.getString("rsp_code")) && jsonResultStr.containsKey("data")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("data"))){
                                result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT,  jsonResultStr.getString("data"));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[旺发支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[旺发支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[旺发支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}