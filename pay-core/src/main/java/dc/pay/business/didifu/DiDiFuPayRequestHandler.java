package dc.pay.business.didifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 26, 2018
 */
@RequestPayHandler("DIDIFU")
public final class DiDiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DiDiFuPayRequestHandler.class);

    //字段名              变量名           类型            说明          可空          
    //请求方式            action           string          Bank：银联（需提前开交易权限）          WeiXin：微信（需提前开交易权限）          AliPay：支付宝（需提前开交易权限）          Arbi：由商户选择（开通两个以上交易权限有效）          N          
    //交易金额            txnamt           int             订单金额，单位为分          N          
    //商户号          merid          string          商户号，支付平台时分配          N          
    //商户订单号          orderid          string          由商户生成，必需唯一，长度          8-32位，由字母和数          字组成          N          
    //通知URL             backurl          string          商户系统的地址，支付结束后，通过该url通知商户          交易结果          N          
    //前台URL             fronturl         string          商户系统的地址，客户支付结束后跳转到该页面(get          方式)                    Y          
    //姓名                accname          string          绑卡付必填          Y          
    //身份证号            accno            string          绑卡付必填          Y         
    private static final String action              ="action";
    private static final String txnamt              ="txnamt";
    private static final String merid               ="merid";
    private static final String orderid             ="orderid";
    private static final String backurl             ="backurl";
    private static final String fronturl            ="fronturl";
//    private static final String accname             ="accname";
//    private static final String accno               ="accno";
    
    private static final String req               ="req";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(action,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(txnamt, channelWrapper.getAPI_AMOUNT());
                put(merid,channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(backurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                if (!channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("DIDIFU_BANK_WAP_ZFB_SM")) {
                    put(fronturl,channelWrapper.getAPI_WEB_URL());
                }
            }
        };
        log.debug("[樀樀付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String string = new String(Base64.encodeBase64(JSON.toJSONString(api_response_params).getBytes()));;
        String signMd5 = HandlerUtil.getMD5UpperCase(string+channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[樀樀付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String string = new String(Base64.encodeBase64(JSON.toJSONString(payParam).getBytes()));
        HashMap<String, String> map = Maps.newHashMap();
        map.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        map.put(req, string);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWapOrApp(channelWrapper)) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.GET);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[樀樀付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[樀樀付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            if (!resultStr.contains("{") || !resultStr.contains("}") || !resultStr.contains("resp")) {
               log.error("[樀樀付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
               throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[樀樀付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
//            String resp = new String(Base64.getDecoder().decode(resJson.getString("resp")));
            String resp = new String(Base64.decodeBase64(resJson.getString("resp")));
            if (!resp.contains("{") || !resp.contains("}") || !resp.contains("resp")) {
                log.error("[樀樀付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject respObject = JSONObject.parseObject(resp);
            //只取正确的值，其他情况抛出异常
            if (null != respObject && respObject.containsKey("respcode") && "00".equalsIgnoreCase(respObject.getString("respcode"))  && respObject.containsKey("formaction") && StringUtils.isNotBlank(respObject.getString("formaction"))) {
                result.put(QRCONTEXT, respObject.getString("formaction"));
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[樀樀付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resp) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resp);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[樀樀付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[樀樀付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    public static void main(String[] args) {
//        请求返回=========>{"resp":"eyJyZXNwY29kZSI6IjkwIiwicmVzcG1zZyI6Iuivt+axguWPguaVsOmUmeivr++8gSJ9","sign":""}
        String string2 = new String(Base64.decodeBase64("eyJyZXNwY29kZSI6IjkwIiwicmVzcG1zZyI6Iuivt+axguWPguaVsOmUmeivr++8gSJ9"));
        System.out.println(string2);
    }
}