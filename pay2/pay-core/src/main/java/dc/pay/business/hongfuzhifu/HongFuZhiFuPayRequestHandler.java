package dc.pay.business.hongfuzhifu;

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
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author cobby
 * May 22, 2019
 */
@RequestPayHandler("HONGFUZHIFU")
public final class HongFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HongFuZhiFuPayRequestHandler.class);

    private static final String merchant_code       ="merchant_code";      //  merchant_code     String(12)    √    参数名称：商家号    商户签约时，掌付分配给商家的唯一身份标识    例如：800003004321
    private static final String service_type        ="service_type";       //  service_type      String(10)    √    参数名称：服务类型        支付宝扫码：alipay_scan    支付宝H5：alipay_h5
    private static final String notify_url          ="notify_url";         //  notify_url        String(200)   √    参数名称：服务器异步通知地址    支付成功后，掌付会主动发送通知给商户，商户必须指定此通知地址
    private static final String interface_version   ="interface_version";  //  interface_version String(10)    √    参数名称：接口版本3.0
    private static final String input_charset       ="input_charset";      //  input_charset     String(5)     √    参数名称：参数编码字符集    取值：UTF-8、GBK(必须大写)
    private static final String sign_type           ="sign_type";          //  sign_type         String(10)    √    参数名称：签名方式1.取值为：RSA或RSA-S，RSA使用pfx证书文件进行数据加密，RSA-S使用字符串密钥进行数据加密，商户需要从中选择一个值2.该字段不参与签名
    private static final String order_no            ="order_no";           //  order_no          String(100)    √    参数名称：商家订单号    商家网站生成的订单号，由商户保证其唯一性，由字母、数字、下划线组成。
    private static final String order_time          ="order_time";         //  order_time        Date           √    参数名称：商家订单时间    时间格式：yyyy-MM-dd HH:mm:ss
    private static final String order_amount        ="order_amount";       //  order_amount      Number(13,2)   √    参数名称：商家订单金额    以元为单位，精确到小数点后两位.例如：12.01
    private static final String product_name        ="product_name";       //  product_name      String(100)    √    参数名称：商品名称
    private static final String extend_param        ="extend_param";       //  extend_param      String         √    参数名称：业务扩展参数    格式:参数名1^参数值1|参数名2^参数值2...，多个参数使用“|”进行分割    例如：name ^Zhang San|sex^Male    跨境商家必选，非跨境商家可选

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_code,channelWrapper.getAPI_MEMBERID());
                put(service_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(interface_version,"V3.0");
                put(input_charset,"UTF-8");
                put(sign_type,"RSA-S");
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_time,DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
                put(order_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(product_name, channelWrapper.getAPI_ORDER_ID());
                put(extend_param,"name ^Zhang San");
            }
        };
        log.debug("[鸿付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> params) throws PayException {
         List paramKeys = MapUtils.sortMapByKeyAsc(params);
         StringBuilder signSrc = new StringBuilder();
         for (int i = 0; i < paramKeys.size(); i++) {
             if (!sign_type.equals(paramKeys.get(i)) && StringUtils.isNotBlank(params.get(paramKeys.get(i)))) {
                 signSrc.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
             }
         }
//         删除最后一个字符
         signSrc.deleteCharAt(signSrc.length()-1);
         String signInfo = signSrc.toString();
         String signMd5="";
         try {
             signMd5 = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY());    // 签名
         } catch (Exception e) {
             log.error("[鸿付支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
             throw new PayException(e.getMessage(),e);
         }
         log.debug("[鸿付支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
         return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
            if (HandlerUtil.isWapOrApp(channelWrapper) ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else {
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");

                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[鸿付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null == jsonObject || !jsonObject.containsKey("response") || !StringUtils.isNotBlank(jsonObject.getString("response"))) {
                    log.error("[鸿付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                jsonObject = JSONObject.parseObject(jsonObject.getString("response"));
                if (null != jsonObject && jsonObject.containsKey("resp_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("resp_code"))
                        && jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode"))) {
                    String code_url = jsonObject.getString("qrcode");
//                    result.put(QRCONTEXT, code_url);
                    if (handlerUtil.isWapOrApp(channelWrapper)) {
                        result.put(JUMPURL, code_url);
                    }else{
                        result.put(QRCONTEXT, code_url);
                    }
                } else {
                    log.error("[鸿付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }

        } catch (Exception e) {
            log.error("[鸿付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[鸿付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[鸿付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}