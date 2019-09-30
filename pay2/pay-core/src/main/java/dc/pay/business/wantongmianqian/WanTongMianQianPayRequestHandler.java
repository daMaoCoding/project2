package dc.pay.business.wantongmianqian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 29, 2019
 */
@RequestPayHandler("WANTONGMIANQIAN")
public final class WanTongMianQianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WanTongMianQianPayRequestHandler.class);

    //3、请求协议参数：
    //表1 支付请求参数说明
    //参数名 参数  可空  加入签名    说明
    //商户号 merchant    N   Y   下发的商户号
    private static final String merchant            ="merchant";
    //金额  amount  N   Y   单位元（人民币），2位小数
    private static final String amount            ="amount";
    //支付方式    pay_code    N   Y   填写相应的支付方式编码
    private static final String pay_code            ="pay_code";
    //商户订单号   order_no    N   Y   订单号，max(50),该值需在商户系统内唯一
    private static final String order_no            ="order_no";
    //异步通知地址  notify_url  N   Y   异步通知地址，需要以http://开头且没有任何参数
    private static final String notify_url            ="notify_url";
    //同步通知地址  return_url  N   Y   同步跳转地址，支付成功后跳回
    private static final String return_url            ="return_url";
    //请求返回方式  json    Y   N   固定值：json; 注意：只适用于扫码付款
    private static final String json            ="json";
    //备注消息    attach  Y   有值加入    回调时原样返回
    private static final String attach            ="attach";
    //请求时间    order_time  Y   Y   格式YYYY-MM-DD hh:ii:ss，回调时原样返回
    private static final String order_time            ="order_time";
    //商户的用户id cuid    Y   有值加入    商户名下的能表示用户的标识，方便对账，回调时原样返回
    private static final String cuid            ="cuid";
    //MD5签名   sign    N   N   32位小写MD5签名值
    private static final String sign            ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant, channelWrapper.getAPI_MEMBERID());
                put(amount,  handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                    put(json,"json");
                }
//                put(order_time, System.currentTimeMillis()+"");
//                put(callback_url,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[万通免签]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
         StringBuilder signSrc = new StringBuilder();
         for (int i = 0; i < paramKeys.size(); i++) {
             if (!json.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                 signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
             }
         }
         signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
         //删除最后一个字符
         //signSrc.deleteCharAt(paramsStr.length()-1);
         String paramsStr = signSrc.toString();
         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
//         String signMd5 = Sha1Util.getSha1(paramsStr).toLowerCase();
         log.debug("[万通免签]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
         return signMd5;
     }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//        if (StringUtils.isBlank(resultStr)) {
//            log.error("[万通免签]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            throw new PayException(resultStr);
//        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[万通免签]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[万通免签]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
//        if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("result") && StringUtils.isNotBlank(resJson.getString("result")) && 
//                (resJson.getJSONObject("result").containsKey("qrCode") && StringUtils.isNotBlank(resJson.getJSONObject("result").getString("qrCode")))){
//        if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("result") && StringUtils.isNotBlank(resJson.getString("result"))) {
        if (resJson.containsKey("QRCodeLink") && StringUtils.isNotBlank(resJson.getString("QRCodeLink"))) {
            result.put(handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL, HandlerUtil.UrlDecode(resJson.getString("QRCodeLink")));
//            result.put(handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL, code_url);
        }else {
            log.error("[万通免签]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[万通免签]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[万通免签]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}