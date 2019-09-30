package dc.pay.business.sptfu;

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

@RequestPayHandler("SPTFU")
public final class SPTFuPayRequestHandler extends PayRequestHandler {
     private static final Logger log = LoggerFactory.getLogger(SPTFuPayRequestHandler.class);

     private static final String   merch = "merch";    //  String(32)  是  商户号
     private static final String   orderno = "orderno";    //  String(32)  是  商户订单号，支付通知时会将该信息返回
     private static final String   amount = "amount";    //  int  是  交易金额，以分为单位，不允许包含任何字 符号
     private static final String   tranchannel = "tranchannel";    //  String（32）  是  银行渠道编码参见附录。例如建设银行：CCB
     private static final String   body = "body";    //  String(128)  是  商品描述
     private static final String   pageurl = "pageurl";    //  String(256)  是  支付成功页面跳转链接
     private static final String   notifyurl = "notifyurl";    //  String(256)  是  异步回调通知地址
     private static final String   sign = "sign";    //  String(64)  是  签名串
     private static final String   signtype = "signtype";    //  String(64)  是  签名方式，目前指定MD5
     private static final String   MD5 = "MD5";
     private static final String   comment = "comment";
     private static final String   userid  = "userid";
     private static final String   backurl  = "backurl";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
            payParam.put(merch,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()  );
            payParam.put(signtype,  MD5);

            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("SPTFU_BANK_WEBWAPAPP_WX_SM")){
                payParam.put(comment,  channelWrapper.getAPI_ORDER_ID());
            }

            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("SPTFU_BANK_WEBWAPAPP_YL_KJZF")){
                payParam.put(userid,  HandlerUtil.getRandomNumber(10));
                payParam.put(backurl,  channelWrapper.getAPI_WEB_URL());
            }

            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().startsWith("SPTFU_BANK_WEB_WY_")){
                payParam.put(tranchannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(pageurl,channelWrapper.getAPI_WEB_URL());
            }
        log.debug("[spt付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()) ||signtype.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[spt付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
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
                    if(null!=jsonResultStr && jsonResultStr.containsKey("return_code") && SUCCESS.equalsIgnoreCase(jsonResultStr.getString("return_code")) && jsonResultStr.containsKey("code_url")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("code_url"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("code_url")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[spt付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[spt付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[spt付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}