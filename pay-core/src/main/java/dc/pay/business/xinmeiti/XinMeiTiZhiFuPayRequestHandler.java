package dc.pay.business.xinmeiti;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XINMEITIZHIFU")
public final class XinMeiTiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinMeiTiZhiFuPayRequestHandler.class);

    private static final String    mert_no = "mert_no";   //	string	商户号	20	是	我方系统分配的商户号
    private static final String    out_trade_no = "out_trade_no";   //	string	商户订单号	32	是	商户生成的订单号，必须唯一
    private static final String    pay_type = "pay_type";   //	string	支付类型	10	是	请参考3支付类型说明表
    private static final String    amount = "amount";   //	int	金额	8	是	单位：分
    private static final String    order_ip = "order_ip";   //	string	用户IP	128	是	支付用户的IP地址
    private static final String    notify_url = "notify_url";   //	string	异步通知地址	100	是	异步回调地址
    private static final String    order_time = "order_time";   //	int	Unix时间戳	20	是	Unix时间戳
    private static final String    sign = "sign";   //	string	签名	32	是	Md5加密，详细加密方法见《加密方法》



     private static final String  state="state";// "ok",
     private static final String  res_type="res_type";// "qr",
     private static final String  res_data="res_data";// "qr",
     private static final String  ok="ok";// "qr",

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mert_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(order_ip,channelWrapper.getAPI_Client_IP());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(order_time,System.currentTimeMillis()+"");
        }
        log.debug("[新媒体支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
                sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[新媒体支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map reqParams = HandlerUtil.convertStringMapToIntMapByKey(payParam, amount, order_time);

        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),reqParams).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqParams);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey(state) && ok.equalsIgnoreCase(jsonResultStr.getString(state)) && jsonResultStr.containsKey(res_type) && StringUtils.isNotBlank(jsonResultStr.getString(res_type))){
                        if("qr".equalsIgnoreCase(jsonResultStr.getString(res_type))  && StringUtils.isNotBlank(jsonResultStr.getString(res_data))){
                            result.put(QRCONTEXT, jsonResultStr.getString(res_data));
                        }else if("url".equalsIgnoreCase(jsonResultStr.getString(res_type))  && StringUtils.isNotBlank(jsonResultStr.getString(res_data))){
                            result.put(JUMPURL,jsonResultStr.getString(res_data));
                        }else{ throw new PayException(resultStr);}
                        payResultList.add(result);
                    }else {
                        throw new PayException(resultStr);
                    }
				}
            }
        } catch (Exception e) { 
             log.error("[新媒体支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[新媒体支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[新媒体支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}