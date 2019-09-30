package dc.pay.business.miaoshoubao;

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
 * Dec 21, 2018
 */
@RequestPayHandler("MIAOSHOUBAO")
public final class MiaoShouBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MiaoShouBaoPayRequestHandler.class);

    //字段名   变量名 是否必填    类型  说明
    //接口类型  trade_type  是   String(10)  固定值：WX或ALI
    private static final String trade_type                ="trade_type";
    //商户号   mch_id  是   String(32)  商户号，由平台分配
    private static final String mch_id                 ="mch_id";
    //订单号   trade_no    是   String(32)  商户系统内部的订单号 ,5到32个字符、 只能包含字母数字或者下划线，区分大小写，每次下单请求确保在商户系统唯一
    private static final String trade_no                ="trade_no";
    //商品名   goods_name  是   String(100) 
    private static final String goods_name                ="goods_name";
    //金额    amount  是   Int 总金额，以分为单位，不允许包含任何字、符号
    private static final String amount              ="amount";
    //异步通知地址    nortify_url 是   String(200) 异步通知地址
    private static final String notify_url              ="notify_url";
    //前台跳转地址    callback_url    是   String(200) 支付成功时前端页面跳转的URL，需给绝对路径，200字符内格式。注:该地址只作为前端页面的一个跳转，须使用notify_url通知结果作为支付最终结果。
    private static final String callback_url             ="callback_url";
    //前台跳转地址    fail_url    是   String(200) 支付超时或失败时前端页面跳转的URL，需给绝对路径，200字符内格式。注:该地址只作为前端页面的一个跳转，须使用notify_url通知结果作为支付最终结果。
    private static final String fail_url             ="fail_url";
    //签名    sign    是   String(32)  MD5签名结果
//    private static final String sign             ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(trade_no,channelWrapper.getAPI_ORDER_ID());
                put(goods_name,"name");
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callback_url,channelWrapper.getAPI_WEB_URL());
                put(fail_url,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[秒收宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        log.debug("[秒收宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
      if (StringUtils.isBlank(resultStr)) {
          log.error("[秒收宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
          //log.error("[秒收宝]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
          //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      }
      resultStr = UnicodeUtil.unicodeToString(resultStr);
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
         log.error("[秒收宝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         throw new PayException(resultStr);
      }
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[秒收宝]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("errcode") && "0".equalsIgnoreCase(jsonObject.getString("errcode"))  && jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url"))) {
          String code_url = jsonObject.getString("pay_url");
          result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
          //if (handlerUtil.isWapOrApp(channelWrapper)) {
          //    result.put(JUMPURL, code_url);
          //}else{
          //    result.put(QRCONTEXT, code_url);
          //}
      }else {
          log.error("[秒收宝]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[秒收宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[秒收宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}