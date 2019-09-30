package dc.pay.business.chengxinzhifu;

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
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("CHENGXINZHIFU")
public final class ChengXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChengXinZhiFuPayRequestHandler.class);

     private static final String     uid = "uid";   //	支付平台提供给商户平台的唯一凭证（支付平台分配）	必填
     private static final String     orderid = "orderid";   //	订单id（商户自定义生成）	40字节内，必填
     private static final String     paymoney = "paymoney";   //	支付金额，单位：元（不需要写单位“元”），保留2位有效数字	14字节内，必填
     private static final String     paytype = "paytype";   //	支付方式(支付宝)：11	必填.
     private static final String     notifyurl = "notifyurl";   //	支付结果通知地址（用于支付平台向商户平台发起请求，告知订单支付结果）	80字节内，必填
     private static final String     returnurl = "returnurl";   //	支付页面跳转地址	80字节内，必填
     private static final String     key = "key";   //	校验秘钥，用于验证报文数据的真实性； 把参数名、参数值以及token拼接在一起，作md5-32位加密，取字符串小写。 明文内容范例（注意字段顺序；url网址类型的参数值不要encode；若字段为空，参数名依然需要写，例如范例中的orderinfo）：uid=123&orderid=123&ordername=abc&paymoney=1.01&orderuid=123&paytype=11&notifyurl=http//notifyurl&returnurl=http://returnurl&orderinfo=&token=ABC123


    private static final String     ordername = "ordername";
    private static final String     orderuid = "orderuid";
    private static final String     orderinfo = "orderinfo";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(uid,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(returnurl,channelWrapper.getAPI_WEB_URL());

            payParam.put(ordername,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderuid,HandlerUtil.getRandomStr(10));
            payParam.put(orderinfo,channelWrapper.getAPI_ORDER_ID());
        }
        log.debug("[诚信支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //=123&=123&=abc&=1.01&orderuid=123&paytype=11&notifyurl=http//notifyurl&returnurl=http://returnurl&orderinfo=&token=ABC123
        //uid=123&orderid=123&ordername=abc&paymoney=1.01&orderuid=123&paytype=11&notifyurl=http//notifyurl&returnurl=http://returnurl&orderinfo=&token=ABC123
        String paramsStr = String.format("uid=%s&orderid=%s&ordername=%s&paymoney=%s&orderuid=%s&paytype=%s&notifyurl=%s&returnurl=%s&orderinfo=%s&token=%s",
                params.get(uid),
                params.get(orderid),
                params.get(ordername),
                params.get(paymoney),
                params.get(orderuid),
                params.get(paytype),
                params.get(notifyurl),
                params.get(returnurl),
                params.get(orderinfo),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[诚信支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;

    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
/*				
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getValueAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
				
*/				
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[诚信支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[诚信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[诚信支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}