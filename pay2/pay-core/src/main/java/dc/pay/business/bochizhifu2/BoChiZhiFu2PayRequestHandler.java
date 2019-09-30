package dc.pay.business.bochizhifu2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 19, 2019
 */
@RequestPayHandler("BOCHIZHIFU2")
public final class BoChiZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BoChiZhiFu2PayRequestHandler.class);

    //3.1二维码扫码支付接口
    //参数名称              参数编码               属性           数据描述       数据类型
    //version                版本号                 M              固定值1.0.0       ASC(5)
    //transType              业务类型               M              固定值 SALES       A(4)
    //productId              产品类型               M               0131支付宝H5支付       N(4)
    //merNo                  商户号                 M              下发给商户的商户号       N(15)
    //orderDate              订单日期               M              订单交易日期 yyyyMMdd       N(8)
    //orderNo                订单号                 M              商户平台订单号       ASC(40)
    //notifyUrl              后台通知地址           M              用户完成支付后,服务器后台通知地址       ASC(255)
    //transAmt               交易金额               M              分为单位如 100 代表  1.00元       N(64)
    //commodityDetail        商品名称               M              用户购买商品的名称       ASC(32)
    //subMerchantId          子商户号               C              APP端风控参数 推荐上送       ASC(40)
    //storeId                门店编号               C              APP端风控参数 推荐上送       ASC(40)
    //clientIp               用户IP                 C              APP端风控参数 推荐上送       ASC(15)
    //signature              签名字段               M              参考 目录2.3       
    private static final String version                                      ="version";
    private static final String transType                                    ="transType";
    private static final String productId                                    ="productId";
    private static final String merNo                                        ="merNo";
    private static final String orderDate                                    ="orderDate";
    private static final String orderNo                                      ="orderNo";
    private static final String notifyUrl                                    ="notifyUrl";
    private static final String transAmt                                     ="transAmt";
    private static final String commodityDetail                              ="commodityDetail";
    private static final String returnUrl                                    ="returnUrl";
//    private static final String bankCode                                      ="bankCode";
//    private static final String subMerchantId                                ="subMerchantId";
//    private static final String storeId                                      ="storeId";
//    private static final String clientIp                                     ="clientIp";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0.0");
                put(transType,"SALES");
//                put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(merNo, channelWrapper.getAPI_MEMBERID());
                put(orderDate,  DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(transAmt,  channelWrapper.getAPI_AMOUNT());
                put(commodityDetail,"1");
                put(returnUrl,  channelWrapper.getAPI_WEB_URL());

//                if(HandlerUtil.isWY(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper)){
                if(true){
                    put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                }else {
                    put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        log.debug("[博驰支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        String paramsStr = signSrc.toString();
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        String signMd5="";
        try {
            signMd5 = Rsa.rsa(paramsStr,channelWrapper.getAPI_KEY().getBytes()); // 签名
//            signMd5 = RsaUtil.signByPrivateKey2(paramsStr,channelWrapper.getAPI_KEY(),"utf-8"); // 签名
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[博驰支付2]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[博驰支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();

//        if ( HandlerUtil.isWY(channelWrapper)  ) {
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            if (StringUtils.isBlank(resultStr)) {
                log.error("[博驰支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                log.error("[博驰支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("respCode") && "P000".equalsIgnoreCase(resJson.getString("respCode")) ) {
                result.put(JUMPURL, resJson.getString("payInfo"));
//                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper) && resJson.containsKey("payQRCode") && StringUtils.isNotBlank(resJson.getString("payQRCode"))) {
//                    result.put(QRCONTEXT, resJson.getString("payQRCode"));
//                } else if ((handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper)) && resJson.containsKey("payInfo") && StringUtils.isNotBlank(resJson.getString("payInfo"))) {
//                    result.put(JUMPURL, resJson.getString("payInfo"));
//                }else {
//                    log.error("[博驰支付2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
            }else {
                log.error("[博驰支付2]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        payResultList.add(result);
        log.debug("[博驰支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[博驰支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}