package dc.pay.business.xinguaishouzhifu;

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
import dc.pay.utils.ValidateUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 24, 2019
 */
@RequestPayHandler("XINGUAISHOUZHIFU")
public final class XinGuaiShouZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinGuaiShouZhiFuPayRequestHandler.class);

    private static final String merchant_cod = "merchant_code";// int(10)    √    参数名称：商户ID    商户签约时，MonsterPay分配给商户的唯一身份标识例如：1111110166或者1118004517
    private static final String method       = "method";// String(30)    √    参数名称：支付方式    取值如下（必须小写，多选时请用逗号隔开）    alipay(支付宝)    wechat(微信)    bank(快捷)
    private static final String money        = "money";// Number(6)    √    参数名称：商户订单金额以元为单位
    private static final String order_sn     = "order_sn";// String(64)    √    参数名称：商户订单号商户网站生成的订单号，由商户保证其唯一性，由字母、数字、下划线组成。
    private static final String notify_url   = "notify_url";// String(200)    √    参数名称：页面异步通知地址1.支付成功后，服务器返回参数到该地址
    private static final String return_url   = "return_url";// String(200)    √    参数名称：页面同步通知地址2.支付成功后，通过页面跳转的方式跳转到商户网站
//  private static final String remark           ="remark"         ;// String(200)    ×    参数名称：回传参数(不参与签名)    商户如果支付请求是传递了该参数，则通知商户支付成功时会回传该参数
//  private static final String bank_code        ="bank_code"      ;// String(20)    ×    参数名称：银行代码    银行代码 可为空,当bank_code的值为bank2时支付页面为复制到好友付款
//  private static final String service_type     ="service_type"   ;// String(15)    ×    参数名称：服务类型(不参与签名)    固定值：direct_pay,json
//  private static final String sign             ="sign"           ;// String    √    参数名称：签名数据    该字段不参与签名，值如何获取，请参考MonsterPay提供的示例代码。


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_cod, channelWrapper.getAPI_MEMBERID());
                put(method, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_sn, channelWrapper.getAPI_ORDER_ID());
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url, channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[新怪兽支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length() - 1);
        String paramsStr  = signSrc.toString();
        byte[] privateKey = Base64.decodeBase64(channelWrapper.getAPI_KEY());
        byte[] sign       = new byte[0];
        try {
            sign = RSACoder.sign(paramsStr.getBytes(), privateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String signMd5 = Base64.encodeBase64String(sign);
        log.debug("[新怪兽支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新怪兽支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("result") && "true".equalsIgnoreCase(resJson.getString("result"))
                    && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
                resJson = JSONObject.parseObject(resJson.getString("data"));
                String code_url = resJson.getString("payment_code");
                result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
            } else {
                log.error("[新怪兽支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[新怪兽支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新怪兽支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[新怪兽支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}