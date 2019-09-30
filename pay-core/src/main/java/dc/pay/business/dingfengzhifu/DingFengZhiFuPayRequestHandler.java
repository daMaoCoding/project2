package dc.pay.business.dingfengzhifu;

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
 * Jan 26, 2019
 */
@RequestPayHandler("DINGFENGZHIFU")
public final class DingFengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DingFengZhiFuPayRequestHandler.class);

    //参数名 参数类型 其他说明
    //appid string 固定的合作⽅唯⼀编号,合作进⼊系统时⽣成.
    private static final String appid                ="appid";
    //noise string(16-32) 随机串,噪⾳元素
    private static final String noise                ="noise";
    //request_time string(15) 商户发起请求时的 Unix毫秒时间戳
    private static final String request_time                ="request_time";
    //signature string(32) 参数签名值,详⻅签名算法
//    private static final String signature                ="signature";
    
    //参数名 类型 说明
    //platform int    选择的⽀付平台,以下值取其⼀: 1: alipay, 2: weixin    3:银联⽀付
    private static final String platform                ="platform";
    //pay_source_type int 取其⼀: 1: h5, 2: pc
    private static final String pay_source_type                 ="pay_source_type";
    //sequence string 商户内部订单编号
    private static final String sequence                ="sequence";
    //uid string    下单⽤户唯⼀标识 (务必确保是正确的⽤户id, 此参            数直接影响到成功率),⻓度不⼤于32
    private static final String uid                ="uid";
    //amount string(decimal(10,2)) 订单⾦额
    private static final String amount              ="amount";
    //parameters string 额外参数,会在异步通知时将该参数原样返回.本参    数必须进⾏ UrlEncode 之后才可以发送
    private static final String parameters              ="parameters";
    //return_url string pc端扫码⽀付成功后跳转⻚⾯(get⽅式)
    private static final String return_url             ="return_url";
    //notify_url string 异步通知。⽀付完成后主动通知商户服务器指定的    ⻚⾯(post⽅式)
    private static final String notify_url             ="notify_url";
    //ip string ⽤户下单时ip (强烈建议⽤户传递此参数)
    private static final String ip             ="ip";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[鼎峰支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[鼎峰支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(noise,  HandlerUtil.getRandomStr(16));
                put(request_time,  System.currentTimeMillis()+"");
                
                put(platform,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(pay_source_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(sequence, channelWrapper.getAPI_ORDER_ID());
                put(uid,  HandlerUtil.getRandomStr(8));
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(parameters,"name");
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[鼎峰支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(appid+"=").append(api_response_params.get(appid)).append("&");
        signSrc.append(ip+"=").append(api_response_params.get(ip)).append("&");
        signSrc.append(noise+"=").append(api_response_params.get(noise)).append("&");
        signSrc.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
        signSrc.append(parameters+"=").append(api_response_params.get(parameters)).append("&");
        signSrc.append(pay_source_type+"=").append(api_response_params.get(pay_source_type)).append("&");
        signSrc.append(platform+"=").append(api_response_params.get(platform)).append("&");
        signSrc.append(request_time+"=").append(api_response_params.get(request_time)).append("&");
        signSrc.append(return_url+"=").append(api_response_params.get(return_url)).append("&");
        signSrc.append(sequence+"=").append(api_response_params.get(sequence)).append("&");
        signSrc.append(uid+"=").append(api_response_params.get(uid)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[鼎峰支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//      String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE).trim();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      if (StringUtils.isBlank(resultStr)) {
          log.error("[鼎峰支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
          //log.error("[鼎峰支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
          //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      }
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
         log.error("[鼎峰支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         throw new PayException(resultStr);
      }
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[鼎峰支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && 
      (jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data")) && 
        (StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("qrcode")) || StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("user_pay"))))
      ){
//      if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
          String qrcode = jsonObject.getJSONObject("data").getString("qrcode");
          String user_pay = jsonObject.getJSONObject("data").getString("user_pay");
          result.put( JUMPURL, StringUtils.isNotBlank(qrcode) ? user_pay : qrcode);
      }else {
          log.error("[鼎峰支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[鼎峰支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[鼎峰支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}