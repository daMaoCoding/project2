package dc.pay.business.quanyuzhifu;

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
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("QUANYUZHIFU")
public final class QuanYuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QuanYuZhiFuPayRequestHandler.class);

	 private static final String   userId = "userId";    //	下放商户标识	是	平台发放的商户号
	 private static final String   amount = "amount";    //	交易金额	是	单位:元
	 private static final String   outOrderNo = "outOrderNo";    //	唯一订单号	是	请确保全平台唯一
	 private static final String   notifyUrl = "notifyUrl";    //	回调地址	是	支付成功回调地址
     private static final String   payType = "payType";    //  支付类型
     private static final String   payerIp = "payerIp";    // 支付ip
	 private static final String   sign = "sign";    //	签名	是	详见demo



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(userId,channelWrapper.getAPI_MEMBERID());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(outOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(payerIp,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[全宇支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        log.debug("[全宇支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr="";
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

//				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
//                String qrContent=null;
//                String s = endHtml.asXml();
//                System.out.println(s);
//                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
//                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
//                    if(payUrlInput!=null ){
//                        String qrContentSrc = payUrlInput.getValueAttribute();
//                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
//                    }
//                }
//               if(StringUtils.isNotBlank(qrContent)){
//                    result.put(QRCONTEXT, qrContent);
//                    payResultList.add(result);
//                }else {  throw new PayException(endHtml.asXml()); }
//


                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();

                String getJsonUrl = Jsoup.parse(resultStr).select("input[id=baseUrl]").first().attr("value");
                if(StringUtils.isNotBlank(getJsonUrl)){
                    JSONObject jsonObject = JSON.parseObject(HttpUtil.get(getJsonUrl, null, null).getBody());
                    if(jsonObject!=null && jsonObject.containsKey("code") && "000000".equalsIgnoreCase(jsonObject.getString("code")) && null!=jsonObject.getJSONObject("data")){
                        JSONObject qrDatas = jsonObject.getJSONObject("data");
                        String code = qrDatas.getString("code");
                        if(StringUtils.isNotBlank(code)){
                            result.put(QRCONTEXT,code);
                            payResultList.add(result);
                        }else{throw new PayException(resultStr);}
                    }else{throw new PayException(resultStr);}

                }else{throw new PayException(resultStr);}

//                if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
//                    result.put(HTMLCONTEXT,resultStr);
//                    payResultList.add(result);
//                }else{
//
//                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
//                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
//                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
//                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
//                                payResultList.add(result);
//                            }
//                    }else {
//                        throw new PayException(resultStr);
//                    }
//				}
                 
            }
        } catch (Exception e) { 
             log.error("[全宇支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(resultStr, e);
        }
        log.debug("[全宇支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[全宇支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}