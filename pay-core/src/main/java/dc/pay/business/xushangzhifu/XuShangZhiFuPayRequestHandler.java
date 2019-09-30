package dc.pay.business.xushangzhifu;

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
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("XUSHANGZHIFU")
public final class XuShangZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


    private static final String   mch_id = "mch_id";    //	String(32)	是	商户号，由支付平台分配
    private static final String   service = "service";    //	String(32)	是	服务类型，固定值：pay.alipay
    private static final String   out_trade_no = "out_trade_no";    //	String(32)	是	商户系统内部的订单号,32个字符内、可包含字母
    private static final String   trade_time = "trade_time";    //	String(32	是	订单交易时间，YYYYmmDDHHMiSS，如：20171001122345
    private static final String   subject = "subject";    //	String(64)	是	商品标题
    private static final String   body = "body";    //	String(128)	是	商品描述
    private static final String   total_fee = "total_fee";    //	String(12)	是	总金额，以元为单位，不允许包括小数点之外的字符。如一分钱为0.01
    private static final String   spbill_create_ip = "spbill_create_ip";    //	String(16)	是	用户支付提交的ip地址
    private static final String   notify_url = "notify_url";    //	String(256)	是	接收支付异步通知回调地址，通知url必须为直接可访问的url，不能携带参数。
    private static final String   return_url = "return_url";    //	String(256)	是	接收支付界面通知返回地址， url必须为直接可访问的url。
    private static final String   url_type = "url_type";    //	String(2)	否	返回的mweb_url和code_url为原生码，还是转跳码，0或者为空表示原生码；1:转跳码。 主要解决支付宝10.1.38以上版本个人原生码在IOS上无法唤醒支付宝支付的问题。
    private static final String   sign_type = "sign_type";    //	String(16)	是	签名类型：MD5
    private static final String   trade_type = "trade_type";    //	String(16)	是	取值如下：JSAPI，NATIVE ,H5,APP
    private static final String   sign = "sign";    //	String(32)	是	MD5签名结果




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mch_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(trade_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(spbill_create_ip, channelWrapper.getAPI_Client_IP());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL() );
            payParam.put(url_type,"1" );
            payParam.put(sign_type, "MD5");
            payParam.put(trade_type, "H5");
        }
        log.debug("[旭商支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[旭商支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
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
                    if(null!=jsonResultStr && jsonResultStr.containsKey("return_code") && "success".equalsIgnoreCase(jsonResultStr.getString("return_code"))
                            && jsonResultStr.containsKey("mweb_url") && StringUtils.isNotBlank(jsonResultStr.getString("mweb_url"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("mweb_url"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("mweb_url"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[旭商支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[旭商支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[旭商支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}