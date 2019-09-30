package dc.pay.business.mingfuzhifu;

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

@RequestPayHandler("MINGFUZHIFU")
public final class MingFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MingFuZhiFuPayRequestHandler.class);

    private static final String      sign_type = "sign_type";  ///  MD5
    private static final String      signature = "signature";  ///
    private static final String      biz_content = "biz_content";  ///

    private static final String version ="version";       //      版本号   是    String(5)    1.0    默认为 1.0
    private static final String mch_id ="mch_id";       //      商户号   是    String(32)    1900000109    系统分配的商户号
    private static final String out_order_no ="out_order_no";       //  商户订单号
    private static final String pay_platform ="pay_platform";       //      支付平台   WXPAY-微信； ALIAPY-支付宝 SQPAY-手 Q
    private static final String pay_type ="pay_type";       //      交易类型   是    String(16)  MWEB-支付宝 H5
    private static final String payment_fee ="payment_fee";       //      总金额   是    Int(11)    888    订单总金额，只能为整数(分)
    private static final String cur_type ="cur_type";       //    币种   是  String(16)  CNY    币种：CNY-人民币；
    private static final String body ="body";       //    商品名称   是  String(40)  春季服装
    private static final String notify_url ="notify_url";       //    通知地址   是  String(200)
    private static final String bill_create_ip ="bill_create_ip";       //    终端IP   是  String(15)  123.12.12.123



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newLinkedHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"1.0");
            payParam.put(mch_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(out_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_platform,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(payment_fee,channelWrapper.getAPI_AMOUNT());
            payParam.put(cur_type,"CNY");
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(bill_create_ip,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[明付支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        StringBuilder sb = new StringBuilder();
        sb.append(biz_content).append("=");
        sb.append(JSON.toJSONString(params)).append("&key=").append(channelWrapper.getAPI_KEY());
        pay_md5sign = HandlerUtil.getMD5UpperCase( sb.toString());
        log.debug("[明付支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        HashMap<String, String> reqPayParm = Maps.newHashMap();
        reqPayParm.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        reqPayParm.put(sign_type, "MD5");
        reqPayParm.put(biz_content,JSON.toJSONString(payParam));


        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper)  &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),reqPayParm).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqPayParm, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("ret_code") && "0".equalsIgnoreCase(jsonResultStr.getString("ret_code")) && jsonResultStr.containsKey("biz_content") && StringUtils.isNotBlank(jsonResultStr.getString("biz_content"))){
                        JSONObject biz_content = jsonResultStr.getJSONObject("biz_content");
                        if(null!=biz_content &&  (biz_content.containsKey("mweb_url") ||  biz_content.containsKey("qrcode")   )){
                                if(StringUtils.isNotBlank(biz_content.getString("mweb_url"))){
                                    if(HandlerUtil.isWapOrApp(channelWrapper)){result.put(JUMPURL,  biz_content.getString("mweb_url"));}else{result.put(QRCONTEXT,  biz_content.getString("mweb_url"));}
                                }else if( StringUtils.isNotBlank(biz_content.getString("qrcode"))){
                                    result.put(QRCONTEXT,  biz_content.getString("qrcode"));
                                }else{ throw new PayException(resultStr);}
                                payResultList.add(result);
                            }else{ throw new PayException(resultStr);}

                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[明付支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[明付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[明付支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}