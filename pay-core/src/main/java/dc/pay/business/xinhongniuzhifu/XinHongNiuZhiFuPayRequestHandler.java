package dc.pay.business.xinhongniuzhifu;

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
import dc.pay.utils.HttpUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 01, 2019
 */
@RequestPayHandler("XINHONGNIUZHIFU")
public final class XinHongNiuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinHongNiuZhiFuPayRequestHandler.class);


    private static final String brandNo     = "brandNo";    //    是    string    Y    商户编号
    private static final String callbackUrl = "callbackUrl";//    否    string    N    回调通知地址, 如果不传值, 则不通知
    private static final String price       = "price";      //    是    decimal   Y    交易金额（元）, 举例：12.00;小数点最多两位!
    private static final String serviceType = "serviceType";//    是    integer   Y    服务类型 (见服务类型代码表)
    private static final String signType    = "signType";   //    是    string    N    签名方式 (RSA-S)
    private static final String userName    = "userName";   //    是    string    Y    商户端用户名 (交易进行者的身份识别码)
    private static final String clientIP    = "clientIP";   //    是    string    Y    商户端用户端IP
    private static final String orderNo     = "orderNo";    //    是    string    Y    订单编号 (必须唯一)
//  private static final String frontUrl             ="frontUrl";   //    否    string    N    前台跳转地址, 如果不传值, 则不跳转


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(brandNo, channelWrapper.getAPI_MEMBERID());
                put(callbackUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(orderNo, channelWrapper.getAPI_ORDER_ID());
                put(price, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(serviceType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(signType, "RSA-S");
                put(userName, "user" + HandlerUtil.randomStr(5));
                put(clientIP, channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[新红牛支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        // 签名规则
//        brandNo={brandNo}&clientIP={clientIP}&orderNo={orderNo}&price={price}&serviceType={serviceType}&userName={userName}
//        response.setContentType("text/html;charset=UTF-8");
//        try {
//            byte[] sign = RSAUtils.signMd5ByPriKey( signStr, privateKey);
//            order.setSignature( Base64.getEncoder().encodeToString(sign));
//        } catch (Exception e) {
//            response.getWriter().append("RSA signature fail");
//            return;
//        }
//        Order.logger.debug( "Signature : " + order.getSignature());
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(brandNo + "=").append(api_response_params.get(brandNo)).append("&");
        signSrc.append(clientIP + "=").append(api_response_params.get(clientIP)).append("&");
        signSrc.append(orderNo + "=").append(api_response_params.get(orderNo)).append("&");
        signSrc.append(price + "=").append(api_response_params.get(price)).append("&");
        signSrc.append(serviceType + "=").append(api_response_params.get(serviceType)).append("&");
        signSrc.append(userName + "=").append(api_response_params.get(userName));
        String signMd5 = "";
        try {
            byte[] bytes = RSAUtils.signMd5ByPriKey(signSrc.toString(), channelWrapper.getAPI_KEY());
            signMd5 = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.debug("[新红牛支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            Map<String, String> headerMap = Maps.newHashMap();
            headerMap.put("Content-Type", "application/json");
            JSONObject jsonObject = HttpUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), headerMap, payParam);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(jsonObject.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新红牛支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(jsonObject) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(jsonObject.toJSONString());
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("isSuccess") && "true".equalsIgnoreCase(resJson.getString("isSuccess"))
                    && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
                resJson = JSONObject.parseObject(resJson.getString("data"));
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                if (StringUtils.isNotBlank(resJson.getString("payUrl"))) {
                    String code_url = resJson.getString("payUrl");
                    result.put(JUMPURL, code_url);
                } else {
                    String code_url = resJson.getString("qrCode");
                    result.put(QRCONTEXT, code_url);
                }
            } else {
                log.error("[新红牛支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(jsonObject) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(jsonObject.toJSONString());
            }

        } catch (Exception e) {
            log.error("[新红牛支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新红牛支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[新红牛支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}