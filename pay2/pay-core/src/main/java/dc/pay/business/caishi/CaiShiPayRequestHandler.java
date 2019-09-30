package dc.pay.business.caishi;

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
 *  和NEWCAISHI一样
 * @author andrew
 * Feb 27, 2018
 */
@RequestPayHandler("CAISHI")
public final class CaiShiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CaiShiPayRequestHandler.class);
    
    //外部订单号     outOid      String  必填      是   
    private static final String outOid  ="outOid";
    //平台商户编号    merchantCode        String  必填      是   
    private static final String merchantCode  ="merchantCode";
    //平台集团商户编号  mgroupCode  String  必填      是   
    private static final String mgroupCode  ="mgroupCode";
    //支付类型      payType     String  必填      是       参考附录支付类型
    private static final String payType  ="payType";
    //商品名称      goodName    String  必填      是       
    private static final String goodName  ="goodName";
    //商品数量      goodNum     String  非必填     非空是     
//    private static final String goodNum  ="goodNum";
    //业务类型      busType     String  非必填     非空是             
//    private static final String busType  ="busType";
    //回调地址      notifyUrl   String  必填      否       不参与签名
    private static final String notifyUrl  ="notifyUrl";
    private static final String pageUrl  ="pageUrl";
    //支付金额  tranAmount  String  必填  是   单位：分
    private static final String tranAmount  ="tranAmount";
    //银行编号  bankCode    String  选填  是   网银必填 例如：ABC
    private static final String bankCode  ="bankCode";
    private static final String key  ="key";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == api_MEMBERID || !api_MEMBERID.contains("&") || api_MEMBERID.split("&").length != 2) {
            log.error("[彩世]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantCode&平台号mgroupCode" );
            throw new PayException("[彩世]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantCode&平台号mgroupCode" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(outOid, channelWrapper.getAPI_ORDER_ID());
                put(merchantCode, api_MEMBERID.split("&")[0]);
                put(mgroupCode, api_MEMBERID.split("&")[1]);
                if (HandlerUtil.isWY(channelWrapper)) {
                    put(payType,"31");
                    put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                    put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(tranAmount,  channelWrapper.getAPI_AMOUNT());
                put(goodName,"goodName");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pageUrl,channelWrapper.getAPI_WEB_URL());
                
            }
        };
        log.debug("[彩世]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

//    protected String buildPaySign(Map api_response_params) throws PayException {
//        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
//        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
//        StringBuffer signSrc= new StringBuffer();
//        if (HandlerUtil.isWY(channelWrapper)) {
//            signSrc.append(bankCode+"=").append(api_response_params.get(bankCode)).append("&");
//            signSrc.append(cardType+"=").append(api_response_params.get(cardType)).append("&");
//            signSrc.append(goodsDesc+"=").append(api_response_params.get(goodsDesc)).append("&");
//            signSrc.append(goodsName+"=").append(api_response_params.get(goodsName)).append("&");
//        }else {
//            signSrc.append(goodName+"=").append(api_response_params.get(goodName)).append("&");
//        }
//        signSrc.append(merchantCode+"=").append(api_response_params.get(merchantCode)).append("&");
//        signSrc.append(mgroupCode+"=").append(api_response_params.get(mgroupCode)).append("&");
//        signSrc.append(outOid+"=").append(api_response_params.get(outOid)).append("&");
//        if (HandlerUtil.isWY(channelWrapper)) {
//            signSrc.append(terminalType+"=").append(api_response_params.get(terminalType)).append("&");
//            signSrc.append(transAmount+"=").append(api_response_params.get(transAmount)).append("&");
//            signSrc.append(userType+"=").append(api_response_params.get(userType)).append("&");
//        }else {
//            signSrc.append(payAmount+"=").append(api_response_params.get(payAmount)).append("&");
//            signSrc.append(payType+"=").append(api_response_params.get(payType)).append("&");
//        }
//        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
//        String paramsStr = signSrc.toString();
//        System.out.println("签名源串=========>"+paramsStr);
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
//        log.debug("[彩世]-[请求支付]-2.生成加密URL签名完成：" + signMd5);
//        return signMd5;
//    }
    protected String buildPaySign(Map api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(goodName+"=").append(api_response_params.get(goodName)).append("&");
        signSrc.append(merchantCode+"=").append(api_response_params.get(merchantCode)).append("&");
        signSrc.append(mgroupCode+"=").append(api_response_params.get(mgroupCode)).append("&");
        signSrc.append(outOid+"=").append(api_response_params.get(outOid)).append("&");
        signSrc.append(payType+"=").append(api_response_params.get(payType)).append("&");
        signSrc.append(tranAmount+"=").append(api_response_params.get(tranAmount)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[彩世]-[请求支付]-2.生成加密URL签名完成：" + signMd5);
        return signMd5;
    }
    
    /*protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        Map<String,String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "utf-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[彩世]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            log.error("[彩世]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (null == jsonObject || !jsonObject.containsKey("code")  || !"000000".equals(jsonObject.getString("code"))) {
            log.error("[彩世]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!jsonObject.containsKey("value")  || StringUtils.isBlank(jsonObject.getString("value"))) {
            log.error("[彩世]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        String value = jsonObject.getString("value");
        if(handlerUtil.isWY(channelWrapper)){
            result.put(HTMLCONTEXT, value);
        }else {
            jsonObject = JSONObject.parseObject(value);
            if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_JD_")) {
                StringBuffer sbHtml = new StringBuffer();
                sbHtml.append("<form id='mobaopaysubmit' name='mobaopaysubmit' action='" + jsonObject.getString("requestUrl") + "' method='POST'>");
                sbHtml.append("<input type='hidden' name='body' value='" + jsonObject.getString("body") + "'/>");
                sbHtml.append("<input type='hidden' name='charset' value='" + jsonObject.getString("charset") + "'/>");
                sbHtml.append("<input type='hidden' name='defaultbank' value='" + jsonObject.getString("defaultbank") + "'/>");
                sbHtml.append("<input type='hidden' name='isApp' value='" + jsonObject.getString("isApp") + "'/>");
                sbHtml.append("<input type='hidden' name='merchantId' value='" + jsonObject.getString("merchantId") + "'/>");
                sbHtml.append("<input type='hidden' name='notifyUrl' value='" + jsonObject.getString("notifyUrl") + "'/>");
                sbHtml.append("<input type='hidden' name='orderNo' value='" + jsonObject.getString("orderNo") + "'/>");
                sbHtml.append("<input type='hidden' name='paymentType' value='" + jsonObject.getString("paymentType") + "'/>");
                sbHtml.append("<input type='hidden' name='paymethod' value='" + jsonObject.getString("paymethod") + "'/>");
                sbHtml.append("<input type='hidden' name='returnUrl' value='" + jsonObject.getString("returnUrl") + "'/>");
                sbHtml.append("<input type='hidden' name='sellerEmail' value='" + jsonObject.getString("sellerEmail") + "'/>");
                sbHtml.append("<input type='hidden' name='service' value='" + jsonObject.getString("service") + "'/>");
                sbHtml.append("<input type='hidden' name='title' value='" + jsonObject.getString("title") + "'/>"); 
                sbHtml.append("<input type='hidden' name='totalFee' value='" + jsonObject.getString("totalFee") + "'/>");
                sbHtml.append("<input type='hidden' name='signType' value='" + jsonObject.getString("signType") + "'/>");
                sbHtml.append("<input type='hidden' name='sign' value='" + jsonObject.getString("sign") + "'/>");
                sbHtml.append("</form>"); 
                sbHtml.append("<script>document.forms['mobaopaysubmit'].submit();</script>");
                result.put(HTMLCONTEXT, sbHtml.toString());
                //按不同的请求接口，向不同的属性设置值
//      }else if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
//      }else if(HandlerUtil.isWY(channelWrapper)) {
//            StringBuffer sbHtml = new StringBuffer();
//            sbHtml.append("<form id='mobaopaysubmit' name='mobaopaysubmit' action='" + jsonObject.getString("url") + "' method='post'>");
//            sbHtml.append("<input type='hidden' name='cipher_data' value='" + jsonObject.getString("cipher_data") + "'/>");
//            sbHtml.append("</form>");
//            sbHtml.append("<script>document.forms['mobaopaysubmit'].submit();</script>");
//          result.put(HTMLCONTEXT, sbHtml.toString());
            }else if(handlerUtil.isWebYlKjzf(channelWrapper)) {
                StringBuffer sbHtml = new StringBuffer();
                sbHtml.append("<form id='mobaopaysubmit' name='mobaopaysubmit' action='" + jsonObject.getString("requestUrl") + "' method='POST'>");
                sbHtml.append("<input type='hidden' name='payKey' value='" + jsonObject.getString("payKey") + "'/>");
                sbHtml.append("<input type='hidden' name='orderPrice' value='" + jsonObject.getString("orderPrice") + "'/>");
                sbHtml.append("<input type='hidden' name='outTradeNo' value='" + jsonObject.getString("outTradeNo") + "'/>");
                sbHtml.append("<input type='hidden' name='productType' value='" + jsonObject.getString("productType") + "'/>");
                sbHtml.append("<input type='hidden' name='orderTime' value='" + jsonObject.getString("orderTime") + "'/>");
                sbHtml.append("<input type='hidden' name='payBankAccountNo' value='" + jsonObject.getString("payBankAccountNo") + "'/>");
                sbHtml.append("<input type='hidden' name='productName' value='" + jsonObject.getString("productName") + "'/>");
                sbHtml.append("<input type='hidden' name='orderIp' value='" + jsonObject.getString("orderIp") + "'/>");
                sbHtml.append("<input type='hidden' name='returnUrl' value='" + jsonObject.getString("returnUrl") + "'/>");
                sbHtml.append("<input type='hidden' name='notifyUrl' value='" + jsonObject.getString("notifyUrl") + "'/>");
                sbHtml.append("<input type='hidden' name='sign' value='" + jsonObject.getString("sign") + "'/>");
                sbHtml.append("</form>"); 
                sbHtml.append("<script>document.forms['mobaopaysubmit'].submit();</script>");
                result.put(HTMLCONTEXT, sbHtml.toString());
//      }else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_ZFB_SM")) {
//          StringBuffer sbHtml = new StringBuffer();
//          sbHtml.append("<form id='mobaopaysubmit' name='mobaopaysubmit' action='" + jsonObject.getString("requestUrl") + "' method='POST'>");
//          sbHtml.append("<input type='hidden' name='pay_amount' value='" + jsonObject.getString("pay_amount") + "'/>");
//          sbHtml.append("<input type='hidden' name='pay_applydate' value='" + jsonObject.getString("pay_applydate") + "'/>");
//          sbHtml.append("<input type='hidden' name='pay_bankcode' value='" + jsonObject.getString("pay_bankcode") + "'/>");
//          sbHtml.append("<input type='hidden' name='pay_callbackurl' value='" + jsonObject.getString("pay_callbackurl") + "'/>");
//          sbHtml.append("<input type='hidden' name='pay_memberid' value='" + jsonObject.getString("pay_memberid") + "'/>");
//          sbHtml.append("<input type='hidden' name='pay_notifyurl' value='" + jsonObject.getString("pay_notifyurl") + "'/>");
//          sbHtml.append("<input type='hidden' name='pay_orderid' value='" + jsonObject.getString("pay_orderid") + "'/>");
//          sbHtml.append("<input type='hidden' name='pay_productname' value='" + jsonObject.getString("pay_productname") + "'/>");
//          sbHtml.append("<input type='hidden' name='pay_md5sign' value='" + jsonObject.getString("pay_md5sign") + "'/>");
//          sbHtml.append("</form>"); 
//          sbHtml.append("<script>document.forms['mobaopaysubmit'].submit();</script>");
//          result.put(HTMLCONTEXT, sbHtml.toString());
            }else{
                result.put(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("NEWCAISHI_BANK_WAP_ZFB_SM") ? JUMPURL : QRCONTEXT, jsonObject.get("qrcodeUrl").toString());
            }
        }
        result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
        payResultList.add(result);
        log.debug("[彩世]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }*/

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> result = Maps.newHashMap();
        if (!handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }else {
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "utf-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[彩世]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject jsonObject = null;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                log.error("[彩世]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (null == jsonObject || !jsonObject.containsKey("code")  || !"000000".equals(jsonObject.getString("code"))) {
                log.error("[彩世]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!jsonObject.containsKey("value")  || StringUtils.isBlank(jsonObject.getString("value"))) {
                log.error("[彩世]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            jsonObject = JSONObject.parseObject(jsonObject.getString("value"));
            if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                result.put( QRCONTEXT, jsonObject.getString("qrcodeUrl"));
            } else {
                result.put(JUMPURL, jsonObject.getString("qrcodeUrl"));
            }
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[彩世]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[彩世]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}