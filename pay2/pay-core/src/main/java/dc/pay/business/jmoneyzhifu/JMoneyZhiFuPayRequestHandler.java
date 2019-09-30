package dc.pay.business.jmoneyzhifu;

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
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 17, 2019
 */
@RequestPayHandler("JMONEYZHIFU")
public final class JMoneyZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JMoneyZhiFuPayRequestHandler.class);

    private static final String customer = "customer";     //  商户ID        否    商户编码，由Jmoney商户系统分配 数字
    private static final String banktype = "banktype";     //  支付类型       否    支付类型或银行类型，具体请参考附录1,支持的支付类型有：支付宝，微信，QQ钱包，网银, 京东钱包，银联扫码
    private static final String amount = "amount";       //  金额          否    单位元(人民币)2位小数，最小支付金额为1.00,微信支付宝至少2元，例如：1.00
    private static final String orderid = "orderid";      //  商户订单号     否    商户系统订单号，该订单号将作为Jmoney接口的返回数据。该值需在商户系统内唯一
    private static final String asynbackurl = "asynbackurl";  //  异步通知地址    否    异步通知过程的返回地址，需要以http://开头且没有任何参数(如存在特殊字符请转码,注:不支持参数)
    private static final String request_time = "request_time"; //  请求时间       否    系统请求时间，精确到秒，长度14位，格式为：yyyymmddhhmmss例如：20170820172323 注：北京时间
//  private static final String synbackurl         ="synbackurl";    //  同步通知地址    是    同步通知过程的返回地址(在支付完成后Jmoney接口将会跳转到的商户系统连接地址)。注：若提交值无该参数，或者该参数值为空，则在支付完成后，Jmoney接口将不会跳转到商户系统
//  private static final String israndom           ="israndom";      //  启用订单风控保护规则        是    如果值为Y，则启用订单风控保护规则,默认为启用状态，可选值为：Y、N；备注：为了提高支付成功率强烈建议启用
//  private static final String attach             ="attach";        //  备注消息        是    备注信息，通知过程中会原样返回。若该值包含中文，请注意编码,编码方式为：UTF-8

    private static final String key = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(customer, channelWrapper.getAPI_MEMBERID());
                put(banktype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderid, channelWrapper.getAPI_ORDER_ID());
                put(asynbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(request_time, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            }
        };
        log.debug("[Jmoney支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //customer={0}&banktype={1}&amount={2}&orderid={3}&asynbackurl={4}&request_time={5}&key={6}
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(customer + "=").append(api_response_params.get(customer)).append("&");
        signSrc.append(banktype + "=").append(api_response_params.get(banktype)).append("&");
        signSrc.append(amount + "=").append(api_response_params.get(amount)).append("&");
        signSrc.append(orderid + "=").append(api_response_params.get(orderid)).append("&");
        signSrc.append(asynbackurl + "=").append(api_response_params.get(asynbackurl)).append("&");
        signSrc.append(request_time + "=").append(api_response_params.get(request_time)).append("&");
        signSrc.append(key + "=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[Jmoney支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[Jmoney支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[Jmoney支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[Jmoney支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}