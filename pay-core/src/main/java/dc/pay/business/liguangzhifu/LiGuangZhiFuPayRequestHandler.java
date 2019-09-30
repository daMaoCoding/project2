package dc.pay.business.liguangzhifu;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 6, 2019
 */
@RequestPayHandler("LIGUANGZHIFU")
public final class LiGuangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LiGuangZhiFuPayRequestHandler.class);

    //参数名称    类型  必须  描述
    //mch_id  String(16)  是   商户号，我方分配
    private static final String mch_id                ="mch_id";
    //pay_type    String(4)   是   支付类型，101：微信wap    102：微信扫码    201：京东wap    301：支付宝wap    302：支付宝扫码    401：qqwap    501：网银    502：快捷    503：银联扫码
    private static final String pay_type                ="pay_type";
    //describe    String(128) 否   商品简单描述
//    private static final String describe                ="describe";
    //out_trade_no    String(64)  是   商户订单号，必须唯一
    private static final String out_trade_no                ="out_trade_no";
    //attach  String(128) 否   附加数据，在查询API和支付通知中原样返回，该字段主要用于商户携带订单的自定义数据
//    private static final String attach                ="attach";
    //total_fee   String(8)   是   下单价格，单位分
    private static final String total_fee                ="total_fee";
    //back_url    String(255) 否   交易完成后客户端跳转的 URL不能带有参数
//    private static final String back_url                ="back_url";
    //notify_url  String(255) 是   支付成功后异步通知地址
    private static final String notify_url                ="notify_url";
    //ip  String(20)  是   客户端真实ip
    private static final String ip                ="ip";
    //nonce_str   String(32)  是   随机字符串 ,不长于32位
    private static final String nonce_str                ="nonce_str";
    //sign    String(32)  是   Md5签名值统一大写，所有传输的参数除sign都参与签名，按ASCII码升序排序组成key1=value1&key2=value2的字符串，在字符串最后加上&key=密钥，所有参与签名的参数都是没有编码前的原始参数
//    private static final String sign                ="sign";
    //card_type   String(1)   否   银行卡类型，网银必传，1借记卡2贷记卡
//    private static final String card_type                ="card_type";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,  channelWrapper.getAPI_AMOUNT());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ip,  channelWrapper.getAPI_Client_IP());
                put(nonce_str, handlerUtil.getRandomStr(8));
            }
        };
        log.debug("[利光支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[利光支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        //if (StringUtils.isBlank(resultStr)) {
        //    log.error("[利光支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //    throw new PayException(resultStr);
        //    //log.error("[利光支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
        //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        //}
//        System.out.println("请求返回=========>"+resultStr);
        //if (!resultStr.contains("{") || !resultStr.contains("}")) {
        //   log.error("[利光支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //   throw new PayException(resultStr);
        //}
        //JSONObject jsonObject = JSONObject.parseObject(resultStr);
        JSONObject jsonObject;
        try {
            resultStr = URLDecoder.decode(resultStr, "UTF-8");
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[利光支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
        //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
        // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
        //){
        if (null != jsonObject && jsonObject.containsKey("status") && "200".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url"))) {
            String code_url = jsonObject.getString("pay_url");
//            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
            result.put( JUMPURL, code_url);
            //if (handlerUtil.isWapOrApp(channelWrapper)) {
            //    result.put(JUMPURL, code_url);
            //}else{
            //    result.put(QRCONTEXT, code_url);
            //}
        }else {
            log.error("[利光支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[利光支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[利光支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}