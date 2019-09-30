package dc.pay.business.xiduoduozhifu;

import java.sql.Timestamp;
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
import dc.pay.utils.DateUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 27, 2019
 */
@RequestPayHandler("XIDUODUOZHIFU")
public final class XiDuoDuoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XiDuoDuoZhiFuPayRequestHandler.class);

    //请求参数(以下参数是data的数据参数列表)：
    //名称  必填  类型  说明
    //merAccount  是   String(32)  商户标识，由系统随机生成
    private static final String merAccount                ="merAccount";
    //customerNo  是   String(30)  用户编号，由系统生成
    private static final String customerNo                ="customerNo";
    //payType 是   String(20)  支付方式，银联：UNIONPAY，微信：WEIXIN，    QQ钱包：QQPAY，    支付宝：ALIPAY
    private static final String payType                ="payType";
    //payTypeCode 是   String(30)  支付类型，参考支付类型表
    private static final String payTypeCode                ="payTypeCode";
    //orderNo 是   String(32)  商户订单号，由商户自行生成，必须唯一
    private static final String orderNo                ="orderNo";
    //time    是   long    时间戳，例如：1510109185，精确到秒，前后误差不超过5分钟
    private static final String time                ="time";
    //payAmount   是   int 支付金额，单位分，必须大于0
    private static final String payAmount                ="payAmount";
    //productCode 是   String(10)  商品类别码，固定值01
    private static final String productCode                ="productCode";
    //productName 是   String(50)  商品名称
    private static final String productName                ="productName";
    //productDesc 否   String(200) 商品描述
//    private static final String productDesc                ="productDesc";
    //userType    是   int 用户类型，固定值0
    private static final String userType                ="userType";
    //payIp   是   String(20)  用户IP地址
    private static final String payIp                ="payIp";
    //returnUrl   否   String(255) 页面通知地址
    private static final String returnUrl                ="returnUrl";
    //notifyUrl   否   String(255) 异步通知地址
    private static final String notifyUrl                ="notifyUrl";
    //sign    是   String(200) 签名信息，商户使用SHA1对sign以外的其他参数进行字母排序后串成字符串进行签名，如sign=SHA1(amount+…+merKey)
//    private static final String sign                 ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[喜多多支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户标识 merAccount&商户编号customerNo" );
            throw new PayException("[喜多多支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户标识 merAccount&商户编号customerNo" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merAccount, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(customerNo, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(payTypeCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
//                put(time,System.currentTimeMillis()+"");
                put(time,StringToTimestamp(DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss")).toString());
                put(payAmount,  channelWrapper.getAPI_AMOUNT());
                put(productCode,"01");
                put(productName,"name");
                put(userType,"0");
                put(payIp,  channelWrapper.getAPI_Client_IP());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[喜多多支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
//                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
                signSrc.append(api_response_params.get(paramKeys.get(i)));
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
//        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        String signMd5 = Sha1Util.getSha1(paramsStr);
        log.debug("[喜多多支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        Map<String, String> map = new TreeMap<>();
        map.put(merAccount, payParam.get(merAccount));
        map.put("data", JSON.toJSONString(payParam));
        
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merAccount+"=").append(payParam.get(merAccount)).append("&");
        signSrc.append("data=").append(JSON.toJSONString(payParam)).append("&");
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL()+"?"+signSrc.toString(), JSON.toJSONString(map),MediaType.APPLICATION_JSON_VALUE).trim();
//      String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL()+"?"+param, map, String.class, HttpMethod.GET);
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL()+"?"+signSrc.toString(), map,"UTF-8");
      //if (StringUtils.isBlank(resultStr)) {
      //    log.error("[喜多多支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //    throw new PayException(resultStr);
      //    //log.error("[喜多多支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
      //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      //}
      //if (!resultStr.contains("{") || !resultStr.contains("}")) {
      //   log.error("[喜多多支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //   throw new PayException(resultStr);
      //}
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[喜多多支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("code") && "000000".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))  && 
      (jsonObject.getJSONObject("data").containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("payUrl")) || jsonObject.getJSONObject("data").containsKey("qrCode") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("qrCode")))){
//      if (null != jsonObject && jsonObject.containsKey("code") && "000000".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
          String code_url = jsonObject.getJSONObject("data").getString("payUrl");
//          result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
          result.put( JUMPURL, code_url);
      }else {
          log.error("[喜多多支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[喜多多支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[喜多多支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    /**
     * String(yyyy-MM-dd HH:mm:ss)转10位时间戳
     * @param time
     * @return
     */
    public static Integer StringToTimestamp(String time){
        int times = 0;
        try {  
            times = (int) ((Timestamp.valueOf(time).getTime())/1000);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        if(times==0){
            System.out.println("String转10位时间戳失败");
        }
        return times; 
        
    }
}