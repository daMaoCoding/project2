package dc.pay.business.yuanshengzhifu;

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

import java.net.URLDecoder;
import java.util.*;

/**
 * @author Cobby
 * July 31, 2019
 */
@RequestPayHandler("YUANSHENGZHIFU")
public final class YuanShengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YuanShengZhiFuPayRequestHandler.class);

    private static final String MerchantID  = "MerchantID";  // "A00001",              //商户号
    private static final String ChannelType = "ChannelType"; // "wechat",              //通道类型，暂时只只是 wechat(微信扫码)
    private static final String PayAmount   = "PayAmount";   // "0.3",                 //支付金额
    private static final String OrderNumber = "OrderNumber"; // "2018020513456",       //商户订单号
    private static final String NotifyUrl   = "NotifyUrl";   // "",                    //商户异步回调地址，必填
//  private static final String ReturnUrl            ="ReturnUrl";   // "",                    //商户同步回调地址
//  private static final String Attach               ="Attach";      // "url_ysc_7653_newpay", //附加信息
//  private static final String Isshow               ="Isshow";      // "1",                   //固定值传1
    private static final String ClientIP    = "ClientIP";    // ""                     //商户IP

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerchantID, channelWrapper.getAPI_MEMBERID());
                put(ChannelType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(PayAmount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(OrderNumber, channelWrapper.getAPI_ORDER_ID());
                put(NotifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ClientIP, channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[原生支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
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
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[原生支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[原生支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("Message") && "OK".equalsIgnoreCase(resJson.getString("Message"))
                    && resJson.containsKey("CodeUrl") && StringUtils.isNotBlank(resJson.getString("CodeUrl"))) {
                String code_url = resJson.getString("CodeUrl");
                if (HandlerUtil.isYLKJ(channelWrapper)) {
                    result.put(HTMLCONTEXT, code_url);
                } else {
                    code_url = URLDecoder.decode(code_url, "UTF-8");
                    result.put(handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL, code_url);
                }
            } else {
                log.error("[原生支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        } catch (Exception e) {
            log.error("[原生支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[原生支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[原生支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}