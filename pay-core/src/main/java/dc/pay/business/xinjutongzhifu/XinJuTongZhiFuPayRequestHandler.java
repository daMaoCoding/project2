package dc.pay.business.xinjutongzhifu;

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
 * Jun 1, 2019
 */
@RequestPayHandler("XINJUTONGZHIFU")
public final class XinJuTongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinJuTongZhiFuPayRequestHandler.class);

    //名称    类型  必选  示例值 描述
    //mch_id    String(32)  是   1234567890  平台分配的商户号
    private static final String mch_id                ="mch_id";
    //trade_type    string(10)  是   ALIH5   交易类型
    private static final String trade_type                 ="trade_type";
    //nonce string(32)  是   7VS264I5K8502SI8Z    NMTM6LTKCH16CQ2 随机字符串，不长于 3
    private static final String nonce                ="nonce";
    //user_id   string(32)  否       用户ID
    private static final String user_id                ="user_id";
    //timestamp string  是   1524822584  时间戳
    private static final String timestamp              ="timestamp";
    //subject   String(200) 是   商品1 订单名称
    private static final String subject              ="subject";
    //detail    String(500) 否   商品1详情   商品详情
//    private static final String detail             ="detail";
    //out_trade_no  string(32)  是   20150806125346  商户系统内部的订单号，32 个字符内
    private static final String out_trade_no             ="out_trade_no";
    //total_fee Int 是   100 总金额，单位为分
    private static final String total_fee             ="total_fee";
    //spbill_create_ip  string  是   123.12.12.123   终端IP
    private static final String spbill_create_ip             ="spbill_create_ip";
    //timeout   string  否   1   过期时长
//    private static final String timeout             ="timeout";
    //notify_url    string  是       异步地址
    private static final String notify_url             ="notify_url";
    //return_url    string  否       返回地址
//    private static final String return_url             ="return_url";
    //issuer_id string  否       银行机构代码。如果trade_type是GATEWAY，则必填
//    private static final String issuer_id             ="issuer_id";
    //sign_type string  否   RSA 签名类型，目前支持RSA和 MD5，默认为MD5
    private static final String sign_type             ="sign_type";
    //sign  string  是   D0F7DBB5574FB2C64AE5ABCA66950A3D 签名信息
//    private static final String sign             ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(nonce,  HandlerUtil.getRandomStr(8));
                put(user_id,  HandlerUtil.getRandomStr(8));
                put(timestamp,  System.currentTimeMillis()+"");
                put(subject,"name");
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,  channelWrapper.getAPI_AMOUNT());
                put(spbill_create_ip,  channelWrapper.getAPI_Client_IP());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(sign_type,"MD5");
            }
        };
        log.debug("[新聚通支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        log.debug("[新聚通支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[新聚通支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[新聚通支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
//        System.out.println("======>"+resultStr);
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[新聚通支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject jsonObject = JSONObject.parseObject(resultStr);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[新聚通支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
                (jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
                 jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
                ){
                result.put(handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL , jsonObject.getString("pay_url"));
//        if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))) {
//            if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
//                String jump = jsonObject.getString("jump");
//                result.put("1".equals(jump) ? JUMPURL : QRCONTEXT, jsonObject.getString("pay_url"));
//            }else {
//                result.put( JUMPURL, jsonObject.getString("pay_url"));
//            }
        }else {
            log.error("[新聚通支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新聚通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新聚通支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}