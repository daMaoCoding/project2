package dc.pay.business.jisuzhifu;

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
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("JISUZHIFU")
public final class JiSuZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


     private static final String    account_id = "account_id";   //	商户ID、在平台首页右边获取商户ID	10000
     private static final String    content_type = "content_type";   //	请求过程中返回的网页类型，text (扫码支付 )或 json（H5支付）	json
     private static final String    thoroughfare = "thoroughfare";   //	初始化支付通道，目前通道：wechat_auto（商户版微信）、alipay_auto（商户版支付宝）、service_auto（服务版微信/支付宝）	wechat_auto
     private static final String    type = "type";   //	支付类型，该参数在服务版下有效（service_auto），其他可为空参数，微信：1，支付宝：2	1
     private static final String    out_trade_no = "out_trade_no";   //	订单信息，在发起订单时附加的信息，如用户名，充值订单号等字段参数	2018062668945
     private static final String    robin = "robin";   //	轮训，2：开启轮训，1：进入单通道模式	2
     private static final String    keyId = "keyId";   //	设备KEY，在商户版列表里面Important参数下的DEVICE Key一项，如果该请求为轮训模式，则本参数无效，本参数为单通道模式	785D239777C4DE7739
     private static final String    amount = "amount";   //	支付金额，在发起时用户填写的支付金额	1.00
     private static final String    callback_url = "callback_url";   //	异步通知地址，在支付完成时，本平台服务器系统会自动向该地址发起一条支付成功的回调请求, 对接方接收到回调后，必须返回 success ,否则默认为回调失败,回调信息会补发3次。	http://39.108.180.85/index/index/callback.do
     private static final String    success_url = "success_url";   //	支付成功后网页自动跳转地址，仅在网页类型为text下有效，json会将该参数返回	http://39.108.180.85/index/doc/getQrcode.do
     private static final String    error_url = "error_url";   //	支付失败时，或支付超时后网页自动跳转地址，仅在网页类型为text下有效，json会将该参数返回	http://39.108.180.85/index/doc/getQrcode.do
     private static final String    ip = "ip";   //	客户请求生成二维码的最终IP	127.0.0.1
     private static final String    sign = "sign";   //	签名算法，在支付时进行签名算法，详见《极速支付签名算法》	d92eff67b3be05f5e61502e96278d01b



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(account_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(content_type,"json");
            payParam.put(thoroughfare,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(robin,"2");
           // payParam.put(keyId, );
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) );
            payParam.put(callback_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(success_url, channelWrapper.getAPI_WEB_URL());
            payParam.put(error_url,channelWrapper.getAPI_WEB_URL() );
            payParam.put(ip,channelWrapper.getAPI_Client_IP() );
        }

        log.debug("[极速支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params)  {
        String pay_md5sign = JiSuZhiFuSign.getSign(params.get(amount),params.get(out_trade_no),channelWrapper.getAPI_KEY());
        log.debug("[极速支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code"))
                            && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data")
                            && jsonResultStr.getJSONObject("data").containsKey("qrcode_url2")
                            && StringUtils.isNotBlank(jsonResultStr.getJSONObject("data").getString("qrcode_url2"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getJSONObject("data").getString("qrcode_url2"));
                        }else{
                            result.put(QRCONTEXT,jsonResultStr.getJSONObject("data").getString("qrcode_url2"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[极速支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[极速支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[极速支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}