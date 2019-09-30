package dc.pay.business.tianxianfu;

/**
 * ************************
 * @author tony 3556239829
 */

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

@RequestPayHandler("TIANXIAFU")
public final class TianXiaFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TianXiaFuPayRequestHandler.class);

    // 基本参数
    String service = "service"; // 接口名称
    String signType = "signType"; // 签名类型 MD5/RSA/CA 目前支持MD5
    String inputCharset = "inputCharset"; // 系统之间交互信息时使用的编码字符集 通常默认使用UTF-8
    String sysMerchNo = "sysMerchNo"; // 系统商户号 由我方分配开通

    // 业务参数
    String outOrderNo ="outOrderNo"; // 商户订单号
    String orderTime = "orderTime"; // 订单时间 格式：yyyyMMddHHmmss
    String orderAmt = "orderAmt"; // 订单金额 单位元,精确到分
    String orderTitle = "orderTitle"; // 订单描述
    String orderDetail = "orderDetail"; // 订单描述
    String selectFinaCode = "selectFinaCode"; // 选择扫码机构的编码 支付宝ALIPAY ，微信WEIXIN QQPAY
    String tranAttr = "tranAttr"; // 调用接口提交的交易属性，取值如下： H5
    String backUrl = "backUrl"; // 后台通知地址
    String settleCycle = "settleCycle"; // 收款人到帐周期 T0 T1，默认为T1，如果为T0，以下打款账号为必填
    String clientIp = "clientIp"; // ip
    String sign = "sign"; //sign

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                if(HandlerUtil.isWY(channelWrapper)){
                    put(service ,"ebankPay");
                        put(tranAttr ,"PRIVATE");
                }else{
                    put(service ,"unifiedOrder");
                    if(HandlerUtil.isWapOrApp(channelWrapper)){
                        put(tranAttr ,"H5");
                    }else{
                        put(tranAttr ,"NATIVE");
                    }
                }
                put(signType, "MD5");
                put(inputCharset, "UTF-8");
                put(sysMerchNo, channelWrapper.getAPI_MEMBERID());
                put(outOrderNo, channelWrapper.getAPI_ORDER_ID());
                put(orderTime, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(orderAmt, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderTitle, orderTitle);
                put(orderDetail, orderDetail);
                put(selectFinaCode ,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(backUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(clientIp, channelWrapper.getAPI_Client_IP());
                put(settleCycle, "D0");
            }
        };
        log.debug("[天下支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if("sign".equalsIgnoreCase(paramKeys.get(i).toString())  || "signType".equalsIgnoreCase(paramKeys.get(i).toString()) || StringUtils.isBlank(params.get(paramKeys.get(i)).toString()))continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i)));
            if(i<paramKeys.size()-1)sb.append("&");
        }
        sb.append(channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        log.debug("[天下支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            if (HandlerUtil.isWY(channelWrapper)) {
                HashMap<String, String> result = Maps.newHashMap();
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
                payResultList.add(result);
            }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            JSONObject resJson = JSONObject.parseObject(resultStr);
            if (resJson!=null && resJson.containsKey("retCode") && resJson.containsKey("codeUrl")   &&  resJson.getString("retCode").equalsIgnoreCase("0000") &&   StringUtils.isNotBlank( resJson.getString("codeUrl"))) {
                   String code_url = resJson.getString("codeUrl");
                    HashMap<String, String> result = Maps.newHashMap();
                    if(HandlerUtil.isWapOrApp(channelWrapper) && !HandlerUtil.isWxGZH(channelWrapper)){
                        result.put(JUMPURL, code_url);
                    }else{
                      result.put(QRCONTEXT, code_url);
                    }
                    payResultList.add(result);
            } else {
                log.error("[天下支付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.toJSONString(resultStr));
            }

        }

        } catch (Exception e) {
            log.error("[天下支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[天下支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[天下支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}