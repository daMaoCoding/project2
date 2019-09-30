package dc.pay.business.zhitongbao;

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
import dc.pay.utils.XmlUtil;
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("ZHITONGBAO")
public final class ZhiTongBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhiTongBaoPayRequestHandler.class);

    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";

    private static final String merchant_code = "merchant_code";
    private static final String notify_url = "notify_url";
    private static final String interface_version = "interface_version";
    private static final String client_ip = "client_ip";
    private static final String sign_type = "sign_type";
    private static final String order_no = "order_no";
    private static final String order_time = "order_time";
    private static final String order_amount = "order_amount";
    private static final String product_name = "product_name";
    private static final String service_type = "service_type";
    private static final String pay_type = "pay_type";
    private static final String bank_code = "bank_code";
    private static final String input_charset = "input_charset";
    private static final String auth_code = "auth_code";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                if(HandlerUtil.isWY(channelWrapper) ){
                        put("service_type", "direct_pay");
                        put("input_charset", "UTF-8");
                        put("interface_version", "V3.0");
                        put("pay_type", "b2c");
                        put("bank_code",  channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else{
                   if( HandlerUtil.isFS(channelWrapper) || channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WAP_YL_SM") ){
                        put("input_charset", "UTF-8");
                        put("interface_version", "V3.0");
                    }else{
                       put("interface_version", "V3.1");
                    }
                    put("service_type", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());

                }
                put("merchant_code", channelWrapper.getAPI_MEMBERID());
                put("notify_url", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put("client_ip",channelWrapper.getAPI_Client_IP());
                put("sign_type", "RSA-S");
                put("order_no", channelWrapper.getAPI_ORDER_ID());
                //put("order_no", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())); //// TODO: 2017/12/11
                //put("order_no", "TI20170628151958591937");
                put("order_time", DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss")); //yyyy-MM-dd HH:mm:ss
                put("order_amount", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put("product_name", "PAY");
            }
        };

        String fsAuthCode = HandlerUtil.getFsAuthCode(channelWrapper); //反扫授权码
        if(StringUtils.isNotBlank(fsAuthCode)){
            payParam.put(auth_code,fsAuthCode);
        }

        log.debug("[智通宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map params) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        if(HandlerUtil.isWY(channelWrapper)){
            signSrc.append("bank_code=").append(params.get(bank_code)).append("&");
            signSrc.append("client_ip=").append(params.get(client_ip)).append("&");
            signSrc.append("input_charset=").append(params.get(input_charset)).append("&");
            signSrc.append("interface_version=").append(params.get(interface_version)).append("&");
            signSrc.append("merchant_code=").append(params.get(merchant_code)).append("&");
            signSrc.append("notify_url=").append(params.get(notify_url)).append("&");
            signSrc.append("order_amount=").append(params.get(order_amount)).append("&");
            signSrc.append("order_no=").append(params.get(order_no)).append("&");
            signSrc.append("order_time=").append(params.get(order_time)).append("&");
            signSrc.append("pay_type=").append(params.get(pay_type)).append("&");
            signSrc.append("product_name=").append(params.get(product_name)).append("&");
            signSrc.append("service_type=").append(params.get(service_type));
        }else if(HandlerUtil.isFS(channelWrapper)){
            signSrc.append("auth_code=").append(params.get(auth_code)).append("&");
            signSrc.append("client_ip=").append(params.get(client_ip)).append("&");
            signSrc.append("input_charset=").append(params.get(input_charset)).append("&");
            signSrc.append("interface_version=").append(params.get(interface_version)).append("&");
            signSrc.append("merchant_code=").append(params.get(merchant_code)).append("&");
            signSrc.append("notify_url=").append(params.get(notify_url)).append("&");
            signSrc.append("order_amount=").append(params.get(order_amount)).append("&");
            signSrc.append("order_no=").append(params.get(order_no)).append("&");
            signSrc.append("order_time=").append(params.get(order_time)).append("&");
            signSrc.append("product_name=").append(params.get(product_name)).append("&");
            signSrc.append("service_type=").append(params.get(service_type));
        }else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WAP_YL_SM") ){
            signSrc.append("client_ip=").append(params.get(client_ip)).append("&");
            signSrc.append("input_charset=").append(params.get(input_charset)).append("&");
            signSrc.append("interface_version=").append(params.get(interface_version)).append("&");
            signSrc.append("merchant_code=").append(params.get(merchant_code)).append("&");
            signSrc.append("notify_url=").append(params.get(notify_url)).append("&");
            signSrc.append("order_amount=").append(params.get(order_amount)).append("&");
            signSrc.append("order_no=").append(params.get(order_no)).append("&");
            signSrc.append("order_time=").append(params.get(order_time)).append("&");
            signSrc.append("product_name=").append(params.get(product_name)).append("&");
            signSrc.append("service_type=").append(params.get(service_type));
        } else{
            signSrc.append("client_ip=").append(params.get(client_ip)).append("&");
            signSrc.append("interface_version=").append(params.get(interface_version)).append("&");
            signSrc.append("merchant_code=").append(params.get(merchant_code)).append("&");
            signSrc.append("notify_url=").append(params.get(notify_url)).append("&");
            signSrc.append("order_amount=").append(params.get(order_amount)).append("&");
            signSrc.append("order_no=").append(params.get(order_no)).append("&");
            signSrc.append("order_time=").append(params.get(order_time)).append("&");
            signSrc.append("product_name=").append(params.get(product_name)).append("&");
            signSrc.append("service_type=").append(params.get(service_type));
        }

        String signInfo = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY());	// 签名
        } catch (Exception e) {
            log.error("[智通宝]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        //String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[智通宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            if (HandlerUtil.isWY(channelWrapper) || channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WAP_YL_SM")) {  //银联H5,银联WAP
                Map result = Maps.newHashMap();
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());
                payResultList.add(result);
            }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            resultStr = resultStr.replaceAll("<dinpay>", "").replaceAll("</dinpay>", "");
            Map<String, String> mapBodys = XmlUtil.toMap(resultStr.getBytes(), "utf-8");
            // JSONObject resJson = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
            String resp_code = mapBodys.get("resp_code");//SUCCESS
            String order_amount = mapBodys.get("order_amount");//1.00
            String result_code = mapBodys.get("result_code");//0
            HashMap<String, String> result = Maps.newHashMap();
            if(HandlerUtil.isFS(channelWrapper)){
                if ("SUCCESS".equalsIgnoreCase(resp_code) && "0".equalsIgnoreCase(result_code) && StringUtils.isNotBlank(order_amount)){
                    result.put(QRCONTEXT,"受理成功,充值成功！");
                    payResultList.add(result);
                }else if ("SUCCESS".equalsIgnoreCase(resp_code) && "1".equalsIgnoreCase(result_code) && StringUtils.isNotBlank(order_amount)){
                    result.put(QRCONTEXT,"受理成功,如信息正确，稍后请刷新余额。");
                    payResultList.add(result);
                } else{
                    throw  new PayException(resultStr);
                }

            }else{
                if ("SUCCESS".equalsIgnoreCase(resp_code) && "0".equalsIgnoreCase(result_code) && StringUtils.isNotBlank(order_amount)) {
                    if(HandlerUtil.isWapOrApp(channelWrapper)) {
                        String payurl = mapBodys.get("payurl");
                        if(null!=payurl && StringUtils.isNotBlank(payurl)){
                            result.put(JUMPURL, HandlerUtil.UrlDecode(payurl));
                            payResultList.add(result);
                        }else{
                            log.error("发送支付请求，及获取支付请求结果错误："+resultStr);
                            throw new PayException(resultStr );
                        }
                    }else{
                        String qrcode = mapBodys.get("qrcode");
                        result.put(QRCONTEXT, HandlerUtil.UrlDecode(qrcode));
                        payResultList.add(result);
                    }

                } else {
                    log.error("[智通宝]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(mapBodys) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(mapBodys));
                }
            }



        }

        } catch (Exception e) {
            log.error("[智通宝]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[智通宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[智通宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}