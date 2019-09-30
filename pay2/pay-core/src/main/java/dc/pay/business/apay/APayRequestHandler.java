package dc.pay.business.apay;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 18, 2018
 */
@RequestPayHandler("APAY")
public final class APayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(APayRequestHandler.class);

    //参数项   必须  格式  长度/精度   说明
    //merchant_no   √   string  -   商户号
    private static final String merchant_no                ="merchant_no";
    //amount    √   float   2   申请入款金额(元)
    private static final String amount                 ="amount";
    //merchant_billno   √   string  60  商户订单号,唯一
    private static final String merchant_billno                ="merchant_billno";
    //merchant_remark   X   string  100 备注,通知及查询时均会返回
    private static final String merchant_remark                ="merchant_remark";
    //bank  √   string  10  银行,见附录
    private static final String bank              ="bank";
    //return_url    X   string  100 跳转地址
    private static final String return_url              ="return_url";
    //notify_url    X   string  100 通知地址
    private static final String notify_url             ="notify_url";
    //payer_name    X   string  40  付款人,转账收款时需要
    private static final String payer_name             ="payer_name";
    //sign_way  √   string  10  加密方式，md5
    private static final String sign_way             ="sign_way";
    //sign  √   string  32  签名
//    private static final String sign             ="sign";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_no, channelWrapper.getAPI_MEMBERID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(merchant_billno,channelWrapper.getAPI_ORDER_ID());
                put(merchant_remark, channelWrapper.getAPI_MEMBERID());
                put(bank,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payer_name,"name");
                put(sign_way,"md5");
            }
        };
        log.debug("[Apay]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[Apay]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
//      String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      if (StringUtils.isBlank(resultStr)) {
          log.error("[Apay]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
          //log.error("[Apay]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
          //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      }
      resultStr = UnicodeUtil.unicodeToString(resultStr);
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
         log.error("[Apay]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         throw new PayException(resultStr);
      }
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[Apay]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
          JSONObject jsonObject2 = jsonObject.getJSONObject("data");
          if (null != jsonObject2 && jsonObject2.containsKey("deposit_url") && StringUtils.isNotBlank(jsonObject2.getString("deposit_url"))) {
//              result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject2.getString("deposit_url"));
              result.put( JUMPURL, jsonObject2.getString("deposit_url"));
          }else {
              log.error("[Apay]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
      }else {
          log.error("[Apay]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[Apay]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[Apay]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}