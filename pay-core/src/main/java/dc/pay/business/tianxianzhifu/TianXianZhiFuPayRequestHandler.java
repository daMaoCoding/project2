package dc.pay.business.tianxianzhifu;

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
 * Feb 26, 2019
 */
@RequestPayHandler("TIANXIANZHIFU")
public final class TianXianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TianXianZhiFuPayRequestHandler.class);

    //字段名 变量名 必填  类型  示例值 描述
    //商户ID    mchId   是   long    20001222    分配的商户号
    private static final String mchId                ="mchId";
    //支付产品ID  productId   是   int 8000    支付产品ID，见附录支付产品
    private static final String productId                ="productId";
    //商户订单号   mchOrderNo  是   String(30)  20160427210604000490    商户生成的订单号
    private static final String mchOrderNo                ="mchOrderNo";
    //币种  currency    是   String(3)   cny 三位货币代码,人民币:cny
    private static final String currency                ="currency";
    //支付金额    amount  是   int 100 支付金额,单位分
    private static final String amount                ="amount";
    //客户端IP   clientIp    是   String(32)  210.73.10.148   客户端IP地址
    private static final String clientIp                ="clientIp";
    //设备  device  否   String(64)  ios10.3.1   客户端设备
//    private static final String device                ="device";
    //支付结果前端跳转URL returnUrl   否   String(128)     支付结果回调URL
//    private static final String returnUrl                ="returnUrl";
    //支付结果后台回调URL notifyUrl   是   String(128) http://商户自己的回调地址    支付结果回调URL
    private static final String notifyUrl                ="notifyUrl";
    //商品主题    subject 是   String(64)  ipay66测试商品1 商品主题
    private static final String subject                ="subject";
    //商品描述信息  body    是   String(256) ipay66  商品描述信息
    private static final String body                ="body";
    //扩展参数1   param1  否   String(64)      支付中心回调时会原样返回
//    private static final String param1                ="param1";
    //扩展参数2   param2  否   String(64)      支付中心回调时会原样返回
//    private static final String param2                ="param2";
    //附加参数    extra   否   String(512) {"openId":"o2RvowBf7sOVJf8kJksUEMceaDqo"}   特定渠道发起时额外参数,见下面说明
//    private static final String extra                ="extra";
    //签名  sign    是   String(32)  C380BEC2BFD727A4B6845133519F3AD6    签名值，详见签名算法
//    private static final String sign                 ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchId, channelWrapper.getAPI_MEMBERID());
                put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(mchOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(currency,"cny");
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(clientIp,  channelWrapper.getAPI_Client_IP());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(subject,"name");
                put(body,"name");
            }
        };
        log.debug("[天线支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
//        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
//        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[天线支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String, String> map = new TreeMap<>();
        map.put("params", JSON.toJSONString(payParam));
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
      //if (StringUtils.isBlank(resultStr)) {
      //    log.error("[天线支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //    throw new PayException(resultStr);
      //    //log.error("[天线支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
      //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      //}
      //if (!resultStr.contains("{") || !resultStr.contains("}")) {
      //   log.error("[天线支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //   throw new PayException(resultStr);
      //}
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[天线支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("retCode"))  && 
      (jsonObject.containsKey("payParams") && StringUtils.isNotBlank(jsonObject.getString("payParams")) || 
       jsonObject.getJSONObject("payParams").containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getJSONObject("payParams").getString("payUrl")))
      ){
//      if (null != jsonObject && jsonObject.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("retCode"))  && jsonObject.containsKey("payParams") && StringUtils.isNotBlank(jsonObject.getString("payParams"))) {
          //payUrl的格式，值如下                1：url地址                2：html内容
          if ("1".equals(jsonObject.getString("type"))) {
              result.put( JUMPURL, jsonObject.getJSONObject("payParams").getString("payUrl"));
          }else if ("2".equals(jsonObject.getString("type"))) {
              result.put( HTMLCONTEXT, jsonObject.getJSONObject("payParams").getString("payUrl"));
          }else {
              log.error("[天线支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
      }else {
          log.error("[天线支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[天线支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[天线支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}