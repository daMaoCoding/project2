package dc.pay.business.yuerongzhuang;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("YUERONGZHUANG")
public final class YueRongZhuangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YueRongZhuangZhiFuPayRequestHandler.class);

    private static final String   userName = "userName";      //	String	商户号(由平台提供)
    private static final String   data = "data";      //	String	rsa加密后的数据

    private static final String   userOrderId = "userOrderId";      //	String	商户平台上的订单号，将原样返回给商户
    private static final String   productName = "productName";      //	String	商品名称
    private static final String   money = "money";      //	Long	订单金额，单位为分
    private static final String   type = "type";      //	Ineger	支付类型: 1-支付宝,2-微信（默认为支付宝）
    private static final String   time = "time";      //	Long	订单创建时间（相对1970-01-01 00:00:00的毫秒数）
    private static final String   returnUrl = "returnUrl";      //	String	支付成功后的跳转地址(只有使用接入方式二时有效)


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(userOrderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(productName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(money,channelWrapper.getAPI_AMOUNT());
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(time,System.currentTimeMillis()+"");
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
        }
        log.debug("[悦榕庄支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = "hello";
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {

        HashMap<String, String> payParams = Maps.newHashMap();
        payParams.put(userName,channelWrapper.getAPI_MEMBERID());

        try {
            String dataStr = EncryptUtil.encryptRSAByPublicKey(channelWrapper.getAPI_KEY(), JSON.toJSONString(payParam));
            payParams.put(data,dataStr);
        } catch (Exception e) {
             throw  new PayException("加密数据出错,请检查密钥");
        }


        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 &&HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParams).toString().replace("method='post'","method='post'"));
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParams, String.class, HttpMethod.POST);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "success".equalsIgnoreCase(jsonResultStr.getString("code"))
                            && jsonResultStr.containsKey("order") && null!=jsonResultStr.getJSONObject("order")
                            && jsonResultStr.getJSONObject("order").containsKey("payUrl")  && StringUtils.isNotBlank( jsonResultStr.getJSONObject("order").getString("payUrl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getJSONObject("order").getString("payUrl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getJSONObject("order").getString("payUrl"));
                        }
                        payResultList.add(result);
                    }else if(StringUtils.isBlank(resultStr) ){  throw new PayException(EMPTYRESPONSE);  }else {throw new PayException(resultStr); }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[悦榕庄支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[悦榕庄支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[悦榕庄支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}