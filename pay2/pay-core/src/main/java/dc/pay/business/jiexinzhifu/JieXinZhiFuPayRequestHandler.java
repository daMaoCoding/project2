package dc.pay.business.jiexinzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * Apr 08, 2019
 */
@RequestPayHandler("JIEXINZHIFU")
public final class JieXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JieXinZhiFuPayRequestHandler.class);


    private static final String mchId                ="mchId";      // 商户号
    private static final String body                 ="body";       // 商品描述
    private static final String attach               ="attach";     // 附加数据
    private static final String outTradeNo           ="outTradeNo"; // 订单号
    private static final String totalFee             ="totalFee";   // 订单总金额，单位为元
    private static final String spbillCreateIp       ="spbillCreateIp"; // IP
    private static final String notifyUrl            ="notifyUrl";  // 通知地址
    private static final String tradeType            ="tradeType";  // 类型

    private static final String key        ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchId, channelWrapper.getAPI_MEMBERID());
                put(body,channelWrapper.getAPI_ORDER_ID());
                put(attach,"attach");
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                put(totalFee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(spbillCreateIp,channelWrapper.getAPI_Client_IP());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(tradeType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[捷信支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[捷信支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[捷信支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[捷信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[捷信支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}