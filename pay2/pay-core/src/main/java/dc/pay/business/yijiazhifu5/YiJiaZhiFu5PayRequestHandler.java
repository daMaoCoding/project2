package dc.pay.business.yijiazhifu5;

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
import dc.pay.utils.Sha1Util;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 6, 2019
 */
@RequestPayHandler("YIJIAZHIFU5")
public final class YiJiaZhiFu5PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiJiaZhiFu5PayRequestHandler.class);

    //参数编码    参数名称    是否必填    字段类型
    //merchant    商户号，由平台分配   Y   string
    private static final String merchant            ="merchant";
    //Amount  金额，单位为分 Y   int
    private static final String Amount            ="amount";
    //pay_type    支付产品类型  Y   详见本文最下方的支付产品类型列表
    private static final String pay_type            ="pay_type";
    //order_no    商户订单号   Y   String
    private static final String order_no            ="order_no";
    //order_time  下单时间，Unix时间戳秒   Y   long
    private static final String order_time            ="order_time";
    //subject 商品描述    Y   string
    private static final String subject            ="subject";
    //notify_url  异步回调地址  Y   string
    private static final String notify_url            ="notify_url";
    //callback_url    同步回调地址  Y   string（必须参与SHA1运算，才能提交此参数，否则会导致验签失败）如果同步回调地址为空，则不需要提交此参数，如果填写就必须参与计算签名
    private static final String callback_url            ="callback_url";
    //sign    签名  Y   String（提交通知验签时候，用小写提交）
//    private static final String sign            ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[一加支付5]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[一加支付5]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//                put(merchant, channelWrapper.getAPI_MEMBERID());
                put(merchant, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(Amount,  channelWrapper.getAPI_AMOUNT());
//                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_type,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_time, System.currentTimeMillis()+"");
                put(subject,"name");
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callback_url,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[一加支付5]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
         //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
         StringBuffer signSrc= new StringBuffer();
         signSrc.append(Amount+"=").append(api_response_params.get(Amount)).append("&");
         signSrc.append( callback_url+"=").append(api_response_params.get( callback_url)).append("&");
         signSrc.append(merchant+"=").append(api_response_params.get(merchant)).append("&");
         signSrc.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
         signSrc.append(order_no+"=").append(api_response_params.get(order_no)).append("&");
         signSrc.append(order_time+"=").append(api_response_params.get(order_time)).append("&");
         signSrc.append(pay_type+"=").append(api_response_params.get(pay_type)).append("&");
         signSrc.append(subject+"=").append(api_response_params.get(subject)).append("&");
         signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
         //删除最后一个字符
         //signSrc.deleteCharAt(paramsStr.length()-1);
         String paramsStr = signSrc.toString();
//         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
         String signMd5 = Sha1Util.getSha1(paramsStr).toLowerCase();
         log.debug("[一加支付5]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
         return signMd5;
     }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//          String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
          if (StringUtils.isBlank(resultStr)) {
              log.error("[一加支付5]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          if (!resultStr.contains("{") || !resultStr.contains("}")) {
             log.error("[一加支付5]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
          }
          //JSONObject resJson = JSONObject.parseObject(resultStr);
          JSONObject resJson;
          try {
              resJson = JSONObject.parseObject(resultStr);
          } catch (Exception e) {
              e.printStackTrace();
              log.error("[一加支付5]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          //只取正确的值，其他情况抛出异常
          if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("result") && StringUtils.isNotBlank(resJson.getString("result")) && 
                  (resJson.getJSONObject("result").containsKey("qrCode") && StringUtils.isNotBlank(resJson.getJSONObject("result").getString("qrCode")))){
//          if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("result") && StringUtils.isNotBlank(resJson.getString("result"))) {
              String code_url = resJson.getJSONObject("result").getString("qrCode");
              if (handlerUtil.isWapOrApp(channelWrapper)) {
                  result.put(JUMPURL, code_url);
              }else {
                  result.put(QRCONTEXT, code_url);
              }
          }else {
              log.error("[一加支付5]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
        }
        
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[一加支付5]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[一加支付5]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}