package dc.pay.business.yinshangzhifu;

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
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("SHANGYINZHIFU")
public final class YinShangZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


      private static final String     p0_Cmd = "p0_Cmd";     //	业务类型	是	Max(20)	固定值“Buy”	1
      private static final String     p1_MerId = "p1_MerId";     //	商户编号	是	Max(11)		2
      private static final String     p2_Order = "p2_Order";     //	商户订单号	是	Max(30)	仅限英文字母和数字,在单个自然日内不允许重复	3
      private static final String     p3_Amt = "p3_Amt";     //	支付金额	是	Max(20)	单位:元，精确到分.最少1元	4
      private static final String     p4_Cur = "p4_Cur";     //	交易币种	是	Max(10)	固定值 ”CNY”.	5
      private static final String     pr_NeedResponse = "pr_NeedResponse";     //	应答机制	是	Max(1)	固定值为“1”:	13
      private static final String     p8_Url = "p8_Url";    //	后台通知地址	否	Max(200)	后台通知地址
      private static final String     hmac = "hmac";     //	签名数据	是	Max(32)
      private static final String     pd_FrpId = "pd_FrpId";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(p0_Cmd,"Buy");
            payParam.put(p1_MerId,channelWrapper.getAPI_MEMBERID());
            payParam.put(p2_Order,channelWrapper.getAPI_ORDER_ID());
            payParam.put(p3_Amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(p4_Cur,"CNY");
            payParam.put(p8_Url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pr_NeedResponse,"1");
            payParam.put(pd_FrpId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        }

        log.debug("[商银支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }


    protected String buildPaySign(Map<String,String> params) throws PayException {
      //  p0_Cmd^|^p1_MerId^|^p2_Order^|^p3_Amt^|^p4_Cur^|^^|^^|^^|^p8_Url^|^^|^^|^pd_FrpId^|^pr_NeedResponse^|^
        String paramsStr = String.format("%s^|^%s^|^%s^|^%s^|^%s^|^^|^^|^^|^%s^|^^|^^|^%s^|^%s^|^",
                params.get(p0_Cmd),
                params.get(p1_MerId),
                params.get(p2_Order),
                params.get(p3_Amt),
                params.get(p4_Cur),
                params.get(p8_Url),
                params.get(pd_FrpId),
                params.get(pr_NeedResponse));
        String signMd5 = DigestUtil.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[商银支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==1 ||HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
				
				if(StringUtils.isNotBlank(resultStr) && (HandlerUtil.isYLSM(channelWrapper) || HandlerUtil.isWxSM(channelWrapper)||HandlerUtil.isZfbSM(channelWrapper))){
                    String qrimageBase64Content= Jsoup.parse(resultStr).select("div>img").eq(0).attr("src");
                    if(StringUtils.isBlank(qrimageBase64Content)) throw new PayException(resultStr);
                    String qrContent = QRCodeUtil.decodeByUrl(qrimageBase64Content);
                    result.put(QRCONTEXT,qrContent);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "1".equalsIgnoreCase(jsonResultStr.getString("status"))
                            && jsonResultStr.containsKey("payurl") && StringUtils.isNotBlank(jsonResultStr.getString("payurl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("payurl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("payurl"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[商银支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[商银支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[商银支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}