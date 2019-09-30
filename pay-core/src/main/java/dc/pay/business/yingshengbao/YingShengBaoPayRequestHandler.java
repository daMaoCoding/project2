package dc.pay.business.yingshengbao;

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
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("YINGSHENGBAO")
public final class YingShengBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YingShengBaoPayRequestHandler.class);


     private static final String  ORDER_ID = "ORDER_ID";   // 	订单号
     private static final String  ORDER_AMT = "ORDER_AMT";   // 	订单金额
     private static final String  USER_ID = "USER_ID";   // 	商户id
     private static final String  BUS_CODE = "BUS_CODE";   // 	支付类型
     private static final String  PAGE_URL = "PAGE_URL";   // 	页面回跳地址
     private static final String  BG_URL = "BG_URL";   // 	支付结果后台通知地址
     private static final String  SIGN = "SIGN";   // 	签名值

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
       if( channelWrapper.getAPI_ORDER_ID().length()!=20)
           channelWrapper.setAPI_ORDER_ID(channelWrapper.getAPI_ORDER_ID());

        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(ORDER_ID,procOrderTo20Length(channelWrapper.getAPI_ORDER_ID()));
            payParam.put(ORDER_AMT,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(USER_ID,channelWrapper.getAPI_MEMBERID());
            payParam.put(BUS_CODE,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(PAGE_URL,channelWrapper.getAPI_WEB_URL());
            payParam.put(BG_URL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }

        log.debug("[赢生宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // opstate=opstate值&orderid=orderid值&ovalue=ovalue值&parter=商家接口调用ID&key=商家
        String paramsStr = String.format("%s%s%s%s",
                params.get(ORDER_ID),
                params.get(ORDER_AMT),
                params.get(USER_ID),
                params.get(BUS_CODE));
        String signMd5 = HandlerUtil.getMD5UpperCase( HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().concat(channelWrapper.getAPI_KEY())).toLowerCase().substring(8, 24);
        log.debug("[赢生宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper)&&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("success") && "true".equalsIgnoreCase(jsonResultStr.getString("success"))
                            && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code"))
                            && jsonResultStr.containsKey("result") &&  null!= jsonResultStr.getJSONObject("result") && jsonResultStr.getJSONObject("result").containsKey("QRCODE")
                            && StringUtils.isNotBlank(  jsonResultStr.getJSONObject("result").getString("QRCODE")  )  ){
                        if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("WEBWAPAPP_ZFB_SM")){
                            result.put(QRCONTEXT, jsonResultStr.getJSONObject("result").getString("QRCODE") );
                            payResultList.add(result);
                        }else{
                            String qrContext = QRCodeUtil.decodeByUrl( jsonResultStr.getJSONObject("result").getString("QRCODE") );
                            if(StringUtils.isBlank(qrContext)) throw new PayException(resultStr);
                            result.put(QRCONTEXT, qrContext);
                            payResultList.add(result);
                        }
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[赢生宝]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[赢生宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[赢生宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

    protected  String procOrderTo20Length(String orderNm){
        if(orderNm.length()<20)  orderNm = orderNm.concat("R").concat(HandlerUtil.getRandomNumber(19-orderNm.length()));
        return orderNm;
    }
}