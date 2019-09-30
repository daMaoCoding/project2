package dc.pay.business.shanghaizhifu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 22, 2019
 */
@RequestPayHandler("SHANGHAIZHIFU")
public final class ShangHaiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShangHaiZhiFuPayRequestHandler.class);

    //请求参数
    //字段名称    字段名 是否必填    类型  说明
    //merchNo 商户号 是   String  
    private static final String merchNo                ="merchNo";
    //return_url  同步跳转地址  是   String  
    private static final String return_url                ="return_url";
    //outTradeNo  订单号（只能是数字，不能包含字母）   是   String  订单号：数字类型（时间戳）
    private static final String outTradeNo                ="outTradeNo";
    //totalFee    交易金额(分) 是   String  
    private static final String totalFee                ="totalFee";
    //notifyUrl   支付通知地址  是   String  
    private static final String notifyUrl                ="notifyUrl";
    //nonceStr    随机字符    是   String  
    private static final String nonceStr                ="nonceStr";
    //subject 商品名称    是   String  需做BASE64转码
    private static final String subject                ="subject";
    //userid  终端唯一标识  是   String  终端的用户唯一标识，不同的下游客户最好加上能区分自己平台的前缀编码，方便我方区分处理，参与签名    例如  终端用户IP
    private static final String userid                ="userid";
    //paytype 支付方式    是   String  1:支付宝  2:微信
    private static final String paytype                ="paytype";
    //sign    签名  是   String  
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[商海支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：密钥-商户号" );
            throw new PayException("[商海支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：密钥-商户号" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchNo, channelWrapper.getAPI_MEMBERID());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                put(totalFee, channelWrapper.getAPI_AMOUNT());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(nonceStr,ShangHaiZhiFuPayRequestHandler.this.handlerUtil.getRandomStr(6));
//                put(subject,new Base64().getEncoder().encode("name".getBytes()));
                put(subject,new String(Base64.getEncoder().encode("name".getBytes())));
//                put(userid,ShangHaiZhiFuPayRequestHandler.this.handlerUtil.getRandomStr(8));
                put(userid,channelWrapper.getAPI_Client_IP());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[商海支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merchNo+"=").append(api_response_params.get(merchNo)).append("&");
        signSrc.append(subject+"=").append(api_response_params.get(subject)).append("&");
        signSrc.append(outTradeNo+"=").append(api_response_params.get(outTradeNo)).append("&");
        signSrc.append(totalFee+"=").append(api_response_params.get(totalFee)).append("&");
        signSrc.append(notifyUrl+"=").append(api_response_params.get(notifyUrl)).append("&");
        signSrc.append(return_url+"=").append(api_response_params.get(return_url)).append("&");
        signSrc.append(nonceStr+"=").append(api_response_params.get(nonceStr)).append("&");
        signSrc.append(userid+"=").append(api_response_params.get(userid)).append("&");
        signSrc.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY().split("-")[0]);
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[商海支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
      String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
      if (StringUtils.isBlank(resultStr)) {
          log.error("[商海支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
          //log.error("[商海支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
          //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      }
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
         log.error("[商海支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         throw new PayException(resultStr);
      }
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[商海支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
      //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
      // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
      //){
      
//      if (null != jsonObject && jsonObject.containsKey("CODE") && "00".equalsIgnoreCase(jsonObject.getString("CODE"))  && jsonObject.containsKey("payurl") && StringUtils.isNotBlank(jsonObject.getString("payurl"))) {
//          String code_url = jsonObject.getString("payurl");
//          result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//          //if (handlerUtil.isWapOrApp(channelWrapper)) {
//          //    result.put(JUMPURL, code_url);
//          //}else{
//          //    result.put(QRCONTEXT, code_url);
//          //}
//      }else {
//          log.error("[商海支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//          throw new PayException(resultStr);
//      }
      
      if (null != jsonObject && jsonObject.containsKey("CODE") && "00".equalsIgnoreCase(jsonObject.getString("CODE"))  && jsonObject.containsKey("weburl") && StringUtils.isNotBlank(jsonObject.getString("weburl"))) {
          String code_url = jsonObject.getString("weburl");
          result.put( JUMPURL, code_url);
          //if (handlerUtil.isWapOrApp(channelWrapper)) {
          //    result.put(JUMPURL, code_url);
          //}else{
          //    result.put(QRCONTEXT, code_url);
          //}
      }else {
          log.error("[商海支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[商海支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[商海支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}