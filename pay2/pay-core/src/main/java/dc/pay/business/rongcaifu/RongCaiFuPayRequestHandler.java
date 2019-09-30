package dc.pay.business.rongcaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
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
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("RONGCAIFU")
public final class RongCaiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongCaiFuPayRequestHandler.class);
     private static final String  pay_type   = "pay_type";

    // 扫码（银联扫码&微信&支付宝&QQ）
    private static final String     requestNo	  = "requestNo";                 //请求流水号	必输	交易请求流水号
    private static final String     version	  = "version";                       //版本号	必输	V4.0
    private static final String     productId	  = "productId";                 //产品类型	必输	0104 （0104微信扫码、0109支付宝、0118QQ扫码　0125银联扫码）
    private static final String     transId	  = "transId";                       //交易类型	必输	10（由平台统一提供）
    private static final String     merNo	  = "merNo";                         //商户号	必输	商户号
    private static final String     orderDate	  = "orderDate";                 //订单日期	必输	商品订单支付日期yyyyMMdd
    private static final String     orderNo	  = "orderNo";                       //商户订单号	必输	商户订单号
    private static final String     returnUrl	  = "returnUrl";                 //同步通知地址	必输	同步通知地址
    private static final String     notifyUrl	  = "notifyUrl";                 //异步通知地址	必输	异步通知地址
    private static final String     transAmt	  = "transAmt";                  //交易金额	必输	交易金额，单位为分
    private static final String     commodityName	  = "commodityName";         //商品名称	必输	商品名称
    private static final String     cashier	  = "cashier";                       //是否展示收银台	选输	0 直连 、 1展示收银台 不输入默认无收银台
    private static final String     memo	  = "memo";                          //备注	必输	备注
    private static final String     signature	  = "signature";

    private static final String    bankCode    = "bankCode";  	            //银行编号	cashier为0时必填	参考附录个人网银编号
    private static final String    payType    = "payType";  	            //支付方式	cashier为0时必填 1、借记卡  2、贷记卡





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(HandlerUtil.isWY(channelWrapper)){ //4.0网关支付（网银）
            payParam.put(requestNo, channelWrapper.getAPI_ORDER_ID() );
            payParam.put(version, "V4.0" );
            payParam.put(productId,"0103");
            payParam.put(transId, "01" );
            payParam.put(merNo, channelWrapper.getAPI_MEMBERID() );
            payParam.put(orderDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID()  );
            payParam.put(returnUrl, channelWrapper.getAPI_WEB_URL() );
            payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(transAmt, channelWrapper.getAPI_AMOUNT() );
            payParam.put(commodityName, "HelloWorld" );
            payParam.put(cashier,"0");
            payParam.put(memo,"PAY");
            payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(payType,"1");
        }else if( HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)){ //(快捷&微信支付宝H5&QQ钱包H5)
            payParam.put(requestNo, channelWrapper.getAPI_ORDER_ID() );
            payParam.put(version, "V4.0" );
            payParam.put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()  );
            payParam.put(transId, "01" );
            payParam.put(merNo, channelWrapper.getAPI_MEMBERID() );
            payParam.put(orderDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID()  );
            payParam.put(returnUrl, channelWrapper.getAPI_WEB_URL() );
            payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(transAmt, channelWrapper.getAPI_AMOUNT() );
            payParam.put(commodityName, "HelloWorld" );
            payParam.put(memo,"PAY");
        }else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM") ){ // 扫码（银联扫码&微信&支付宝&QQ）
            payParam.put(requestNo, channelWrapper.getAPI_ORDER_ID() );
            payParam.put(version, "V4.0" );
            payParam.put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()  );
            payParam.put(transId, "10" );
            payParam.put(merNo, channelWrapper.getAPI_MEMBERID() );
            payParam.put(orderDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID()  );
            payParam.put(returnUrl, channelWrapper.getAPI_WEB_URL() );
            payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(transAmt, channelWrapper.getAPI_AMOUNT() );
            payParam.put(commodityName, "HelloWorld" );
            payParam.put(cashier,"0");
            payParam.put(memo,"PAY");
        }

        log.debug("[融财付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || bankCode.equalsIgnoreCase(paramKeys.get(i).toString()) || payType.equalsIgnoreCase(paramKeys.get(i).toString())                   || cashier.equalsIgnoreCase(paramKeys.get(i).toString())   || version.equalsIgnoreCase(paramKeys.get(i).toString())  || signature.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString().replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[融财付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
                payResultList.add(result);
            }else{

                String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();

                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM")) {  //扫码（银联扫码&微信&支付宝&QQ）
                    Map<String, String> reqHttpResult = HandlerUtil.urlToMap(resultStr);
                    if(org.apache.commons.collections4.MapUtils.isNotEmpty(reqHttpResult) && reqHttpResult.containsKey("codeUrl") && StringUtils.isNotBlank(reqHttpResult.get("codeUrl"))){
                        String code = reqHttpResult.get("codeUrl");
                        result.put(QRCONTEXT, code);
                        payResultList.add(result);
                    }else{
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[融财付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            log.error("[融财付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[融财付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[融财付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}