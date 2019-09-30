package dc.pay.business.xunjiezhifu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XUNJIEZHIFU")
public final class XunJieZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XunJieZhiFuPayRequestHandler.class);

    private static final String      merchant_no = "merchant_no";    //	是	String	10	商户号
    private static final String      merchant_order_no = "merchant_order_no";    //	是	String	35	商户订单号
    private static final String      pay_type = "pay_type";    //	是	String	1	支付类别
    private static final String      notify_url = "notify_url";    //	是	String	255	商户通知地址
    private static final String      return_url = "return_url";    //	是	String	255	支付完跳转地址
    private static final String      trade_amount = "trade_amount";    //	是	String	11	交易金额 单位为元，精确到小数点后两位，大于等于1元
    private static final String      sign = "sign";    //	是	String	32	签名（详见【签名方法】）



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchant_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(merchant_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(trade_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        }
        log.debug("[迅捷支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[迅捷支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==1 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper) && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "1".equalsIgnoreCase(jsonResultStr.getString("status")) && jsonResultStr.containsKey("data")){
                        JSONObject data = jsonResultStr.getJSONObject("data");
                        if(null!=data && data.containsKey("pay_url") &&  StringUtils.isNotBlank(data.getString("pay_url")) ){
                            if(HandlerUtil.isWapOrApp(channelWrapper) ||HandlerUtil.isYLKJ(channelWrapper)){
                                result.put(JUMPURL,  data.getString("pay_url"));
                                payResultList.add(result);
                            }else{
                                String qrContext="";
                                String pay_url = data.getString("pay_url");
                                if(StringUtils.isNotBlank(pay_url) && pay_url.contains("uuid")){
                                    qrContext=  HandlerUtil.UrlDecode(pay_url).substring(pay_url.indexOf("uuid=")+5);
                                }else if(StringUtils.isNotBlank(pay_url)) {
                                    qrContext =  HandlerUtil.UrlDecode(pay_url);
                                }
                                if(StringUtils.isNotBlank(qrContext)){
                                    result.put(QRCONTEXT, qrContext);
                                    payResultList.add(result);
                                }else{
                                    throw new PayException(resultStr);
                                }
                            }
                        }
                    }else {
                        throw new PayException(resultStr);
                    }
            }
        } catch (Exception e) { 
             log.error("[迅捷支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[迅捷支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[迅捷支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}