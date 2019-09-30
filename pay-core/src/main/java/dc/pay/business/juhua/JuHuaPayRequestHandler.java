package dc.pay.business.juhua;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 23, 2019
 */
@RequestPayHandler("JUHUA")
public final class JuHuaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuHuaPayRequestHandler.class);

    //公共参数
    //参数  参数名称    类型  参数说明    是否必填
    //MERCNUM 商户号 String  分配的商户号  是
    private static final String MERCNUM                ="MERCNUM";
    //TRANDATA    交易数据    String  交易核心数据  是
    private static final String TRANDATA                ="TRANDATA";
    //SIGN    签名数据    String  对TRANDATA数据签名后的数据   是
//    private static final String SIGN                ="SIGN";
    
    //交易数据TRANDATA 格式：
    //参数  参数名称    类型  参数说明    是否必填
    //ORDERNO 商户订单号   String  商户订单号   是
    private static final String ORDERNO                 ="ORDERNO";
    //TXNAMT  订单金额    String  订单金额（分） 是
    private static final String TXNAMT                 ="TXNAMT";
    //MERRTURL    后台回调地址  String  后台回调地址  是
//    private static final String MERRTURL                 ="MERRTURL";
    private static final String MERNOTIFYURL                 ="MERNOTIFYURL";
    //PRO_ID  产品id    String  ZFBQR_PAY   是
    private static final String PRO_ID                ="PRO_ID";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="SIGN";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(ORDERNO,channelWrapper.getAPI_ORDER_ID());
                put(TXNAMT,  channelWrapper.getAPI_AMOUNT());
//                put(MERRTURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(MERNOTIFYURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(PRO_ID,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[聚华]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(paramsStr,channelWrapper.getAPI_KEY(),"SHA1WithRSA");    // 签名
        } catch (Exception e) {
            log.error("[聚华]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        //String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚华]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
        byte[] encode = Base64.getEncoder().encode(paramsStr.getBytes());
        StringBuffer src= new StringBuffer();
        src.append(MERCNUM+"=").append(channelWrapper.getAPI_MEMBERID()).append("&");
        src.append(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME()+"=").append(handlerUtil.UrlEncode(pay_md5sign)).append("&");
        src.append(TRANDATA+"=").append(handlerUtil.UrlEncode(new String(encode)));
        String params = src.toString();
        Map<String,String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), params,MediaType.APPLICATION_FORM_URLENCODED_VALUE).trim();
        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
        } catch (Exception e) {
            log.error("[聚华]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
            //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(e.getMessage(),e);
        }          
        //只取正确的值，其他情况抛出异常
        //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
        //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
        // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
        //){
        if (null != jsonObject && jsonObject.containsKey("RECODE") && "000000".equalsIgnoreCase(jsonObject.getString("RECODE"))  && jsonObject.containsKey("URL") && StringUtils.isNotBlank(jsonObject.getString("URL"))) {
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject.getString("URL"));
        }else {
            log.error("[聚华]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[聚华]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[聚华]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}