package dc.pay.business.ruyizhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("RUYIZHIFU")
public final class RuYiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RuYiZhiFuPayRequestHandler.class);

    private static final String      merId ="merId";  //  String(10)  商户号  Y
    private static final String      merOrdId ="merOrdId";  //  String(64)  商户网站唯一订单号  Y
    private static final String      merOrdAmt ="merOrdAmt";  //  Number  订单金额，格式：10.00  Y
    private static final String      payType ="payType";  //  String(2)  支付类型：
    private static final String      bankCode ="bankCode";  //  String(7)  银行代码，参考附录银行代码  Y
    private static final String      remark ="remark";  //  String(255)  备注信息，可以随机填写  Y
    private static final String      returnUrl ="returnUrl";  //  String(255)  页面返回地址  Y
    private static final String      notifyUrl ="notifyUrl";  //  String(255)  后台异步通知  Y
    private static final String      signType ="signType";  //  String(5)  签名方式: MD5 或  RSA, 默认MD5  Y
    private static final String      signMsg ="signMsg";  //  String(255)  签名数据  Y



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merId,channelWrapper.getAPI_MEMBERID());
            payParam.put(merOrdId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(merOrdAmt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(remark,channelWrapper.getAPI_OrDER_TIME());
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(signType,"MD5");
        }
        log.debug("[如意支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // merId=%s&merOrdId=%s&merOrdAmt=%s&payType=%s&bankCode=%s&remark=%s&returnUrl=%s&notifyUrl=%s&signType=MD5&merKey=xxx
        String paramsStr = String.format("merId=%s&merOrdId=%s&merOrdAmt=%s&payType=%s&bankCode=%s&remark=%s&returnUrl=%s&notifyUrl=%s&signType=MD5&merKey=%s",
                params.get(merId),
                params.get(merOrdId),
                params.get(merOrdAmt),
                params.get(payType),
                params.get(bankCode),
                params.get(remark),
                params.get(returnUrl),
                params.get(notifyUrl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[如意支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
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

				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//img[@id='qr']").size()==1){
                    HtmlImage payUrlInput = (HtmlImage) endHtml.getByXPath("//img[@id='qr']").get(0);
                    if(payUrlInput!=null ){

                        String qrContentSrc = payUrlInput.getSrcAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(HandlerUtil.getDomain(endHtml.getBaseURI()).concat(qrContentSrc));
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(HandlerUtil.replaceBlank(endHtml.asXml())); }

				

				
//                 resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
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
             log.error("[如意支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[如意支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[如意支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}