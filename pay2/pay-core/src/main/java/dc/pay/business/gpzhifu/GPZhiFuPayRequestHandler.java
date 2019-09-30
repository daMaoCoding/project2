package dc.pay.business.gpzhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * 
 * @author andrew
 * Aug 20, 2019
 */
@RequestPayHandler("GPZHIFU")
public final class GPZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GPZhiFuPayRequestHandler.class);

    private static final String amount      = "amount";     //    订单金额    BigDecimal    是    下单的金额
    private static final String outOrderNo  = "outOrderNo"; //    订单号    String    是    商户订单的唯一标识符
    private static final String orderDesc   = "orderDesc";  //    订单描述    String    是    订单的描述信息
    private static final String timestamp   = "timestamp";  //    请求时间戳    Long    是    13位，请求时间戳不能为空,且与网关时间差不能大于60秒
    private static final String nonceStr    = "nonceStr";   //    业务流水号    String    是    业务流水号不小于10位字符随机字符串用于保证签名的不可预测性
    private static final String returnUrl   = "returnUrl";  //    返回地址    String        成功支付订单的页面返回地址
    private static final String notifyUrl   = "notifyUrl";  //    通知地址    String    是    成功支付后异步回调通知地址
    private static final String appId       = "appId";      //    商户appId    String    是    商户appId
    private static final String payType     = "payType";    //    支付类型    String    是    支付类型:(ALIPAY、WXPAY、CLOUDPAY)
    private static final String userUnqueNo = "userUnqueNo";//    用户编号    String    是    用户唯一编号不能为空,填写用户的ID或者用户名，可加密后传值，用于订单申诉检查,请务必填写很重要
    private static final String attach      = "attach";     //    附加参数    String    是    附加参数，商户自定义值


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[GP支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[GP支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(outOrderNo, channelWrapper.getAPI_ORDER_ID());
                put(orderDesc, channelWrapper.getAPI_ORDER_ID());
                put(timestamp, System.currentTimeMillis() + "");
                put(nonceStr, HandlerUtil.randomStr(11));
                put(returnUrl, channelWrapper.getAPI_WEB_URL());
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(appId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
//                put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(payType, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(userUnqueNo, channelWrapper.getAPI_ORDER_ID());
                put(attach, channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[GP支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        Map<String, String> params = new HashMap<>();
        params.put(outOrderNo, api_response_params.get(outOrderNo));
        params.put(amount, api_response_params.get(amount));
        params.put(payType, api_response_params.get(payType));
        params.put(attach, api_response_params.get(attach));

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder("?");
        for (String key : keys) {
            requestUrl.append(key).append("=");
            try {
                requestUrl.append(URLEncoder.encode(params.get(key), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                requestUrl.append(params.get(key));
            }
            requestUrl.append("&");
        }

        String requestParamsEncode = requestUrl.replace(requestUrl.lastIndexOf("&"), requestUrl.length(), "").toString();

        String md5Value = HandlerUtil.getMD5UpperCase(requestParamsEncode + api_response_params.get(appId) + api_response_params.get(timestamp) + api_response_params.get(nonceStr)).toLowerCase();
        String signMd5  = HandlerUtil.getMD5UpperCase(md5Value + channelWrapper.getAPI_KEY()).toUpperCase();
        log.debug("[GP支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
//            {
//                "code" : 1,
//                "msg" : "success",
//                "data" : "https://api.skpay.club/public/otc/toPay/U0syMDE5MDYyOTE0MTcwMjc5NzQwNDgs",
//                "serverTime" : 1561789022794
//            }
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[GP支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (null != jsonObject && jsonObject.containsKey("code") && "1".equalsIgnoreCase(jsonObject.getString("code"))
                    && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                String code_url = jsonObject.getString("data");
                result.put(JUMPURL, code_url);
            } else {
                log.error("[GP支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[GP支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[GP支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[GP支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}