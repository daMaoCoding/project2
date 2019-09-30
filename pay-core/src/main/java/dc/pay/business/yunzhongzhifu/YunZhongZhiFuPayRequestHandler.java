package dc.pay.business.yunzhongzhifu;

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

import java.util.*;

/**
 * @author Cobby
 * June 22, 2019
 */
@RequestPayHandler("YUNZHONFZHIFU")
public final class YunZhongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YunZhongZhiFuPayRequestHandler.class);

    private static final String fxid = "fxid";       //    商务号     是    唯一号，由乐付支付提供
    private static final String fxddh = "fxddh";      //    商户订单号  是    仅允许字母或数字类型[特殊上游不支持字母，最好不要存在字母、特殊符号],不超过22个字符，不要有中文
    private static final String fxdesc = "fxdesc";     //    商品名称    是    utf-8编码
    private static final String fxfee = "fxfee";      //    支付金额    是    请求的价格(单位：元) 可以0.01元
    private static final String fxnotifyurl = "fxnotifyurl";//    异步通知地址 是    异步接收支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
    private static final String fxbackurl = "fxbackurl";  //    同步通知地址 是    支付成功后跳转到的地址，不参与签名。
    private static final String fxpay = "fxpay";      //    请求类型
    private static final String fxip = "fxip";       //    支付用户IP地址   是    用户支付时设备的IP地址
    private static final String fxsmstyle = "fxsmstyle";  //    用 于 扫 码 模 式（sm），默认 0 返回扫码图片，为1 则返回扫码跳转地址。


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(fxid, channelWrapper.getAPI_MEMBERID());
                put(fxddh, channelWrapper.getAPI_ORDER_ID());
                put(fxdesc, channelWrapper.getAPI_ORDER_ID());
                put(fxfee, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(fxnotifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(fxbackurl, channelWrapper.getAPI_WEB_URL());
                put(fxpay, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(fxsmstyle, "1");
                put(fxip, channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[云众支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //      签名【md5(商务号+商户订单号+支付金额+异步通知地址+商户秘钥)】
        String paramsStr = String.format("%s%s%s%s%s",
                api_response_params.get(fxid),
                api_response_params.get(fxddh),
                api_response_params.get(fxfee),
                api_response_params.get(fxnotifyurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[云众支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[云众支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))
                    && jsonObject.containsKey("payurl") && StringUtils.isNotBlank(jsonObject.getString("payurl"))) {
                String code_url = jsonObject.getString("payurl");
                result.put(JUMPURL, code_url);
            } else {
                log.error("[云众支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[云众支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[云众支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[云众支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}