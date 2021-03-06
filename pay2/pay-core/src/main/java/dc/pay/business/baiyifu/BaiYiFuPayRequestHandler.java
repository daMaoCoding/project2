package dc.pay.business.baiyifu;

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
@RequestPayHandler("BAIYIFU")
public final class BaiYiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


    private static final String   	amount = "amount";      //	   订单金额  是	String(32)	分数为单位
    private static final String   	mch_id = "mch_id";      //	   版本号  是	String(10)	我司分配的商户号(唯一)
    private static final String   	notify_url = "notify_url";      //	   异步通知地址  是	String(255)	接收通知的URL，需给真实路径(要能够访问)
    private static final String   	out_trade_no = "out_trade_no";      //	   商户订单号  是	String(15)	订单号(15位长度内,必须唯一)
    private static final String   	mch_create_ip = "mch_create_ip";      //	   IP地址  是	String(32)	真实ip地址
    private static final String   	time_start = "time_start";      //	   订单日期  是	String(32)	例如: 20180811180825
    private static final String   	body = "body";      //	   商品描述  是	String(128)	商品描述(当网银支付,该字段表示银行编码)若不是网银默认传00
    private static final String   	attach = "attach";      //	   附加信息  是	String(128)	商户附加信息，默认传00
    private static final String   	nonce_str = "nonce_str";      //	   随机字符串  是	String(20)	随机8为字符串
    private static final String   	trade_type = "trade_type";      //	   支付渠道号  是		看红色备注
    private static final String   	paytype = "paytype";      //	   支付方式  是	String(10)	看红色备注
    private static final String   	back_url = "back_url";      //	   前台地址  是	String(255)	注:该地址只作为前端页面的一个跳转，须使用notify_url通知结果作为支付最终结果。
    private static final String   	phone = "phone";      //	   手机号  是	String(20)	手机号
    private static final String   	sign = "sign";      //	   签名  是	String(32)	MD5签名结果





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(mch_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(mch_create_ip,channelWrapper.getAPI_Client_IP());
            payParam.put(time_start,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(body, "00");
            payParam.put(attach, channelWrapper.getAPI_ORDER_ID());
            payParam.put(nonce_str,HandlerUtil.getRandomStr(8) );
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0] );
            payParam.put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]  );
            payParam.put(back_url,channelWrapper.getAPI_WEB_URL() );
            payParam.put(phone,"130".concat(HandlerUtil.getRandomNumber(8)) );
        }

        log.debug("[百益付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        log.debug("[百益付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
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
             log.error("[百益付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[百益付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[百益付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}