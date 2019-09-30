package dc.pay.business.zhongzhifu;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Mikey
 * Jun 12, 2019
 */
@Slf4j
@RequestPayHandler("ZHONGZHIFU")
public final class ZhongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhongZhiFuPayRequestHandler.class);
/*    
    	参数名 		必选 		类型 			长度 		说明
    pid 		是 		String 		64 		商户Pid
    payType 	是 		Number 		1 		商户付款类型，1-支付宝 2-微信
    payScene 	是 		Number 		1 		商户付款场景，1-wap 2-pc
    bizOrderNo 	是 		String 		64 		商户订单号，同一个商户保持唯一
    createTime 	是 		String 		64 		付款请求创建时间yyyy-mm-dd HH:mm:ss 日期格式
    amount 		是 		String 		20 		付款金额，单位元，格式为Decimal（float），最多两位小数
    returnUrl 	是 		String 		255 	同步返回地址，付款成功后，将跳转至该地址
    notifyUrl 	是 		String 		255 	回调地址
    sign 		是 		String 		200 	签名
    signMode 	是 		Number 		1 		固定值，填入2；表示使用MD5 方式签名
*/

    private static final String pid 		= "pid"; 		  	//是 		String 		64 		商户Pid
    private static final String payType 	= "payType"; 	  	//是 		Number 		1 		商户付款类型，1-支付宝 2-微信
    private static final String payScene 	= "payScene"; 	  	//是 		Number 		1 		商户付款场景，1-wap 2-pc
    private static final String bizOrderNo 	= "bizOrderNo"; 	//是 		String 		64 		商户订单号，同一个商户保持唯一
    private static final String createTime 	= "createTime"; 	//是 		String 		64 		付款请求创建时间yyyy-mm-dd HH:mm:ss 日期格式
    private static final String amount 		= "amount"; 		//是 		String 		20 		付款金额，单位元，格式为Decimal（float），最多两位小数
    private static final String returnUrl 	= "returnUrl"; 		//是 		String 		255 	同步返回地址，付款成功后，将跳转至该地址
    private static final String notifyUrl 	= "notifyUrl"; 		//是 		String 		255 	回调地址
    private static final String sign 		= "sign"; 		  	//是 		String 		200 	签名
    private static final String signMode 	= "signMode"; 	  	//是 		Number 		1 		固定值，填入2；表示使用MD5 方式签名
    private static final String key 		= "key"; 		  	

    /**
     *	 參數封裝
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = Maps.newHashMap();
    	
    	payParam.put(pid,channelWrapper.getAPI_MEMBERID());
    	payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);		//商户付款类型，1-支付宝 2-微信
    	payParam.put(payScene,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);	//商户付款场景，1-wap 2-pc
    	payParam.put(bizOrderNo,channelWrapper.getAPI_ORDER_ID());
    	payParam.put(createTime,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
    	payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
    	payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
    	payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    	payParam.put(signMode,"2");
    	
        log.debug("[众支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 	簽名製作
     */
	protected String buildPaySign(Map<String, String> params) throws PayException {
		String pay_md5sign = null;
		params.put(key,channelWrapper.getAPI_KEY());
		List paramKeys = MapUtils.sortMapByKeyAsc(params);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
			if (StringUtils.isBlank(params.get(paramKeys.get(i)))
					|| signMode.equalsIgnoreCase(paramKeys.get(i).toString())) { // signMode不用串進去
				continue;
			}
			sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
		}
		//删除最后一个字符
		sb.deleteCharAt(sb.length()-1);
		params.remove(key);
		pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()); // 进行MD5运算，再将得到的字符串所有字符转换为大写
		log.debug("[众支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

    /**
     * 	發送請求
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(sign,pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[众支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        JSONObject resJson = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("code") && "0".equalsIgnoreCase(resJson.getString("code"))) {
            try {
                result.put(QRCONTEXT, URLDecoder.decode(JSONObject.parseObject(resJson.getString("data")).getString("payUrl"), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                log.error("[众支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }else {
            log.error("[众支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[众支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 	封裝結果
     */
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
        log.debug("[众支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}