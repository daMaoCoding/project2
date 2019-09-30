package dc.pay.business.xinbibaozhifu;

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

import dc.pay.base.processor.ChannelWrapper;
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
 * Feb 23, 2019
 */
@RequestPayHandler("XINBIBAOZHIFU")
public final class XinBiBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinBiBaoZhiFuPayRequestHandler.class);

//    请求地址：http://ip:port/api/{MerCode}/coin/AddUser
    private static final String URL                ="http://opoutox.gosafepp.com";
    
    //coin/AddUser
    //MerCode Y String 商户号
    private static final String MerCode                ="MerCode";
    //Timestamp N Int UNIX 时间戳
    private static final String Timestamp                 ="Timestamp";
    //UserName Y String 会员名称(大小写英文字母和阿拉伯数字，长度 2-16)
    private static final String UserName                ="UserName";
    //Key Y String 验证码(需全小写)，組成方式如下:Key=A+B+C(验证码    組合方式)    A= 无意义字串长度 X 位(新币宝支付后台可配置)    B=MD5(MerCode + UserName + KeyB + YYYYMMDD)    C=无意义字串长度 X 位(新币宝支付后台可配置)    YYYYMMDD 为北京时间(GMT+8)(20150320)
    private static final String Key                ="Key";
    
    //coin/GetAddre
    //UserType Y Int 类型 1 会员，2 
    private static final String UserType                ="UserType";
    //CoinCode Y String 币种代码 详见附录 5.2
    private static final String CoinCode                ="CoinCode";
    
    //coin/Log
    //Type Y Int 请求类型 0-查看订单，1-买币，2-
    private static final String Type                ="Type";
    //Coin N String 币种 如：BCB,DC,US
    private static final String Coin                ="Coin";
    //Amount N String 卖/买币数量,如果指定，则资金托管将根据数量进行限
    private static final String Amount                ="Amount";
    //OrderNum Y String 商户定义的唯一订单编号,长度不超过 32 位，OTC 订    单变动通知，以 OrderNum 为准
    private static final String OrderNum                ="OrderNum";
    //PayMethods N String 商户限制的支付方式，多个用英文逗号隔开，取值范    围 :bankcard: 银行卡 ,aliPay: 支付宝 ,weChatpay: 微    信,payPal:PayPal
    private static final String PayMethods                ="PayMethods";
    
    private static final String param                ="param";
    private static final String A                ="A";
    private static final String B                ="B";
    private static final String C                ="C";
    
//    private static final String key        ="key";

    private static final String FLAG        ="XINBIBAOZHIFU";
    
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[新币宝支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&keyB" );
            throw new PayException("[新币宝支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&keyB" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerCode , channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(Timestamp ,System.currentTimeMillis()+"");
                put(UserName ,  HandlerUtil.getRandomStr(6));
                put(A ,  handlerUtil.getRandomStr(6));
                put(B ,  channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(C ,  handlerUtil.getRandomStr(4));
                put(PayMethods ,  channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[新币宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map params) throws PayException {
//        StringBuffer signSrc= new StringBuffer();
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
//        String paramsStr = signSrc.toString();
        String signMd5="在sendRequestGetResult()请求里生成";
        try {
//            signMd5 = RsaUtil.signByPrivateKey(paramsStr,channelWrapper.getAPI_KEY());    // 签名
        } catch (Exception e) {
            log.error("[新币宝支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        //String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新币宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        HashMap<String, String> result = Maps.newHashMap();
        try {
            
            JSONObject addUser_my = addUser(channelWrapper,payParam);
            JSONObject address = getAddress(channelWrapper,payParam);
            
            JSONObject jsonObject = log(channelWrapper,payParam);
//            {"Success":true,"Code":1,"Message":"操作成功","Data":{"Url":"https://api.bbaoapi.com/funds/login","Token":"token20181222150954506pMgihRH"}}
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("Success") && "true".equalsIgnoreCase(jsonObject.getString("Success"))  &&
                    jsonObject.containsKey("Code") && "1".equalsIgnoreCase(jsonObject.getString("Code"))  &&
                    jsonObject.containsKey("Data") && StringUtils.isNotBlank(jsonObject.getString("Data"))) {
                JSONObject jsonObject2 = jsonObject.getJSONObject("Data");
                if (null != jsonObject2 && 
                        jsonObject2.containsKey("Url") && StringUtils.isNotBlank(jsonObject2.getString("Url")) &&
                        jsonObject2.containsKey("Token") && StringUtils.isNotBlank(jsonObject2.getString("Token"))) {
                    String url = jsonObject2.getString("Url")+"/"+jsonObject2.getString("Token");
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, url);
                    result.put( JUMPURL, url);
                }
            }else {
                log.error("[新币宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(jsonObject) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.toJSONString(jsonObject));
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            log.error("[新币宝支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + e1.getMessage() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(e1);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新币宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新币宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    protected JSONObject addUser(ChannelWrapper channelWrapper, Map<String, String> myparam) throws Exception {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerCode , myparam.get(MerCode));
                put(Timestamp ,myparam.get(Timestamp));
                put(UserName ,  myparam.get(UserName));
            }
        };
        StringBuffer encryptStr = new StringBuffer();
        encryptStr.append(MerCode+"=").append(payParam.get(MerCode)).append("&");
        encryptStr.append(UserName+"=").append(payParam.get(UserName)).append("&");
        encryptStr.append(Timestamp+"=").append(payParam.get(Timestamp));
        DESUtil desUtil = new DESUtil(channelWrapper.getAPI_KEY());
        String encrypt = null;
        try {
            encrypt = desUtil.encrypt(encryptStr.toString());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[新币宝支付]-[请求支付]-addUser().1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(payParam) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(payParam));
        }
//      Key Y String 验证码(需全小写)，組成方式如下:Key=A+B+C(验证码組合方式)     A= 无意义字串长度 X 位(新币宝支付后台可配置)        B=MD5(MerCode + UserName + KeyB + YYYYMMDD)       C=无意义字串长度 X 位(新币宝支付后台可配置)         YYYYMMDD 为北京时间(GMT+8)(20150320)
//     请求地址：http://ip:port/api/{MerCode}/coin/AddUser
//      虚拟币密钥：keyA=6,keyB=1EZymvxeb3,keyC=4
        StringBuffer md5Str = new StringBuffer();
        md5Str.append(payParam.get(MerCode));
        md5Str.append(payParam.get(UserName));
        md5Str.append(myparam.get(B));
        md5Str.append(DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
        String B = HandlerUtil.getMD5UpperCase(md5Str.toString());
        Map<String, String> map = new TreeMap<String, String>() ;
        map.put(param , encrypt);
        map.put(Key ,myparam.get(A)+B+myparam.get(C));
        String addUserUrl = URL+"/api/"+myparam.get(MerCode)+"/coin/AddUser";
        String resultStr = RestTemplateUtil.postJson(addUserUrl, map);
        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[新币宝支付]-[请求支付]-addUser().2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[新币宝支付]-[请求支付]-addUser().3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //JSONObject jsonObject = JSONObject.parseObject(resultStr);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[新币宝支付]-[请求支付]-addUser().4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        return jsonObject;
    }
    
    protected JSONObject  getAddress(ChannelWrapper channelWrapper, Map<String, String> myparam) throws Exception {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerCode , myparam.get(MerCode));
                put(Timestamp ,myparam.get(Timestamp));
                put(UserType , "1");
                put(UserName ,  myparam.get(UserName));
                put(CoinCode  , "DC");
            }
        };
     // 组装参数
        StringBuffer encryptStr = new StringBuffer();
        encryptStr.append(MerCode+"=").append(payParam.get(MerCode)).append("&");
        encryptStr.append(UserType+"=").append(payParam.get(UserType)).append("&");
        encryptStr.append(UserName+"=").append(payParam.get(UserName)).append("&");
        encryptStr.append(CoinCode+"=").append(payParam.get(CoinCode)).append("&");
        encryptStr.append(Timestamp+"=").append(payParam.get(Timestamp));
        DESUtil desUtil = new DESUtil(channelWrapper.getAPI_KEY());
        String encrypt = null;
        try {
            encrypt = desUtil.encrypt(encryptStr.toString());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[新币宝支付]-[请求支付]-getAddress().1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(payParam) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(payParam));
        }
        StringBuffer md5Str = new StringBuffer();
        md5Str.append(payParam.get(MerCode));
        md5Str.append(payParam.get(UserType));
        md5Str.append(payParam.get(CoinCode));
        md5Str.append(myparam.get(B));
        md5Str.append(DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
        String B = HandlerUtil.getMD5UpperCase(md5Str.toString());
        Map<String, String> map = new TreeMap<String, String>() ;
        map.put(param , encrypt);
        map.put(Key ,myparam.get(A)+B+myparam.get(C));
        String getAddressUrl = URL+"/api/"+myparam.get(MerCode)+"/coin/GetAddress";
        String resultStr = RestTemplateUtil.postJson(getAddressUrl, map);
        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[新币宝支付]-[请求支付]-getAddress().2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[新币宝支付]-[请求支付]-getAddress().3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //JSONObject jsonObject = JSONObject.parseObject(resultStr);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[新币宝支付]-[请求支付]-getAddress().4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        return jsonObject;
    }
    
    protected JSONObject  log(ChannelWrapper channelWrapper, Map<String, String> myparam) throws Exception {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(MerCode , myparam.get(MerCode));
              put(Timestamp ,myparam.get(Timestamp));
              put(UserName ,  myparam.get(UserName));
              put(Type , "1");
              put(Coin , "DC");
              put(Amount , HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(OrderNum  , channelWrapper.getAPI_ORDER_ID());
              put(PayMethods , channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          }
      };
      StringBuffer encryptStr = new StringBuffer();
      encryptStr.append(MerCode+"=").append(payParam.get(MerCode)).append("&");
      encryptStr.append(OrderNum+"=").append(payParam.get(OrderNum)).append("&");
      encryptStr.append(UserName+"=").append(payParam.get(UserName)).append("&");
      encryptStr.append(Coin+"=").append(payParam.get(Coin)).append("&");
      encryptStr.append(Type+"=").append(payParam.get(Type)).append("&");
      encryptStr.append(PayMethods+"=").append(payParam.get(PayMethods)).append("&");
      encryptStr.append(Amount+"=").append(payParam.get(Amount)).append("&");
      encryptStr.append(Timestamp+"=").append(payParam.get(Timestamp));
      DESUtil desUtil = new DESUtil(channelWrapper.getAPI_KEY());
      String encrypt = null;
      try {
          encrypt = desUtil.encrypt(encryptStr.toString());
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[新币宝支付]-[请求支付]-log().1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(payParam) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(JSON.toJSONString(payParam));
      }
      StringBuffer md5Str = new StringBuffer();
      md5Str.append(payParam.get(MerCode));
      md5Str.append(payParam.get(UserName));
      md5Str.append(payParam.get(Type ));
      md5Str.append(payParam.get(OrderNum ));
      md5Str.append(myparam.get(B));
      md5Str.append(DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
//      String B = HandlerUtil.getMD5UpperCase(md5Str.toString());
      String B = HandlerUtil.getMD5UpperCase(md5Str.toString());
      Map<String, String> map = new TreeMap<String, String>() ;
      map.put(param , encrypt);
      map.put(Key ,myparam.get(A)+B+myparam.get(C));
      String addUserUrl = URL+"/api/"+myparam.get(MerCode)+"/coin/Login";
      String resultStr = RestTemplateUtil.postJson(addUserUrl, map);
      if (StringUtils.isBlank(resultStr)) {
          log.error("[新币宝支付]-[请求支付]-log().2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
         log.error("[新币宝支付]-[请求支付]-log().3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         throw new PayException(resultStr);
      }
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[新币宝支付]-[请求支付]-log().4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      return jsonObject;
  }
}