package dc.pay.business.sutong;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 10, 2019
 */
@RequestPayHandler("SUTONG")
public final class SuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SuTongPayRequestHandler.class);

    //参数名称 参数变量名 类型 必 填 说明
    //异步通知地址 notify_url varchar(200) 是  ⽀付成功后，⽀付平台会主动通知。通知的地址， 例如： http://www.merchant.cn/notify 注意： 不可包含 ?``& 等 符号
    private static final String notify_url                ="notify_url";
    //同步跳转地址 return_url varchar(200) 否     ⽀付成功后，通过⻚⾯跳转的⽅式跳转到商户指定的⽹站⻚⾯ 例如： http://www.merchant.cn/return注意：注意事项同上
    private static final String return_url                 ="return_url";
    //⽀付⽅式 pay_type varchar(2) 是    1为⽹银⽀付    2为微信⽀付    3⽀付宝⽀付    5为QQ钱包    6为JD钱包    7为银联
    private static final String pay_type                ="pay_type";
    //银⾏编码 bank_code varchar(16) 否 当⽀付⽅式为⽹银时必填    (参⻅附录中的银⾏编码对照表)
    private static final String bank_code                ="bank_code";
    //商户号 merchant_code varchar(8) 是 商户注册签约后，⽀付平台分配的
    private static final String merchant_code              ="merchant_code";
    //商户订单号 order_no varchar(32) 是 由商户系统⽣成的唯⼀订单编号
    private static final String order_no              ="order_no";
    //商户订单总⾦    额 order_amount decimal(14,2) 是 订单总⾦额以元为单位，精确到⼩数点后两位
    private static final String order_amount             ="order_amount";
    //商户订单时间 order_time date 是    字符串格式要求为：    yyyy-MM-dd HH:mm:ss    例如： 2017-01-01 12:45:52
    private static final String order_time             ="order_time";
    //来路域名 req_referer varchar(200) 是
    private static final String req_referer             ="req_referer";
    //消费者 IP customer_ip varchar(15) 是
    private static final String customer_ip             ="customer_ip";
    //回传参数 return_params varchar(200) 否 如果商户⽀付请求时传递了该参数，    则通知商户⽀付成功时会回传该参数
    private static final String return_params             ="return_params";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  =" sign ";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                if (HandlerUtil.isWY(channelWrapper)) {
                    put(pay_type,"1");
                    put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                    put(pay_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(merchant_code, channelWrapper.getAPI_MEMBERID());
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_time,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(req_referer,channelWrapper.getAPI_WEB_URL());
                put(customer_ip,channelWrapper.getAPI_Client_IP());
                put(return_params, channelWrapper.getAPI_MEMBERID());
            }
        };
        log.debug("[速通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[速通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[速通]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[速通]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//               log.error("[速通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//               throw new PayException(resultStr);
//            }
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
//                resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[速通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("state") && "true".equalsIgnoreCase(jsonObject.getString("state"))  && jsonObject.containsKey("qrurl") && StringUtils.isNotBlank(jsonObject.getString("qrurl"))) {
                String code_url = jsonObject.getString("qrurl");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[速通]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[速通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[速通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}