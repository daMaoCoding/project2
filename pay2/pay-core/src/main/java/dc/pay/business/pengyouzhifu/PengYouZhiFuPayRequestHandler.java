package dc.pay.business.pengyouzhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("PENGYOUZHIFU")
public final class PengYouZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PengYouZhiFuPayRequestHandler.class);

    private static final String    mer_id = "mer_id"; 		   // 商户号   20
    private static final String    out_trade_no = "out_trade_no"; 		   // 商户订单号   32
    private static final String    pay_type = "pay_type"; 		   // 支付类型   3
    private static final String    goods_name = "goods_name"; 		   // 商品描述   50
    private static final String    total_fee = "total_fee"; 		   // 交易金额   6
    private static final String    callback_url = "callback_url"; 		   // 前台回调URL   100
    private static final String    notify_url = "notify_url"; 		   // 后台同步URL   100
    private static final String    attach = "attach"; 		   // 透传参数   36
    private static final String    nonce_str = "nonce_str"; 		   // 随机字符串   32
    private static final String    sign = "sign"; 		   // 签名   32


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mer_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(goods_name,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,channelWrapper.getAPI_AMOUNT());
            payParam.put(callback_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(attach,"1");
            payParam.put(nonce_str,HandlerUtil.getRandomStr(8));
        }
        log.debug("[朋友支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("mer_id=%s&nonce_str=%s&out_trade_no=%s&total_fee=%s&key=%s",
                params.get(mer_id),
                params.get(nonce_str),
                params.get(out_trade_no),
                params.get(total_fee),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[朋友支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper) &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "0".equalsIgnoreCase(jsonResultStr.getString("status")) && jsonResultStr.containsKey("code_url")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("code_url"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isWebYlKjzf(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString("code_url"));
                                    payResultList.add(result);
                                }else{
                                    result.put(QRCONTEXT, jsonResultStr.getString("code_url"));
                                    payResultList.add(result);
                                }
                            }else{
                                throw new PayException(resultStr);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[朋友支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[朋友支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[朋友支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}