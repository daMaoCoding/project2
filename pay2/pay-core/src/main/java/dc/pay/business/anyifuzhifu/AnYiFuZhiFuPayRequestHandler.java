package dc.pay.business.anyifuzhifu;



import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 
 * @author Wilson Chou
 * 06 6, 2019
 */
@RequestPayHandler("ANYIFUZHIFU")
public final class AnYiFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AnYiFuZhiFuPayRequestHandler.class);

    private static final String pay_memberid    = "pay_memberid";       // 商户ID
    private static final String pay_orderid     = "pay_orderid";        // 位订单号
    private static final String pay_applydate   = "pay_applydate";      // 提交时间yyyy-MM-dd HH:mm:ss
    private static final String pay_bankcode    = "pay_bankcode";       // 通道编码
    private static final String pay_notifyurl   = "pay_notifyurl";      // 通知地址
    private static final String pay_callbackurl = "pay_callbackurl";    // 回调地址
    private static final String pay_amount      = "pay_amount";         // 商品金额
    private static final String pay_productname = "pay_productname";    // 商品名称
//  private static final String type                = "type";               // 商品金额
    //pay_userid  平台会员id  是   否   例如：pay_userid=123            或者使用md5加密如：pay_userid=md5(123)
    private static final String pay_userid                = "pay_userid";               // 商品金额


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBER_PLATFORMID());
                put(pay_orderid, channelWrapper.getAPI_ORDER_ID());
                put(pay_applydate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl, channelWrapper.getAPI_WEB_URL());
                put(pay_amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_bankcode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_productname, "name");
//                put(type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(pay_userid, HandlerUtil.getRandomNumber(6));
            }
        };
        log.debug("[安易付支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_userid.equals(paramKeys.get(i)) && !pay_productname.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
//            if (!pay_productname.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = null;
        try {
            signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        } catch (Exception e) {
            log.error("[安易付支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}", e.getMessage(), e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[安易付支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        HashMap<String, String> result = Maps.newHashMap();
        try {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());
            payResultList.add(result);
        } catch (Exception e) {
             log.error("[安易付支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[安易付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[安易付支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}