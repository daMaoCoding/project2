package dc.pay.business.juqian;

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
 * Nov 16, 2018
 */
@RequestPayHandler("JUQIAN")
public final class JuQianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuQianPayRequestHandler.class);

    //中文名                参数名                     必选            类型                    说明          
    //接口名称               method                    是             String(32)                alipay.sqm.h5          
    //版本信息               version                   是             String(8)                1.0          
    //签名类型               sign_type                 否             String(10)                请填写交易密钥对应的签名类型，如MD5          
    //随机字符串             nonce_str                 是             String(32)                随机字符串，不大于32位。推荐随机数生成算法          
    //商户号                 mch_id                    是             String(20)                平台分配的商户号          
    //商户订单号             mch_order_no              是             String(32)                商户系统内部订单号，要求32个字符内，只能是数字、大小写字母，且在同一个商户号下唯一          
    //商品名称               body                      是             String(128)                商品简单描述          
    //商品描述               detail                    否             String(1024)                对商品的描述信息          
    //币种                   cur_code                  是             String(10)                货币类型，符合          ISO4217标准的三位字母代码。目前仅支持人民币，CNY          
    //总金额                 total_amount              是             Decimal(16,2)                总金额(单位元，两位小数)          
    //终端IP                 spbill_create_ip          是             String(20)                终端IP          
    //附加数据               attach                    否             String(128)                附加数据，在查询API和支付通知中原样返回，可作为自定义参数使用。          
    //订单提交时间           mch_req_time              是             String(14)                订单生成时间，格式为yyyyMMddHHmmss，如2009年12月25日9点10分10秒表示为20091225091010请使用UTC+8          北京时间          
    //通知地址               notify_url                是             String(128)                后台通知地址，用于接收支付成功通知          
    //签名                   sign                      是             String(1024)                签名值          
    private static final String method                       ="method";
    private static final String version                      ="version";
    private static final String sign_type                    ="sign_type";
    private static final String nonce_str                    ="nonce_str";
    private static final String mch_id                       ="mch_id";
    private static final String mch_order_no                 ="mch_order_no";
    private static final String body                         ="body";
//    private static final String detail                       ="detail";
    private static final String cur_code                     ="cur_code";
    private static final String total_amount                 ="total_amount";
    private static final String spbill_create_ip             ="spbill_create_ip";
    private static final String attach                       ="attach";
    private static final String mch_req_time                 ="mch_req_time";
    private static final String notify_url                   ="notify_url";
//    private static final String sign                         ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(method,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(version,"1.0");
                put(sign_type,"MD5");
                put(nonce_str,  HandlerUtil.getRandomStr(8));
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(mch_order_no,channelWrapper.getAPI_ORDER_ID());
                put(body,"name");
                put(cur_code,"CNY");
                put(total_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(spbill_create_ip,channelWrapper.getAPI_Client_IP());
                put(attach, channelWrapper.getAPI_MEMBERID());
                put(mch_req_time,  DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[聚前]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[聚前]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
       String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//       if (StringUtils.isBlank(resultStr)) {
//           log.error("[聚前]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//           throw new PayException(resultStr);
//           //log.error("[聚前]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//           //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//       }
//       if (!resultStr.contains("{") || !resultStr.contains("}")) {
//          log.error("[聚前]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//          throw new PayException(resultStr);
//       }
       //JSONObject resJson = JSONObject.parseObject(resultStr);
       JSONObject resJson;
       try {
           resJson = JSONObject.parseObject(resultStr);
       } catch (Exception e) {
           e.printStackTrace();
           log.error("[聚前]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
       }
       //只取正确的值，其他情况抛出异常
       if (null != resJson && resJson.containsKey("return_code") && "SUCCESS".equalsIgnoreCase(resJson.getString("return_code"))  && resJson.containsKey("code_url") && StringUtils.isNotBlank(resJson.getString("code_url"))) {
           String code_url = resJson.getString("code_url");
//           result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
           result.put( JUMPURL, code_url);
       }else {
           log.error("[聚前]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
       }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[聚前]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[聚前]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}