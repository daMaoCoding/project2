package dc.pay.business.xin123zhifu;

import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author cobby
 * Jan 21, 2019
 */
@RequestPayHandler("XIN123ZHIFU")
public final class Xin123ZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(Xin123ZhiFuPayRequestHandler.class);

    private static final String merchantid           ="merchantid";  //商户的ID
	private static final String method               ="method";      //固定为：pay
	private static final String version              ="version";     //固定为1.0.1
	private static final String out_trade_no         ="out_trade_no";//商户订单号
	private static final String total_amount         ="total_amount";//订单金额
	private static final String trade_type           ="trade_type";  //交易类型
	private static final String notify_url           ="notify_url";  //异步通知地址
	private static final String return_url           ="return_url";  //同步通知地址
	private static final String client_ip            ="client_ip";   //支付用户IP

	private static final String key                  ="key";  //签名
	private static final String sign                 ="sign";  //签名

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantid, channelWrapper.getAPI_MEMBERID());
                put(method,"pay");
                put(version,"1.0.1");
	            put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
	            put(total_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
	            put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
	            put(return_url,channelWrapper.getAPI_WEB_URL());
                put(client_ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[新123支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key + "="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新123支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
	    payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
	    String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
	    if (StringUtils.isBlank(resultStr)) {
		    log.error("[新123支付]3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		    throw new PayException(resultStr);
	    }
	    JSONObject resJson = null;
	    try {
		    resJson = JSONObject.parseObject(resultStr);
	    } catch (Exception e) {
		    e.printStackTrace();
		    log.error("[新123支付]3.2.发送支付请求，及获取支付请求结果：" + resJson.toJSONString() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		    throw new PayException(resJson.toJSONString());
	    }
	    if (!"success".equalsIgnoreCase(resJson.getString("status"))) {
		    log.error("[新123支付]3.3.发送支付请求，及获取支付请求结果：" + resJson.toJSONString() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		    throw new PayException(resJson.toJSONString());
	    }
	    ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
	    result.put((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) ?  JUMPURL : QRCONTEXT, resJson.getJSONObject("result").getString("pay_url"));
	    payResultList.add(result);
	    log.debug("[新123支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[新123支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}