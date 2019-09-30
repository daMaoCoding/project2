package dc.pay.business.jutuzhifu;

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
 * July 10, 2019
 */
@RequestPayHandler("JUTUZHIFU")
public final class JuTuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuTuZhiFuPayRequestHandler.class);

    private static final String method           = "method";          //接口名称  是 String(32) alipay.sqm.h5
    private static final String version          = "version";         //版本信息  是 String(8) 1.0
    private static final String nonce_str        = "nonce_str";       //随机字符串  是 String(32)随机字符串，不大于 32 位。推荐随机数生成算法
    private static final String mch_id           = "mch_id";          //商户号  是 String(20) 平台分配的商户号
    private static final String mch_order_no     = "mch_order_no";    //商户订单号  是 String(32) 商户系统内部订单号，要求 32 个字符内，只能是数字、大小写字母，且在同一个商户号下唯一
    private static final String body             = "body";            //商品名称  是 String(128) 商品简单描述
    private static final String cur_code         = "cur_code";        //币种  是 String(10)货币类型，符合 ISO4217 标准的三位字母代码。目前仅支持人民币，CNY
    private static final String total_amount     = "total_amount";    //总金额  是 Decimal(16,2)总金额(单位元，两位小数)
    private static final String spbill_create_ip = "spbill_create_ip";//终端 IP  是 String(20) 终端 IP
    private static final String mch_req_time     = "mch_req_time";    //订单提交时间  是 String(14)订单生成时间，格式为yyyyMMddHHmmss，如 2009年 12 月 25 日 9 点 10 分 10 秒表示为 20091225091010 请使用UTC+8 北京时间
    private static final String notify_url       = "notify_url";      //通知地址  是 String(128)后台通知地址，用于接收支付成功通知
//  private static final String detail             ="detail";          //商品描述  否 String(1024) 对商品的描述信息
//  private static final String attach             ="attach";          //附加数据  否 String(128)附加数据，在查询 API 和支付通知中原样返回，可作为自定义参数使用。
//  private static final String sign_type          ="sign_type";       //签名类型  否 String(10)请填写交易密钥对应的签名类型，如 MD5


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(method, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(version, "1.0");
                put(nonce_str, HandlerUtil.randomStr(8));
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(mch_order_no, channelWrapper.getAPI_ORDER_ID());
                put(body, "body");
                put(cur_code, "CNY");
                put(total_amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(spbill_create_ip, channelWrapper.getAPI_Client_IP());
                put(mch_req_time, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[聚图支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
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
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[聚图支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[聚图支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("return_code") && "SUCCESS".equalsIgnoreCase(resJson.getString("return_code"))
                    && resJson.containsKey("code_url") && StringUtils.isNotBlank(resJson.getString("code_url"))) {
                String code_url = resJson.getString("code_url");
                result.put(JUMPURL, code_url);
            } else {
                log.error("[聚图支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[聚图支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[聚图支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[聚图支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}