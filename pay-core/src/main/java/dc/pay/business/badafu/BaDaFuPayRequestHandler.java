package dc.pay.business.badafu;

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

@RequestPayHandler("BADAFU")
public final class BaDaFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaDaFuPayRequestHandler.class);


    private static final String   	version = "version";      //	   版本号   N	Y	接口版本号,目前固定值为3.0
    private static final String   	method = "method";      //	   接口名称   N	Y	接口名称: pay
    private static final String   	partner = "partner";      //	   商户ID   N	Y	商户id,由PayOctopus API分配
    private static final String   	requesttype = "requesttype";      //	   请求类型   N	N	test:模拟操作流程；pro：实际流程,默认test;
    private static final String   	banktype = "banktype";      //	   银行类型   N	Y	银行类型，具体参考附录1,default为ALIPAY，现在只支付ALIPAY！
    private static final String   	paymoney = "paymoney";      //	   金额   N	Y	单位元（人民币），仅支持整数
    private static final String   	ordernumber = "ordernumber";      //	   商户订单号   N	Y	商户系统订单号。PayOctopus API 将在同步或异步中原样返回，不做任何判断及处理。
    private static final String   	callbackurl = "callbackurl";      //	   下行异步通知地址   N	Y	下行异步通知的地址，需要以http://开头且没有任何参数
    private static final String   	timestamp = "timestamp";      //	   请求时间戳   N	Y	如：1527846677422（毫秒）
    private static final String   	memberId = "memberId";      //	   会员号   N	N	会员号（充值用户唯一识别码）
    private static final String   	sign = "sign";      //	   MD5签名   N	N	32位小写MD5签名值，GB2312编码



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"3.0");
            payParam.put(method,"pay");
            payParam.put(partner,channelWrapper.getAPI_MEMBERID());
            payParam.put(requesttype,"pro");
            payParam.put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(paymoney,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(ordernumber,channelWrapper.getAPI_ORDER_ID());
            payParam.put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            //payParam.put(timestamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(timestamp,System.currentTimeMillis()+"");
            payParam.put(memberId,HandlerUtil.getRandomStr(10));
        }

        log.debug("[八达付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //version=%s&method=%s&partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&timestamp=%s&callbackurl=%s%s
        String paramsStr = String.format("version=%s&method=%s&partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&timestamp=%s&callbackurl=%s%s",
                params.get(version),
                params.get(method),
                params.get(partner),
                params.get(banktype),
                params.get(paymoney),
                params.get(ordernumber),
                params.get(timestamp),
                params.get(callbackurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[八达付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
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
             log.error("[八达付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[八达付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[八达付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}