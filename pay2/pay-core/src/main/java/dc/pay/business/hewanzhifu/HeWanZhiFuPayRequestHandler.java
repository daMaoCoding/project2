package dc.pay.business.hewanzhifu;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 17, 2019
 */
@RequestPayHandler("HEWANZHIFU")
public final class HeWanZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeWanZhiFuPayRequestHandler.class);

    private static final String mchid     = "mchid";       //商户号 * mchid 是 string 商户号，由平台分配 2088125037856215
    private static final String paytype   = "paytype";     //支付类型 *  是 int 10
    private static final String amount    = "amount";      //总金额 *  是 int 总金额，以分为单位，不允许包含任何字、符号 100
    private static final String orderid   = "orderid";     //商户订单号 *  是 string 商户系统内部的订单号 ,32个字符内、 可包含字母,确保在商户系统唯一 2017225091020231023123
    private static final String ordertime = "ordertime";   //订单生成时间 *  是 20091225091020
    private static final String createip  = "createip";    //终端IP *  是 string 订单生成的机器IP
    private static final String notifyurl = "notifyurl";   //通知地址 *  是 string
//  private static final String body             ="body";        //商品描述  选填 string 商品描述 测试商品描述
//  private static final String callbackurl      ="callbackurl"; //前台地址  选填 string
//  private static final String attach           ="attach";      //附加信息  选填 string 商户附加信息，可做扩展参数，255字符内

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchid, channelWrapper.getAPI_MEMBERID());
                put(paytype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount, channelWrapper.getAPI_AMOUNT());
                put(orderid, channelWrapper.getAPI_ORDER_ID());
                put(ordertime, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(createip, channelWrapper.getAPI_Client_IP());
                put(notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[和万支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> params) throws PayException {
//         mchid={value}&paytype={value}&amount={value}&orderid={value}&ordertime={value}&apikey
        String paramsStr = String.format("mchid=%s&paytype=%s&amount=%s&orderid=%s&ordertime=%s&%s",
                params.get(mchid),
                params.get(paytype),
                params.get(amount),
                params.get(orderid),
                params.get(ordertime),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[和万支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[和万支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("ResponseMessage") && "success".equalsIgnoreCase(resJson.getString("ResponseMessage"))
                    && resJson.containsKey("Response") && StringUtils.isNotBlank(resJson.getString("Response"))) {
                resJson = JSONObject.parseObject(resJson.getString("Response"));
                resJson = JSONObject.parseObject(resJson.getString("Result"));
                String code_url = resJson.getString("payinfo");
                result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
            } else {
                log.error("[和万支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[和万支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[和万支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[和万支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}