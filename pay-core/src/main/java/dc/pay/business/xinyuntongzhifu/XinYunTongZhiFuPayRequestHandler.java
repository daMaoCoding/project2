package dc.pay.business.xinyuntongzhifu;

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
 * Apr 04, 2019
 */
@RequestPayHandler("XINYUNTONGZHIFU")
public final class XinYunTongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinYunTongZhiFuPayRequestHandler.class);

    private static final String partner                ="partner";     //    商户PID    必填
    private static final String user_seller            ="user_seller"; //    商户号    必填
    private static final String out_order_no           ="out_order_no";//    商户网站订单号（唯一）    必填
    private static final String subject                ="subject";     //    订单名称    必填
    private static final String total_fee              ="total_fee";   //    订单价格    必填
    private static final String body                   ="body";        //    订单描述    选填
    private static final String notify_url             ="notify_url";  //    异步回调地址    必填
    private static final String return_url             ="return_url";  //    同步回调地址    必填
    private static final String pay_type               ="pay_type";    //    wx zfb wxh5 zfbh5 选填（不参与签名）当填写这个项目时，收银台只会显示该支付
//    private static final String http_referer           ="http_referer";//    域名(可选参数 请求是https需要 )    选填 不参与签名

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接[商户号]和[商户PID],如：商户号&商户PID");
        }

        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(partner,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(user_seller,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(out_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
        }

        log.debug("[新运通支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> params) throws PayException {
        //body=123&notify_url=http://www.test.com/notify_url.php&out_order_no=1233&partner=1234567899876111&return_url=http://www.test.com/return_url.php&subject=123&total_fee=1&user_seller=666666
        //=%s&=%s&=%s&=%s&=%s&=%s&=%s&=%s%s
        String paramsStr = String.format("body=%s&notify_url=%s&out_order_no=%s&partner=%s&return_url=%s&subject=%s&total_fee=%s&user_seller=%s%s",
                params.get(body),
                params.get(notify_url),
                params.get(out_order_no),
                params.get(partner),
                params.get(return_url),
                params.get(subject),
                params.get(total_fee),
                params.get(user_seller),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新运通支付]-[请求支付]-2.生成加密URL签名完成：" + signMd5);
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        try {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
        } catch (Exception e) {
            log.error("[新运通支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[新运通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[新运通支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}