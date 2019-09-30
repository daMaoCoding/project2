package dc.pay.business.zhihui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Oct 16, 2018
 */
@RequestPayHandler("ZHIHUI")
public final class ZhiHuiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhiHuiPayRequestHandler.class);

    //参数名                   参数                可空         加入签名         说明
    //商户ID                   parter               N              Y             商户id，由智汇分配
    //银行类型                 type                 N              Y             银行类型，具体请参考附录1
    //金额                     value                N              Y             单位元（人民币），2位小数，最小支付金额为0.02
    //商户订单号               orderid              N              Y             商户系统订单号，该订单号将作为智汇接口的返回数据。该值需在商户系统内唯一，智汇系统暂时不检查该值是否唯一
    //下行异步通知地址         callbackurl          N              Y             下行异步通知过程的返回地址，需要以http://开头且没有任何参数
    //下行同步通知地址         hrefbackurl          Y              N             下行同步通知过程的返回地址(在支付完成后智汇接口将会跳转到的商户系统连接地址)。注：若提交值无该参数，或者该参数值为空，则在支付完成后，智汇接口将不会跳转到商户系统，用户将停留在智汇接口系统提示支付成功的页面。
    //支付用户IP               payerIp              Y              N             用户在下单时的真实IP，智汇接口将会判断玩家支付时的ip和该值是否相同。若不相同，智汇通接口将提示用户支付风险
    //备注消息                 attach               Y              N             备注信息，下行中会原样返回。若该值包含中文，请注意编码
    //MD5签名                  sign                 N              -             32位小写MD5签名值，GB2312编码
    private static final String parter                  ="parter";
    private static final String type                    ="type";
    private static final String value                   ="value";
    private static final String orderid                 ="orderid";
    private static final String callbackurl             ="callbackurl";
//    private static final String hrefbackurl             ="hrefbackurl";
//    private static final String payerIp                 ="payerIp";
    private static final String attach                  ="attach";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(parter, channelWrapper.getAPI_MEMBERID());
                put(attach, channelWrapper.getAPI_MEMBERID());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(value,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
             }
        };
        log.debug("[智汇]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(parter+"=").append(api_response_params.get(parter)).append("&");
        signSrc.append(type+"=").append(api_response_params.get(type)).append("&");
        signSrc.append(value+"=").append(api_response_params.get(value)).append("&");
        signSrc.append(orderid +"=").append(api_response_params.get(orderid )).append("&");
        signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[智汇]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
//        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
            if (StringUtils.isBlank(resultStr)) {
                log.error("[智汇]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("<") && !resultStr.contains(">")) {
            log.error("[智汇]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWY(channelWrapper)) {
//        if (true) {
            result.put(HTMLCONTEXT, resultStr);
        }else {
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[智汇]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject resJson = JSONObject.parseObject(resultStr);
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
                String code_url = resJson.getString("codeimg");
                result.put(QRCONTEXT, code_url);
            }else {
                log.error("[智汇]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[智汇]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[智汇]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}