package dc.pay.business.jinbao2zhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 22, 2019
 */
@RequestPayHandler("JINBAO2ZHIFU")
public final class JinBao2ZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinBao2ZhiFuPayRequestHandler.class);

    //mchid 商户号 ① 否    由金宝商户系统分配的商户    id
    private static final String mchid            ="mchid";
    //paytype 支付类型 ② 否 支付类型，参考附录 1
    private static final String paytype            ="paytype";
    //paymoney 金额 ③ 否 单位元，2 位小数
    private static final String paymoney            ="paymoney";
    //payordid     商户订单号    ④ 否 商户系统订单号，值唯一
    private static final String payordid            ="payordid";
    //notifyaddress    异步通知    地址 ⑤ 否 异步通知接收地址
    private static final String notifyaddress            ="notifyaddress";
    //synaddress    同步通知    地址 - 是 支付完成后跳转的地址
//    private static final String synaddress            ="synaddress";
    //remark 备注消息 - 是    通知会原样返回。若包含中    文，请注意编码
//    private static final String remark            ="remark";
    //sign MD5 签名 - 否 32 位小写，GB2312 编码
//    private static final String sign            ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchid, channelWrapper.getAPI_MEMBERID());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney,  handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payordid,channelWrapper.getAPI_ORDER_ID());
                put(notifyaddress,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[金宝2支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
         //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
         StringBuffer signSrc= new StringBuffer();
         signSrc.append(mchid+"=").append(api_response_params.get(mchid)).append("&");
         signSrc.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
         signSrc.append(paymoney+"=").append(api_response_params.get(paymoney)).append("&");
         signSrc.append(payordid+"=").append(api_response_params.get(payordid)).append("&");
         signSrc.append(notifyaddress+"=").append(api_response_params.get(notifyaddress));
         signSrc.append(channelWrapper.getAPI_KEY());
         //删除最后一个字符
         //signSrc.deleteCharAt(paramsStr.length()-1);
         String paramsStr = signSrc.toString();
         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
//         String signMd5 = Sha1Util.getSha1(paramsStr).toLowerCase();
         log.debug("[金宝2支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
         return signMd5;
     }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
////          String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//          if (StringUtils.isBlank(resultStr)) {
//              log.error("[金宝2支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//          if (!resultStr.contains("{") || !resultStr.contains("}")) {
//             log.error("[金宝2支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//             throw new PayException(resultStr);
//          }
//          //JSONObject resJson = JSONObject.parseObject(resultStr);
//          JSONObject resJson;
//          try {
//              resJson = JSONObject.parseObject(resultStr);
//          } catch (Exception e) {
//              e.printStackTrace();
//              log.error("[金宝2支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//          //只取正确的值，其他情况抛出异常
//          if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("result") && StringUtils.isNotBlank(resJson.getString("result")) && 
//                  (resJson.getJSONObject("result").containsKey("qrCode") && StringUtils.isNotBlank(resJson.getJSONObject("result").getString("qrCode")))){
////          if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("result") && StringUtils.isNotBlank(resJson.getString("result"))) {
//              String code_url = resJson.getJSONObject("result").getString("qrCode");
//              if (handlerUtil.isWapOrApp(channelWrapper)) {
//                  result.put(JUMPURL, code_url);
//              }else {
//                  result.put(QRCONTEXT, code_url);
//              }
//          }else {
//              log.error("[金宝2支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//        }
        
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[金宝2支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[金宝2支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}