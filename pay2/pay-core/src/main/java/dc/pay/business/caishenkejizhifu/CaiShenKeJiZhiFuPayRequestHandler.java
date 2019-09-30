package dc.pay.business.caishenkejizhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 15, 2019
 */
@RequestPayHandler("CAISHENKEJIZHIFU")
public final class CaiShenKeJiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CaiShenKeJiZhiFuPayRequestHandler.class);

    private static final String merchantCode = "merchantCode"; //    商户号    char    Y    商户编号
    private static final String orderNo      = "orderNo";      //    订单号    char    Y    必须唯一
    private static final String menty        = "menty";        //    交易金额    int    Y    分为单位，整数
    private static final String typeCode     = "typeCode";     //    业务编码    char    Y    详情见：业务编码
    private static final String wares        = "wares";        //    商品详情    char    Y    商品详情
    private static final String notifyUrl    = "notifyUrl";    //    异步通知地址    char    Y    支付成功后，平台主动通知商家系统，商家系统必须指定接收通知的地址。
    private static final String pageUrl      = "pageUrl";      //    支付成功，页面跳转地址    char    N    H5业务必填
//  private static final String bankCode             ="bankCode";     //    银行编码    char    N    网关业务必填
//  private static final String signature            ="signature";    //    签名    char    Y    详情见：签名加密明细

    private static final String key = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantCode, channelWrapper.getAPI_MEMBERID());
                put(orderNo, channelWrapper.getAPI_ORDER_ID());
                put(menty, channelWrapper.getAPI_AMOUNT());
                put(typeCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(wares, "name");
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pageUrl, channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[财神科技支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
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
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[财神科技支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            String     resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[财神科技支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("status") && "SUCCESS".equalsIgnoreCase(resJson.getString("status"))
                    && resJson.containsKey("order_data") && StringUtils.isNotBlank(resJson.getString("order_data"))) {
                String code_url = resJson.getString("order_data");
                result.put(JUMPURL, code_url);
                if (resultStr.contains("<form")) {
                    result.put(HTMLCONTEXT, code_url);
                } else {
                    result.put(JUMPURL, code_url);
                }
            } else {
                log.error("[财神科技支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[财神科技支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[财神科技支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[财神科技支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}