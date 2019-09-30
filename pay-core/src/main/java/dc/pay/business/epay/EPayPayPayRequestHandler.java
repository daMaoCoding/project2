package dc.pay.business.epay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 24, 2019
 */
@RequestPayHandler("E-PAY")
//@RequestPayHandler("EPAY1")
public final class EPayPayPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(EPayPayPayRequestHandler.class);

    //易参数
    //序号  域名  变量名 数据格式    出现要求    域说明
    //基本信息    
    //1   交易类型编号  transTypeNo AN1..10 M   详见附录
    private static final String transTypeNo                ="transTypeNo";
    //2   签名模式    signMode    AN1..10 M   签名模式1：01；签名模式2:02（上送01或者02）
    private static final String signMode                ="signMode";
    //3   签名  signature   ANS1..1024  M   填写对报文摘要的签名
//    private static final String signature                ="signature";
    //商户信息                    
    //4   商户编码    merchantNum N20 M   已被批准加入平台的商户会分配该编码
    private static final String merchantNum                ="merchantNum";
    //5   后台通知地址  backUrl ANS1..256   M   后台返回商户结果时使用
    private static final String backUrl                ="backUrl";
    //订单信息                    
    //6   订单号 orderId AN8..40 M   商户订单号，不应含“-”或“_
    private static final String orderId                ="orderId";
    //7   交易金额    txnAmt  N1..12  M   单位为分，不能带小数点，样例：1元送100
    private static final String txnAmt                ="txnAmt";
    //8   订单发送时间  txnTime YYYYMMDDHHmmss  M   必须使用当前北京时间（年年年年月月日日时时分分秒秒）24小时制，样例：20151123152540，北京时间 商户发送交易时间
    private static final String txnTime                ="txnTime";
    //9   请求方自定义域 reqReserved ANS1..1024  O   商户自定义保留域，交易应答时会原样返回
    private static final String reqReserved                ="reqReserved";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(transTypeNo,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(signMode, "01");
                put(merchantNum, channelWrapper.getAPI_MEMBERID());
                put(backUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(orderId,channelWrapper.getAPI_ORDER_ID());
                put(txnAmt, channelWrapper.getAPI_AMOUNT());
                put(txnTime, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(reqReserved, channelWrapper.getAPI_MEMBERID());
            }
        };
        log.debug("[E-PAY]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }
    
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(transTypeNo));
        signSrc.append(api_response_params.get(merchantNum));
        signSrc.append(api_response_params.get(orderId));
        signSrc.append(api_response_params.get(txnTime));
        signSrc.append(channelWrapper.getAPI_KEY());
        signSrc.append(api_response_params.get(txnAmt));
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[E-PAY]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//      String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
          if (StringUtils.isBlank(resultStr)) {
              log.error("[E-PAY]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          if (!resultStr.contains("{") || !resultStr.contains("}")) {
              log.error("[E-PAY]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          JSONObject jsonObject = null;
          try {
              jsonObject = JSONObject.parseObject(resultStr);
          } catch (Exception e) {
              log.error("[E-PAY]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
              //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(e.getMessage(),e);
          }          
          //只取正确的值，其他情况抛出异常
          if (null != jsonObject && jsonObject.containsKey("respCode") && "66".equalsIgnoreCase(jsonObject.getString("respCode"))  && jsonObject.containsKey("payInfo") && StringUtils.isNotBlank(jsonObject.getString("payInfo"))) {
               String string = jsonObject.getString("payInfo");
//               result.put(handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL, string);
               result.put(JUMPURL, string);
          }else {
              log.error("[E-PAY]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[E-PAY]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[E-PAY]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}