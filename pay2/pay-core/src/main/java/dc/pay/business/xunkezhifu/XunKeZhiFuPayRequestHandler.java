package dc.pay.business.xunkezhifu;

import java.util.ArrayList;
import java.util.Base64;
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
 * Jan 23, 2019
 */
@RequestPayHandler("XUNKEZHIFU")
public final class XunKeZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XunKeZhiFuPayRequestHandler.class);

    
    //参数名称          限制          类型          说明
    //公共参数
    //merch_id          M          Int(6)          平台分配给商户的正式商户编号
    private static final String merch_id                ="merch_id";
    //version           M          Int(2)          固定值：10
    private static final String version                ="version";
    //signtype          M          Int(1)          固定值：0
    private static final String signtype                ="signtype";
    //timestamp         M          Timestamp          当前13位时间戳
    private static final String timestamp                ="timestamp";
    //norce_str         M          String(10, 32)          随机生成字符串
    private static final String norce_str                ="norce_str";
    //sign              M          String(32)          签名内容
//    private static final String sign                ="sign";
    
    //接口参数
    //body              C          String(10...)          商品描述
//    private static final String body                ="body";
    //detail            M          String(10...)          商品说明
    private static final String detail                ="detail";
    //out_trade_no      M          String(20, 30)          商户订单编号, 商户应当保证此为唯一值
    private static final String out_trade_no                ="out_trade_no";
    //money             M          Int(1, 18)          订单金额，单位为：分
    private static final String money                ="money";
    //channel           M          Int(6)          充值渠道
    private static final String channel                ="channel";
    //attach            C          String(1, 255)          附加数据，将会原路返回
//    private static final String attach                ="attach";
    //delay             C          Int            最大等待时间(支付宝创建订单大约2-5s)
//    private static final String delay                ="delay";
    //callback_url      C          String          异步通知地址, 该域名需要到商户后台添加白名单
    private static final String callback_url                ="callback_url";
    //callfront_url     C          String          同步通知地址, 该域名需要到商户后台添加白名单
//    private static final String callfront_url                ="callfront_url";
    //ip                M          String          发起者IP地址, 请上传客户IP地址，不要上传商户服务器地址，以便于我们对成功率分析。
    private static final String ip                ="ip";
    //other             C          String          其他
//    private static final String other                ="other";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[讯科支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[讯科支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merch_id, channelWrapper.getAPI_MEMBERID());
                put(version,"10");
                put(signtype,"0");
                put(timestamp,System.currentTimeMillis()+"");
                put(norce_str,handlerUtil.getRandomStr(12));
                put(detail,"name");
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(money,  channelWrapper.getAPI_AMOUNT());
                put(channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[讯科支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        log.debug("[讯科支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      if (StringUtils.isBlank(resultStr)) {
          log.error("[讯科支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
          //log.error("[讯科支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
          //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      }
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
         log.error("[讯科支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         throw new PayException(resultStr);
      }
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[讯科支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("result") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result"))  && jsonObject.containsKey("body") && StringUtils.isNotBlank(jsonObject.getString("body"))) {
          String body = new String(Base64.getDecoder().decode(jsonObject.getString("body")));
          JSONObject jsonObject2;
          try {
              jsonObject2 = JSONObject.parseObject(body);
          } catch (Exception e) {
              e.printStackTrace();
              log.error("[讯科支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(body) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(body);
          }
          if (null != jsonObject2 && jsonObject2.containsKey("payurl") && StringUtils.isNotBlank(jsonObject2.getString("payurl"))) {
              result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject2.getString("payurl"));
          }else {
              log.error("[讯科支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
      }else {
          log.error("[讯科支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[讯科支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[讯科支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}