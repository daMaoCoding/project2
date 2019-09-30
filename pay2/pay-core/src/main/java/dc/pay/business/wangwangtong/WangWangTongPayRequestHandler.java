package dc.pay.business.wangwangtong;

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

@RequestPayHandler("WANGWANGTONGZHIFU")
public final class WangWangTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WangWangTongPayRequestHandler.class);

     private static final String   fxid = "fxid";   //	商务号	是	唯一号，由捷付通提供
     private static final String   fxddh = "fxddh";   //	商户订单号	是	仅允许字母或数字类型,不超过22个字符，不要有中文
     private static final String   fxdesc = "fxdesc";   //	商品名称	是	utf-8编码
     private static final String   fxfee = "fxfee";   //	支付金额	是	请求的价格(单位：元) 可以0.01元
     private static final String   fxnotifyurl = "fxnotifyurl";   //	异步通知地址	是	异步接收支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
     private static final String   fxbackurl = "fxbackurl";   //	同步通知地址	是	支付成功后跳转到的地址，不参与签名。
     private static final String   fxpay = "fxpay";   //	请求类型	是	请求支付的接口类型， 固定取值为: alipay。
     private static final String   fxsign = "fxsign";   //	签名【md5(商务号+商户订单号+支付金额+异步通知地址+商户秘钥)】	是	通过签名算法计算得出的签名值。
     private static final String   fxip = "fxip";   //	支付用户IP地址	是	用户支付时设备的IP地址


    private static final String  status = "status";   // 1,
    private static final String  type = "type";   // 0,
    private static final String  payurl = "payurl" ;   //"https://qr.alipay.com/bax03674oeeezclybq6a0092",
    private static final String  payimg = "payimg" ;   //"http://qr.topscan.com/api.php?text=https%3A%2F%2Fqr.alipay.com%2Fbax03674oeeezclybq6a0092&w=150&m=10"

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(fxid,channelWrapper.getAPI_MEMBERID());
            payParam.put(fxddh,channelWrapper.getAPI_ORDER_ID());
            payParam.put(fxdesc,channelWrapper.getAPI_ORDER_ID());
            payParam.put(fxfee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(fxnotifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(fxbackurl,channelWrapper.getAPI_WEB_URL());
            payParam.put(fxpay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(fxip,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[旺旺通支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }


    protected String buildPaySign(Map<String,String> params) throws PayException {
        //商务号+商户订单号+支付金额+异步通知地址+商户秘钥
        //fxsign = md5($data["fxid"] . $data["fxddh"] . $data["fxfee"] . $data["fxnotifyurl"] . $fxkey) ;
        String paramsStr = String.format("%s%s%s%s%s",
                params.get(fxid),
                params.get(fxddh),
                params.get(fxfee),
                params.get(fxnotifyurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[旺旺通支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper)  &&  HandlerUtil.isYLKJ(channelWrapper)   &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
                resultStr = UnicodeUtil.unicodeToString(resultStr);
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey(status) && "1".equalsIgnoreCase(jsonResultStr.getString(status))
                            && jsonResultStr.containsKey(payurl) && StringUtils.isNotBlank(jsonResultStr.getString(payurl))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString(payurl));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString(payurl));
                        }
                        payResultList.add(result);
                    }else if(StringUtils.isBlank(resultStr) ){  throw new PayException(EMPTYRESPONSE);  }else {throw new PayException(resultStr); }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[旺旺通支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[旺旺通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[旺旺通支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}