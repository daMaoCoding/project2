package dc.pay.business.yifutong;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 21, 2018
 */
@RequestPayHandler("YIFUTONG")
public final class YiFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiFuTongPayRequestHandler.class);

    //输入项                输入项名称           属性            注释                                                              数据类型
    //merchantId             商户号               M            合作商户的商户号，由一付通分配                                       String
    //notifyUrl              通知URL              M            针对该交易的交易状态异步通知接收URL                                  String
    //outOrderId             订单号               M            合作伙伴交易号（确保在合作伙伴系统中唯一                             String
    //subject                订单名称             M            商品描述或者填写商户名称                                             String
    //body                   商品描述             C                                                                                 String
    //transAmt               交易金额             M            交易的总金额，单位为元                                               Double
    //scanType               业务代码             M            支付宝：10000001其他详细文档尾部                                     String
    //sign                   签名值               M            数据的加密校验字符串，目前使用RSA签名算法对待签名数据进行签名        String
    private static final String merchantId                ="merchantId";
    private static final String notifyUrl                 ="notifyUrl";
    private static final String outOrderId                ="outOrderId";
    private static final String subject                   ="subject";
    private static final String body                      ="body";
    private static final String transAmt                  ="transAmt";
    private static final String scanType                  ="scanType";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantId, channelWrapper.getAPI_MEMBERID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(outOrderId,channelWrapper.getAPI_ORDER_ID());
                put(subject,"name");
                put(body,"name");
                put(transAmt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(scanType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[一付通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        String paramsStr = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(paramsStr.substring(0,paramsStr.length()-1),channelWrapper.getAPI_KEY(),"SHA1WithRSA");    // 签名
        } catch (Exception e) {
            log.error("[一付通]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[一付通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
    	if (handlerUtil.isWapOrApp(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }
    	else{
    	    String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[一付通]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[一付通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject resJson = JSONObject.parseObject(resultStr);
            //只取正确的值，其他情况抛出异常
//            "respType":"R""respCode":"99"
            if (null != resJson && resJson.containsKey("respType") && "R".equalsIgnoreCase(resJson.getString("respType"))  
                    && resJson.containsKey("respCode") && "99".equalsIgnoreCase(resJson.getString("respCode"))
                    && resJson.containsKey("payCode") && StringUtils.isNotBlank(resJson.getString("payCode"))) {
                String code_url = resJson.getString("payCode");
                result.put(QRCONTEXT, code_url);
            }else {
                log.error("[一付通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[一付通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[一付通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}