package dc.pay.business.qianying;

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
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@RequestPayHandler("QIANYING")
public class QianYingPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianYingPayRequestHandler.class);

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String yuan = HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT());
        if(!yuan.endsWith(".00")){
            throw new PayException("[千应支付]-[请求支付]-1.组装请求参数出错，只支持整数金额："+yuan);
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put("uid", channelWrapper.getAPI_MEMBERID());
                put("type", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put("m",yuan.endsWith(".00")?yuan.substring(0,yuan.length()-3):yuan);
                put("orderid", channelWrapper.getAPI_ORDER_ID());
                put("callbackurl", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put("charset", "UTF-8");
                put("gofalse", channelWrapper.getAPI_WEB_URL());
                put("gotrue", channelWrapper.getAPI_WEB_URL());
                put("token", "3556239829");
               // put("ts", String.valueOf(System.currentTimeMillis()));
            }
        };
        log.debug("[千应支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam) throws PayException {
        StringBuilder sb = new StringBuilder();
        sb.append("uid=").append(payParam.get("uid")).append("&")
                .append("type=").append(payParam.get("type")).append("&")
                .append("m=").append(payParam.get("m")).append("&")
                .append("orderid=").append(payParam.get("orderid")).append("&")
                .append("callbackurl=").append(payParam.get("callbackurl"))
                .append(channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        log.debug("[千应支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        HashMap<String, String> result = Maps.newHashMap();
        StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        //保存第三方返回值
        result.put(HTMLCONTEXT,htmlContent.toString());
        payResultList.add(result);
        log.debug("[千应支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> result) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (!result.isEmpty() && result.size() == 1) {
            Map<String, String> lastResult = result.get(0);
            requestPayResult = buildResult(lastResult,channelWrapper,requestPayResult);
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[千应支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}
