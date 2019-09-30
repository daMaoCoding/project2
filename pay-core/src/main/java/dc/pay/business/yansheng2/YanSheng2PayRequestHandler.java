package dc.pay.business.yansheng2;

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
 * Apr 18, 2019
 */
@RequestPayHandler("YANSHENG2")
public final class YanSheng2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YanSheng2PayRequestHandler.class);

    //参数  参数值 是否必填    参数说明
    //user    商户号 是   -
    private static final String user                ="user";
    //money   金额  是   保留2位小数，单位元
    private static final String money                ="money";
    //osn 商户源单号   否   不为空时，则系统在发送订单通知时会将此源单号一并发送回去
    private static final String osn                ="osn";
    //sign    签名  否   请看签名方法
//    private static final String sign                ="sign";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String sign  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[衍生2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[衍生2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(user, channelWrapper.getAPI_MEMBERID());
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(osn,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[衍生2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(user ));
        signSrc.append(api_response_params.get(money));
        signSrc.append(api_response_params.get(osn));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[衍生2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();

//      String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      //if (StringUtils.isBlank(resultStr)) {
      //    log.error("[衍生2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //    throw new PayException(resultStr);
      //    //log.error("[衍生2]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
      //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      //}
//      System.out.println("请求返回=========>"+resultStr);
      //if (!resultStr.contains("{") || !resultStr.contains("}")) {
      //   log.error("[衍生2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      //   throw new PayException(resultStr);
      //}
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[衍生2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
//      if (null != jsonObject && jsonObject.containsKey("result") && "0".equalsIgnoreCase(jsonObject.getString("result"))  && 
//      (jsonObject.containsKey("context") && StringUtils.isNotBlank(jsonObject.getString("context")) &&
//       jsonObject.getJSONObject("context").containsKey("qrcode_url") && StringUtils.isNotBlank(jsonObject.getJSONObject("context").getString("qrcode_url")))
//      ){
      if (null != jsonObject && jsonObject.containsKey("result") && "true".equalsIgnoreCase(jsonObject.getString("result"))  && jsonObject.containsKey("payurl") && StringUtils.isNotBlank(jsonObject.getString("payurl"))) {
          String code_url = jsonObject.getString("payurl");
          result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
          //if (handlerUtil.isWapOrApp(channelWrapper)) {
          //    result.put(JUMPURL, code_url);
          //}else{
          //    result.put(QRCONTEXT, code_url);
          //}
      }else {
          log.error("[衍生2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[衍生2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[衍生2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}