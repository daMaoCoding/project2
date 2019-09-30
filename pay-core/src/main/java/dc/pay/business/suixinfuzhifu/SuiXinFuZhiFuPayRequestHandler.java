package dc.pay.business.suixinfuzhifu;

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
import dc.pay.business.tongsao.TongSaoRequestHandler;
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
 * Apr 6, 2019
 */
@RequestPayHandler("SUIXINFUZHIFU")
public final class SuiXinFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongSaoRequestHandler.class);

//    //快捷接口
//    //请求字段名称  说明
//    //merchantCode    商户号
//    private static final String merchantCode                ="merchantCode";
//    //amount  订单金额
//    private static final String amount                ="amount";
//    //orderNumber 商户支付订单号（长度50以内）
//    private static final String orderNumber                ="orderNumber";
//    //payCode 对照附录支付编码表
//    private static final String payCode                ="payCode";
//    //submitTime  下单时间，格式(yyyyMMddHHmmss)
//    private static final String submitTime                ="submitTime";
//    //commodityName   支付产品名称(超出部分将会被截断)
//    private static final String commodityName                ="commodityName";
//    //submitIp    下单IP(必须符合IPv4地址规范)
//    private static final String submitIp                ="submitIp";
//    //syncRedirectUrl 页面通知地址
//    private static final String syncRedirectUrl                ="syncRedirectUrl";
//    //asyncNotifyUrl  后台异步通知地址
//    private static final String asyncNotifyUrl                ="asyncNotifyUrl";
//    //bankCode    银行编码
//    private static final String bankCode                ="bankCode";
//    //bankAccountType 银行卡类型，1:对公, 0:对私
//    private static final String bankAccountType                ="bankAccountType";
//    //identityNo  身份证
//    private static final String identityNo                ="identityNo";
//    //bankNo  银行卡号
//    private static final String bankNo                ="bankNo";
//    //remark  备注
//    private static final String remark                ="remark";
//    //sign    MD5签名
//    private static final String sign                ="sign";
    
    //扫码接口
    //请求字段名称  说明
    //merchantCode    商户号
    private static final String merchantCode                ="merchantCode";
    //amount  订单金额
    private static final String amount                ="amount";
    //orderNumber 商户支付订单号（长度50以内）
    private static final String orderNumber                ="orderNumber";
    //payCode 对照附录支付编码表
    private static final String payCode                ="payCode";
    //submitTime  下单时间，格式(yyyyMMddHHmmss)
    private static final String submitTime                ="submitTime";
    //commodityName   支付产品名称(超出部分将会被截断)
    private static final String commodityName                ="commodityName";
    //submitIp    下单IP(必须符合IPv4地址规范)
    private static final String submitIp                ="submitIp";
    //syncRedirectUrl 页面通知地址
    private static final String syncRedirectUrl                ="syncRedirectUrl";
    //asyncNotifyUrl  后台异步通知地址
    private static final String asyncNotifyUrl                ="asyncNotifyUrl";
    //remark  备注
    private static final String remark                ="remark";
    //sign    MD5签名
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantCode, channelWrapper.getAPI_MEMBERID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderNumber,channelWrapper.getAPI_ORDER_ID());
                put(payCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(submitTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(commodityName,"name");
                put(submitIp,channelWrapper.getAPI_Client_IP());
                put(syncRedirectUrl,channelWrapper.getAPI_WEB_URL());
                put(asyncNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(remark,"name");
            }
        };
        log.debug("[随心付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append( channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[随心付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

//      String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      //if (StringUtils.isBlank(resultStr)) {
      //    log.error("[随心付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //    throw new PayException(resultStr);
      //    //log.error("[随心付支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
      //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      //}
//      System.out.println("请求返回=========>"+resultStr);
      //if (!resultStr.contains("{") || !resultStr.contains("}")) {
      //   log.error("[随心付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //   throw new PayException(resultStr);
      //}
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[随心付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
      //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
      // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
      //){
      if (null != jsonObject && jsonObject.containsKey("returnCode") && "0".equalsIgnoreCase(jsonObject.getString("returnCode"))  && jsonObject.containsKey("content") && StringUtils.isNotBlank(jsonObject.getString("content"))
              && !jsonObject.getString("content").contains("localizedMessage") && !jsonObject.getString("content").contains("Error updating database")  
              ) {
          String code_url = jsonObject.getString("content");
          result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
          //if (handlerUtil.isWapOrApp(channelWrapper)) {
          //    result.put(JUMPURL, code_url);
          //}else{
          //    result.put(QRCONTEXT, code_url);
          //}
      }else {
          log.error("[随心付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[随心付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[随心付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}