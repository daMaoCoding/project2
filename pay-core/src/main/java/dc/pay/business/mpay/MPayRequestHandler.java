package dc.pay.business.mpay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * May 16, 2019
 */
@Slf4j
@RequestPayHandler("MPAY")
public final class MPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

    private static final String      pay_memberid = "pay_memberid"; //      商户账号  是  是  30  商户在支付平台的唯一标识
    private static final String      pay_orderid = "pay_orderid"; //      商户订单号  是  是  32  商户系统产生的唯一订单号
    private static final String      pay_amount = "pay_amount"; //      订单金额  是  是  30  以“元”为单位，仅允 许两位小数，必须大于   零
    private static final String      pay_applydate = "pay_applydate"; //      交易日期  是  是  14  商户系统生成的订单  日期  格式：YYYYMMDDHHMMSS
    private static final String      pay_channelCode = "pay_channelCode"; //      交易渠道  是  是
    private static final String      pay_notifyurl = "pay_notifyurl"; //      支付结果通知地址
//    private static final String      pay_md5sign = "pay_md5sign"; //      签名档
    private static final String      pay_bankcode = "pay_bankcode";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(pay_memberid,channelWrapper.getAPI_MEMBERID());
            payParam.put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(pay_applydate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
            payParam.put(pay_channelCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//            payParam.put(pay_bankcode,"QRCODE");
            payParam.put(pay_bankcode,"");
        }
        log.debug("[mpay]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("pay_memberid^%s&pay_orderid^%s&pay_amount^%s&pay_applydate^%s&pay_channelCode^%s&pay_notifyurl^%s&key=%s",
                params.get(pay_memberid),
                params.get(pay_orderid),
                params.get(pay_amount),
                params.get(pay_applydate),
                params.get(pay_channelCode),
                params.get(pay_notifyurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[mpay]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();

//      if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
      if (true) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
          payResultList.add(result);
      }
      
//      else{
///*              
//          HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
//          String qrContent=null;
//          if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
//              HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
//              if(payUrlInput!=null ){
//                  String qrContentSrc = payUrlInput.getValueAttribute();
//                  if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
//              }
//          }
//         if(StringUtils.isNotBlank(qrContent)){
//              result.put(QRCONTEXT, qrContent);
//              payResultList.add(result);
//          }else {  throw new PayException(endHtml.asXml()); }
//          
//*/              
//          
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//          
//          if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
//              result.put(HTMLCONTEXT,resultStr);
//              payResultList.add(result);
//          }else if(StringUtils.isNotBlank(resultStr) ){
//              JSONObject jsonResultStr = JSON.parseObject(resultStr);
//              if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "0".equalsIgnoreCase(jsonResultStr.getString("code"))
//                      && jsonResultStr.containsKey("qrcode") && StringUtils.isNotBlank(jsonResultStr.getString("qrcode"))){
//                  if( HandlerUtil.isWapOrApp(channelWrapper)  ){
//                      result.put(JUMPURL, jsonResultStr.getString("qrcode"));
//                  }else{
//                      result.put(QRCONTEXT, jsonResultStr.getString("qrcode"));
//                  }
//                  payResultList.add(result);
//              }else {throw new PayException(resultStr); }
//          }else{ throw new PayException(EMPTYRESPONSE);}
//           
//      }
  
        log.debug("[mpay]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[mpay]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}