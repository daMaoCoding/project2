package dc.pay.business.guizuzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * Mar 11, 2019
 */
@RequestPayHandler("GUIZUZHIFU")
public final class GuiZuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GuiZuZhiFuPayRequestHandler.class);


    private static final String shopAccountId       ="shopAccountId";  //   商家ID
    private static final String shopUserId          ="shopUserId";  //   商家⽤户ID
    private static final String amountInString      ="amountInString";  //订单 单位元
    private static final String payChannel          ="payChannel";  // ⽀付宝：alipay
    private static final String shopNo              ="shopNo";  // 商家订单号
    private static final String shopCallbackUrl     ="shopCallbackUrl";  // 订单⽀付成功回调地址
    private static final String returnUrl           ="returnUrl";  // ⽀付成功⻚⾯
    private static final String target              ="target";  // 跳转⽅式 1，⼿机跳转 2、⼆维码展示 否


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(shopAccountId, channelWrapper.getAPI_MEMBERID());
                put(shopUserId,channelWrapper.getAPI_ORDER_ID());
                put(amountInString,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(shopNo,channelWrapper.getAPI_ORDER_ID());
                put(shopCallbackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(target,"1");
            }
        };
        log.debug("[贵族支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //    sign 字符串 验签字符串，
//            MD5（shopAccountId + shopUserId +amountInString + shopNo + payChannel +KEY）；字符串相加再计算MD5⼀次，MD5为32位⼩写；
        String signSrc = String.format("%s%s%s%s%s%s",
                api_response_params.get(shopAccountId),
                api_response_params.get(shopUserId),
                api_response_params.get(amountInString),
                api_response_params.get(shopNo),
                api_response_params.get(payChannel),
                channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String signMd5 = HandlerUtil.getMD5UpperCase(signSrc).toLowerCase();
        log.debug("[贵族支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[贵族支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[贵族支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[贵族支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}