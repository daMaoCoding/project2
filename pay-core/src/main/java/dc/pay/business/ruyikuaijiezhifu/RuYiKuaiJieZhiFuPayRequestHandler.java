package dc.pay.business.ruyikuaijiezhifu;

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
import org.springframework.http.HttpMethod;

import java.util.*;

/**
 * @author Cobby
 * June 18, 2019
 */
@RequestPayHandler("RUYIKUAIJIEZHIFU")
public final class RuYiKuaiJieZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RuYiKuaiJieZhiFuPayRequestHandler.class);

    private static final String merId     = "merId";    // String(10) 商户号 Y
    private static final String merOrdId  = "merOrdId"; // String(64) 商户网站唯一订单号 Y
    private static final String merOrdAmt = "merOrdAmt";// Number 订单金额，格式：10.00 Y
    private static final String payType   = "payType";  // String(2) 支付类型：            Y
    private static final String bankCode  = "bankCode"; // String(7) 银行代码，参考附录银行代码 Y
    private static final String remark    = "remark";   // String(255) 备注信息，可以随机填写 Y
    private static final String returnUrl = "returnUrl";// String(255) 页面返回地址 Y
    private static final String notifyUrl = "notifyUrl";// String(255) 后台异步通知 Y
    private static final String signType  = "signType"; // String(5) 签名方式: MD5, 默认 MD5 Y

    private static final String key = "merKey";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(merOrdId, channelWrapper.getAPI_ORDER_ID());
                put(merOrdAmt, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(bankCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(remark, HandlerUtil.randomStr(6));
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl, channelWrapper.getAPI_WEB_URL());
                put(signType, "MD5");
            }
        };
        log.debug("[如意快捷支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //md5(merId=xxx&merOrdId=xxx&merOrdAmt=xxx&payType=xxx&bankCode=xxx&remark=xxx
        //&returnUrl=xxx&notifyUrl=xxx&signType=MD5&merKey=xxx)
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(merId + "=").append(api_response_params.get(merId)).append("&");
        signSrc.append(merOrdId + "=").append(api_response_params.get(merOrdId)).append("&");
        signSrc.append(merOrdAmt + "=").append(api_response_params.get(merOrdAmt)).append("&");
        signSrc.append(payType + "=").append(api_response_params.get(payType)).append("&");
        signSrc.append(bankCode + "=").append(api_response_params.get(bankCode)).append("&");
        signSrc.append(remark + "=").append(api_response_params.get(remark)).append("&");
        signSrc.append(returnUrl + "=").append(api_response_params.get(returnUrl)).append("&");
        signSrc.append(notifyUrl + "=").append(api_response_params.get(notifyUrl)).append("&");
        signSrc.append(signType + "=").append(api_response_params.get(signType)).append("&");
        signSrc.append(key + "=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[如意快捷支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
//            {"merOrdId":"594318","merOrdAmt":"5.00","payType":"wxpay","retCode":"SUCCESS","re
//                tType":"img","retUrl":"http:\/\/www.xxx.com\/weixin\/img\/aaa.jpg"}
            if (HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYL(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());
            } else {
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[如意快捷支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                JSONObject resJson;
                try {
                    resJson = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[如意快捷支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                //只取正确的值，其他情况抛出异常
                if (null != resJson && resJson.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(resJson.getString("retCode"))
                        && resJson.containsKey("retType") && StringUtils.isNotBlank(resJson.getString("retType"))) {
                    String code_url = resJson.getString("retType");
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                    if ("qrcode".equalsIgnoreCase(code_url)) {
                        result.put(JUMPURL, code_url);
                    } else {
                        result.put(QRCONTEXT, code_url);
                    }
                } else {
                    log.error("[如意快捷支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

            }
        } catch (Exception e) {
            log.error("[如意快捷支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[如意快捷支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[如意快捷支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}