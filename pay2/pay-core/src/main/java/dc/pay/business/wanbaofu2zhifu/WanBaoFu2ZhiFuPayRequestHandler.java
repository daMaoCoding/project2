package dc.pay.business.wanbaofu2zhifu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * Mar 25, 2019
 */
@RequestPayHandler("WANBAOFU2ZHIFU")
public final class WanBaoFu2ZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WanBaoFu2ZhiFuPayRequestHandler.class);

    private static final String    P_UserID     = "P_UserID";    //    必填    商户编号如1000001
    private static final String    P_OrderID    = "P_OrderID";   //    必填    商户定单号（要保证唯一），长度最长32字符
    private static final String    P_FaceValue  = "P_FaceValue"; //    必填    申明交易金额
    private static final String    P_ChannelID  = "P_ChannelID"; //    必填    支付方式，支付方式编码：参照附录6.1
    private static final String    P_Price      = "P_Price";     //    非必填   商品售价
    private static final String    P_Result_URL = "P_Result_URL";//    必填    支付后异步通知地址，UBL参数是以http://或https://开头的 完整URL地址(后台处理）提交的url地址必须外网能访问到， 否则无法通知商户


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
            payParam.put(P_UserID,channelWrapper.getAPI_MEMBERID());
            payParam.put(P_OrderID,channelWrapper.getAPI_ORDER_ID());
            payParam.put(P_FaceValue,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(P_Price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(P_ChannelID,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(P_Result_URL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        log.debug("[万宝付2支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s",
                params.get(P_UserID),
                params.get(P_OrderID),
                "",
                "",
                params.get(P_FaceValue),
                params.get(P_ChannelID),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[万宝付2支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        try {
           
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[万宝付2支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[万宝付2支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[万宝付2支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}