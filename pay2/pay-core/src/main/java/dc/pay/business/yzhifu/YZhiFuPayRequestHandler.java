package dc.pay.business.yzhifu;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("YZHIFU")
public final class YZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

     private static final String     price = "price";    // 是 string 支付的金额
     private static final String     out_order_id = "out_order_id";    // 是 string 你订单系统的唯一订单号
     private static final String     type = "type";    // 是 string 请求支付的方式，wechat:微信  alipay:支付宝
     private static final String     notifyurl = "notifyurl";    // 否 string    支付成功的回调通知URL，为空将以插件管理中配置的             URL为准
     private static final String     sign = "sign";    // 是 string 签名,构造格式见下方的签名方法
     private static final String     product_id = "product_id";
     private static final String     returnurl = "returnurl";
     private static final String     extend = "extend";
     private static final String     aid = "aid";
     private static final String     format = "format";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if (null == channelWrapper.getAPI_MEMBERID() || !channelWrapper.getAPI_MEMBERID().contains("&")) {
            throw new PayException("商户号格式错误。正确格式如：[商户号&aid],例如：2342734&22,其中aid请咨询第三方");
        }
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) );
            payParam.put(out_order_id, channelWrapper.getAPI_ORDER_ID());
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(product_id, channelWrapper.getAPI_ORDER_ID());
            payParam.put(returnurl, channelWrapper.getAPI_WEB_URL());
            payParam.put(extend, channelWrapper.getAPI_ORDER_ID());
            payParam.put(aid, channelWrapper.getAPI_MEMBERID().split("&")[1]);
            if(HandlerUtil.isWapOrApp(channelWrapper)){
               // payParam.put(format, "html");
            }

        }
        log.debug("[Y支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //md5(md5(aid+price+out_order_id+type+product_id+notifyurl+returnurl+extend)+secretkey)
        String paramsStr = String.format("%s%s%s%s%s%s%s%s",
                params.get(aid),
                params.get(price),
                params.get(out_order_id),
                params.get(type),
                params.get(product_id),
                params.get(notifyurl),
                params.get(returnurl),
                params.get(extend));

        String md5_1 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        String signMd5 =HandlerUtil.getMD5UpperCase( md5_1.concat(channelWrapper.getAPI_KEY())).toLowerCase();
        log.debug("[Y支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 &&  HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    resultStr = UnicodeUtil.unicodeToString(resultStr);
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "1".equalsIgnoreCase(jsonResultStr.getString("code"))
                            && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data")){
                        if( jsonResultStr.getJSONObject("data").containsKey("qrcodeurl") && StringUtils.isNotBlank(jsonResultStr.getJSONObject("data").getString("qrcodeurl"))){
                        	result.put(JUMPURL, jsonResultStr.getJSONObject("data").getString("qrcodeurl"));
                            payResultList.add(result);
                        }else {throw new PayException(resultStr); }

                    }else {
                        resultStr = UnicodeUtil.unicodeToString(resultStr);
                        throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[Y支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[Y支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[Y支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}