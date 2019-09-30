package dc.pay.business.xinxiaoxiongbao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Sep 10, 2018
 */
@RequestPayHandler("XINXIAOXIONGBAO")
public final class XinXiaoXiongBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinXiaoXiongBaoPayRequestHandler.class);

    //参数名                含义                        类型                说明                                                                                
    //type                 接口调用方式                 string              默认为表单模式。form：表单模式；json：返回json结构                                  
    //merchantId           商户uid                      string              必填。您的商户唯一标识，注册后在设置里获得。                                        
    //money                该笔记录所要支付的金额       decimal             必填。需要由用户支付的金额。                                                        
    //timestamp            时间戳                       long                必填。精确到毫秒                                                                    
    //goodsName            商品名称                     string(64)          选填。商品的名称，商户后台会展示该名称。                                            
    //notifyURL            回调地址                     string(255)         选填。支付成功后系统会对该地址发起回调，通知支付成功的消息。                        
    //returnURL            支付结果展示地址             string(255)         选填。成功成功后系统会跳转页面到该地址上。                                          
    //merchantOrderId      商户自定义订单号             string(32)          必填。商户自定的订单号，该订单号将后在后台展示。                                    
    //merchantUid          商户自定义会员ID             string(32)          选填。商户提交支付的用户Id，该ID会后台展示。                                        
    //paytype              支付类型                     string(32)          默认为微信支付。选择参数：WX、QQ、ALIPAY(值均为大写)。                              
    //sign                 签名                         string(32)          必填。把参数，连密匙一起，按指定的顺序。做md5-32位加密，取字符串小写。得到key。     
    private static final String type                         ="type";
    private static final String merchantId                   ="merchantId";
    private static final String money                        ="money";
    private static final String timestamp                    ="timestamp";
    private static final String goodsName                    ="goodsName";
    private static final String notifyURL                    ="notifyURL";
    private static final String returnURL                    ="returnURL";
    private static final String merchantOrderId              ="merchantOrderId";
//    private static final String merchantUid                  ="merchantUid";
    private static final String paytype                      ="paytype";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//                put(type,handlerUtil.isWapOrApp(channelWrapper) ? "form" : "json");
                put(type,"json");
                put(merchantId, channelWrapper.getAPI_MEMBERID());
//                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(money,  HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
                put(timestamp,System.currentTimeMillis()+"");
                put(goodsName,"name");
                put(returnURL,channelWrapper.getAPI_WEB_URL());
                put(notifyURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(merchantOrderId,channelWrapper.getAPI_ORDER_ID());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[新小熊宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(money)).append("&");
        signSrc.append(api_response_params.get(merchantId)).append("&");
        signSrc.append(api_response_params.get(notifyURL)).append("&");
        signSrc.append(api_response_params.get(returnURL)).append("&");
        signSrc.append(api_response_params.get(merchantOrderId)).append("&");
        signSrc.append(api_response_params.get(timestamp)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新小熊宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
        if (StringUtils.isBlank(resultStr)) {
            log.error("[新小熊宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
//        if (handlerUtil.isWapOrApp(channelWrapper)) {
        if (false) {
//            if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("XIAOXIONGBAO_BANK_WAP_ZFB_SM")
//                    || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("XIAOXIONGBAO_BANK_WAP_JD_SM")
//                    ) {
//                String src = Jsoup.parse(resultStr).select("[id=show_qrcode]").first().attr("src");
//                if (StringUtils.isBlank(src)) {
//                    log.error("[新小熊宝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//                String byBase64 = QRCodeUtil.decodeByBase64(src);
//                if (StringUtils.isBlank(byBase64)) {
//                    log.error("[新小熊宝]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//                result.put(JUMPURL, byBase64);
//            }else {
//                result.put(HTMLCONTEXT, resultStr);
//            }
            
            Elements elements = Jsoup.parse(resultStr).select("[id=show_qrcode]");
            if (null == elements || elements.size() < 1) {
                log.error("[新小熊宝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            String src = elements.first().attr("src");
            if (StringUtils.isBlank(src)) {
                log.error("[新小熊宝]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            String byBase64 = QRCodeUtil.decodeByBase64(src);
            if (StringUtils.isBlank(byBase64)) {
                log.error("[新小熊宝]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            result.put(JUMPURL, byBase64);
        }else {
            JSONObject resJson = JSONObject.parseObject(resultStr);
            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("code") && "10000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("img") && StringUtils.isNotBlank(resJson.getString("img"))) {
//                String code_url = resJson.getString("img");
//                result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, QRCodeUtil.decodeByBase64(code_url));
//            }else {
//                log.error("[新小熊宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
            
            if (null != resJson && resJson.containsKey("code") && "10000".equalsIgnoreCase(resJson.getString("code"))  && 
                    (resJson.containsKey("img") && StringUtils.isNotBlank(resJson.getString("img")) || resJson.containsKey("payURL") && StringUtils.isNotBlank(resJson.getString("payURL")))) {
                if (handlerUtil.isWapOrApp(channelWrapper)) {
                    result.put(JUMPURL, resJson.getString("payURL"));
                }else {
                    String qr = QRCodeUtil.decodeByBase64(resJson.getString("img"));
                    if (StringUtils.isBlank(qr)) {
                        log.error("[新小熊宝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                        throw new PayException(resultStr);
                    }
                    result.put(QRCONTEXT, qr);
                }
            }else {
                log.error("[新小熊宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[新小熊宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新小熊宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}