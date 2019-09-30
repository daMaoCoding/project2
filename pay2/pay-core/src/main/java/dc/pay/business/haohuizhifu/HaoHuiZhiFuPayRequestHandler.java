package dc.pay.business.haohuizhifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("HAOHUIZHIFU")
public final class HaoHuiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HaoHuiZhiFuPayRequestHandler.class);

     private static final String     account = "account";  //   商户号 String 必填 商户签约时，⽀付平台分配的唯⼀商户号。
     private static final String     payType = "payType";  //   ⽀付类型 String 必填 固定值：alipay。
     private static final String     payMoney = "payMoney";  //   ⽀付⾦额 Number 必填 ⽀付请求⾦额 ( 限整数 )。
     private static final String     ip = "ip";  //   客户端IP String 必填 消费者创建交易时所使⽤机器的IP 或者终端ip，最⼤⻓度为15个字符。 举例：192.168.1.25
     private static final String     notifyURL = "notifyURL";  //   回调位址 String ⾮必填 异步通知商⼾回調位址，⽤以通知結果。
     private static final String     orderNo = "orderNo";  //   订单号 String ⾮必填 商户系统⾥的订单号
     private static final String     sign = "sign";  //   订单号 String ⾮必填 商户系统⾥的订单号



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_KEY().contains("&")){
            throw new PayException("密钥(私钥)格式错误，正确格式请使用&符号链接【私钥】和【Personal Access Token】,如：私钥&Personal Access Token");
        }

        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(account,channelWrapper.getAPI_MEMBERID());
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(payMoney,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(ip,channelWrapper.getAPI_Client_IP());
            payParam.put(notifyURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
        }
        log.debug("[豪汇支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i)))    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY().split("&")[0]);
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[豪汇支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      //  payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper)  &&  HandlerUtil.isYLKJ(channelWrapper)   &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
/*				
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getValueAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
				
*/


                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization","Bearer ".concat(channelWrapper.getAPI_KEY().split("&")[1]));
                headers.add("Content-Type","application/x-www-form-urlencoded");


				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,headers);
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("retCode") && "0".equalsIgnoreCase(jsonResultStr.getString("retCode")) && jsonResultStr.containsKey("redirectURL")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("redirectURL"))){
                                result.put(JUMPURL, jsonResultStr.getString("redirectURL"));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(UnicodeUtil.unicodeToString(resultStr));
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[豪汇支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[豪汇支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[豪汇支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}