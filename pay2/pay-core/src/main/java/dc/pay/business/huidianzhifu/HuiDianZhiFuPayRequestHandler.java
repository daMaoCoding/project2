package dc.pay.business.huidianzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Cobby
 * May 11, 2019
 */
@RequestPayHandler("HUIDIANZHIFU")
public final class HuiDianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiDianZhiFuPayRequestHandler.class);

    private static final String mer_id                ="mer_id";       //商户ID         Y    Y    商户id，由API分配
    private static final String bank_code             ="bank_code";    //银行类型        Y    Y    银行类型，具体参考 附录1
    private static final String amount                ="amount";       //金额           Y    Y    单位：元
    private static final String order_id              ="order_id";     //商户订单号      Y    Y    商户订单号，该值需在商户系统内唯一
    private static final String notify_url            ="notify_url";   //下行异步通知地址 Y    Y    下行异步通知的地址，需要以http://开头且没有任何参数
    private static final String return_format         ="return_format";//返回数据类型     N    N    参数json，返回json格式。

    private static final String key        ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mer_id, channelWrapper.getAPI_MEMBERID());
                put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_format,"json");
            }
        };
        log.debug("[汇点支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //    md5(amount=金额&bank_code=银行编码&mer_id=商户ID&notify_url=通知地址&order_id=订单号&key=密钥)，小写。
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(bank_code+"=").append(api_response_params.get(bank_code)).append("&");
        signSrc.append(mer_id+"=").append(api_response_params.get(mer_id)).append("&");
        signSrc.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
        signSrc.append(order_id+"=").append(api_response_params.get(order_id)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇点支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[汇点支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("code") && "1".equalsIgnoreCase(jsonObject.getString("code"))
                                && jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url"))) {
                    String code_url = jsonObject.getString("pay_url");
                    result.put( JUMPURL , code_url);
                    //if (handlerUtil.isWapOrApp(channelWrapper)) {
                    //    result.put(JUMPURL, code_url);
                    //}else{
                    //    result.put(QRCONTEXT, code_url);
                    //}
                }else {
                    log.error("[汇点支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
            
        } catch (Exception e) {
            log.error("[汇点支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[汇点支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[汇点支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}