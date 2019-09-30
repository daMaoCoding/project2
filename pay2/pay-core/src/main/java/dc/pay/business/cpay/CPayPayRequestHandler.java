package dc.pay.business.cpay;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 6, 2019
 */
@RequestPayHandler("CPAY")
public final class CPayPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CPayPayRequestHandler.class);

    //参数名 必选 类型 说明 
    //userId 是 string 商户ID
    private static final String userId                ="userId";
    //merchantUserID 是 string 商户系统用户号
    private static final String merchantUserID                ="merchantUserID";
    //userOrder 是 string 用户订单号
    private static final String userOrder                ="userOrder";
    //number 是 string 金额小数点后两位
    private static final String number                ="number";
    //payType 是 string 支付类型:0 支付宝,1 微信，2 papal 3 银行卡
    private static final String payType                ="payType";
    //isPur 是 string 固定值 1
    private static final String isPur                ="isPur";
    //remark 是 string 原值返回
    private static final String remark                ="remark";
    //appID 是 string APPID
    private static final String appID                ="appID";
    //ckValue 是 string 
//    private static final String ckValue                ="ckValue";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="ckValue";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[cpay]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：用户身份userId&APPID" );
            throw new PayException("[cpay]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：用户身份userId&APPID" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(userId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(merchantUserID, handlerUtil.getRandomStr(8));
                put(userOrder,channelWrapper.getAPI_ORDER_ID());
                put(number,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(isPur,"1");
                put(remark, channelWrapper.getAPI_ORDER_ID());
                put(appID, channelWrapper.getAPI_MEMBERID().split("&")[1]);
            }
        };
        log.debug("[cpay]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(api_response_params.get(userId)).append("|");
        signSrc.append(api_response_params.get(merchantUserID)).append("|");
        signSrc.append(api_response_params.get(userOrder)).append("|");
        signSrc.append(api_response_params.get(number)).append("|");
        signSrc.append(api_response_params.get(payType)).append("|");
        signSrc.append(api_response_params.get(isPur)).append("|");
        signSrc.append(api_response_params.get(remark)).append("|");
        signSrc.append(api_response_params.get(appID)).append("|");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[cpay]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

//      String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      //if (StringUtils.isBlank(resultStr)) {
      //    log.error("[cpay]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //    throw new PayException(resultStr);
      //    //log.error("[cpay]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
      //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      //}
      //if (!resultStr.contains("{") || !resultStr.contains("}")) {
      //   log.error("[cpay]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //   throw new PayException(resultStr);
      //}
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[cpay]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("resultCode") && "0000".equalsIgnoreCase(jsonObject.getString("resultCode"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))  && 
      (jsonObject.getJSONObject("data").containsKey("payPage") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("payPage")))
      ){
//      if (null != jsonObject && jsonObject.containsKey("resultCode") && "0000".equalsIgnoreCase(jsonObject.getString("resultCode"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
          String code_url = jsonObject.getJSONObject("data").getString("payPage");
          result.put( JUMPURL, code_url);
          //if (handlerUtil.isWapOrApp(channelWrapper)) {
          //    result.put(JUMPURL, code_url);
          //}else{
          //    result.put(QRCONTEXT, code_url);
          //}
      }else {
          log.error("[cpay]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[cpay]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[cpay]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}