package dc.pay.business.shanweizhifu;

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
 * Dec 21, 2018
 */
@RequestPayHandler("SHANWEIZHIFU")
public final class ShanWeiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShanWeiZhiFuPayRequestHandler.class);

    //参数名称  变量名     类型  说明
    //版本号   version     String  默认 1.0
    private static final String version                ="version";
    //商户编号  merId   String  商户后台获取
    private static final String merId                 ="merId";
    //商户订单号     orderId     String(32)  商户唯一订单号
    private static final String orderId                ="orderId";
    //订单金额  totalMoney  String  单位:分， 必须大于 100 分
    private static final String totalMoney                ="totalMoney";
    //支付类型  tradeType   String  支付宝： alipay
    private static final String tradeType              ="tradeType";
    //请求ip  ip  String  请求IP， 部分支付请求IP与支付IP必须一致，所以请上传用户请求的真实IP
    private static final String ip              ="ip";
    //商品描述  describe    String  商品描述
    private static final String describe             ="describe";
    //    异步通知 URL    notify  String  数据异步通知(可在后台配置，后台配            置的通知地址优先)
    private static final String notify             ="notify";
    //同步跳转URL   redirectUrl     String  不能带有任何参数(某些通道无效)
    private static final String redirectUrl             ="redirectUrl";
    //订单备注说明    remark  String(64)  可为空， 如果传递必须为字符串或者    数据组合
//    private static final String remark             ="remark";
    //支付来源  fromtype    String  wap : 普通 wap    weixinwap : 微信内 wap
    private static final String fromtype             ="fromtype";
    //md5签名串    sign    String  参照签名校验规则
//    private static final String sign             ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0");
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(orderId,channelWrapper.getAPI_ORDER_ID());
                put(totalMoney,  channelWrapper.getAPI_AMOUNT());
                put(tradeType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(ip,channelWrapper.getAPI_Client_IP());
                put(describe,"1");
                put(notify,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(redirectUrl,channelWrapper.getAPI_WEB_URL());
                put(fromtype,handlerUtil.isWxSM(channelWrapper) ? "weixinwap" : "wap");
            }
        };
        log.debug("[微支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merId+"=").append(api_response_params.get(merId)).append("&");
        signSrc.append(orderId+"=").append(api_response_params.get(orderId)).append("&");
        signSrc.append(totalMoney+"=").append(api_response_params.get(totalMoney)).append("&");
        signSrc.append(tradeType+"=").append(api_response_params.get(tradeType)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[微支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
      if (StringUtils.isBlank(resultStr)) {
          log.error("[微支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
          //log.error("[微支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
          //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      }
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
         log.error("[微支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         throw new PayException(resultStr);
      }
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[微支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("object") && StringUtils.isNotBlank(jsonObject.getString("object"))) {
          JSONObject jsonObject2 = jsonObject.getJSONObject("object");
          if (null != jsonObject2 && jsonObject2.containsKey("data") && StringUtils.isNotBlank(jsonObject2.getString("data"))) {
              String code_url = jsonObject2.getString("data");
              result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
          }else {
              log.error("[微支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
      }else {
          log.error("[微支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[微支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[微支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}