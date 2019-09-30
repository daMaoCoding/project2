package dc.pay.business.xianlizhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.*;
import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RequestPayHandler("XIANLIZHIFU")
public final class XianLiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XianLiZhiFuPayRequestHandler.class);

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        SortedMap<String, String> payParam = Maps.newTreeMap();
        return payParam;
    }


    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = "";
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParamA, String pay_md5sign) throws PayException {
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==1 || HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper) ) {

                Map<String,  Object> parms = Maps.newTreeMap();
                parms.put("parms[amount]",HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
                parms.put("parms[currency]","CNY");
                parms.put("parms[notify_url]",HandlerUtil.UrlEncode(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()));
                parms.put("parms[order_name]",channelWrapper.getAPI_ORDER_ID());
                parms.put("parms[order_no]",channelWrapper.getAPI_ORDER_ID());
                parms.put("parms[pay_type]",channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());

                SortedMap<String, Object> payParam = Maps.newTreeMap();
                payParam.put("action",channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put("client_ip",channelWrapper.getAPI_Client_IP());
                payParam.put("encrypt_mode","0");
                payParam.put("key",channelWrapper.getAPI_KEY());
                payParam.put("model","cash");
                payParam.put("sn",channelWrapper.getAPI_MEMBERID());
                payParam.put("parms",parms);

                HashSet<String> spParam = Sets.newHashSet();//不需要引号的值
                spParam.add("parms[amount]");
                spParam.add("encrypt_mode");


                // sign: BCC2EEBED55EFA7CB9CC466CC3C483C1
                String str = HandlerUtil.mapToJsonWithSpParam(payParam,spParam).replaceAll("parms\\[","").replaceAll("\\]","");
                payParam.put("sign",HandlerUtil.md5(str));
                payParam.remove("parms");
                payParam.remove("key");
                payParam.put("parms[notify_url]",channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.putAll(parms);

                StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                result.put(HTMLCONTEXT,htmlContent.toString());
                payResultList.add(result);


            }
        } catch (Exception e) {
            log.error("[先力支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[先力支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[先力支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}