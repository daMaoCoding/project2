package dc.pay.business.jiahongzhifu;

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
 * Mar 1, 2019
 */
@RequestPayHandler("JIAHONGZHIFU")
public final class JiaHongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiaHongZhiFuPayRequestHandler.class);

// out_trade_no   非空    分销商订单号，64个字符以内、可包含字母、数字、下划线；需保证在商户端不重复    示例：201901281607
// fxskey         非空    分配的分销商Key    示例：610124198702225113
// total_amount   非空    订单总金额，单位为元，精确到小数点后两位，取值范围[0.01,100000000]。    示例：88.88
// notify_url     非空    异步通知页面路径    示例：http://120.76.225.90:8080/AlipaySys/notifyTest
// return_url     非空    同步通知页面路径    http://120.76.225.90:8080/Distribution/home/return_url.jsp
// sign           非空    签名信息    示例：BA6284CEB37CB305A67EDA9458639EE5
    private static final String out_trade_no           ="out_trade_no"; // 分销商订单号
    private static final String fxskey                 ="fxskey";       // 分配的分销商Key
    private static final String total_amount           ="total_amount"; // 订单总金额
    private static final String notify_url             ="notify_url";   // 异步通知页面路径
    private static final String return_url             ="return_url";   // 同步通知页面路径

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(fxskey, channelWrapper.getAPI_MEMBERID());
                put(total_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[迦鸿支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> params) throws PayException {
//         String hmacstr = "out_trade_no="+out_trade_no+"&fxskey="+fxskey+"&total_amount="+total_amount
//                 +"&notify_url="+notify_url+"&return_url="+return_url+"&key="+key;
        String paramsStr = String.format("out_trade_no=%s&fxskey=%s&total_amount=%s&notify_url=%s&return_url=%s&key=%s",
                 params.get(out_trade_no),
                 params.get(fxskey),
                 params.get(total_amount),
                 params.get(notify_url),
                 params.get(return_url),
                 channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[迦鸿支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[迦鸿支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[迦鸿支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[迦鸿支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}