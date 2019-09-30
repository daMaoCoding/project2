package dc.pay.business.zhuoyuezhifu;

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
@RequestPayHandler("ZHUOYUEZHIFU")
public final class ZhuoYueZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhuoYueZhiFuPayRequestHandler.class);
/*    
	参数名称				是否必须		数据类型				描述
	orderId				true		bigint (<50 位)		订单 id
	rechargeAmount		true		decimal				充值金额(100-10000)
	type				true		enum				请求类型：1 支付宝
	notifyUrl			true		string				异步通知地址（支付完成回调地址）
	appId				true		int					应用 ID  
	sign				true		string				签名（根据提供的私钥签名）
*/
    private static final String orderId			= "orderId";			//订单 id
    private static final String rechargeAmount	= "rechargeAmount";		//充值金额(100-10000)
    private static final String type			= "type";				//请求类型：1 支付宝
    private static final String notifyUrl		= "notifyUrl";			//异步通知地址（支付完成回调地址）
    private static final String appId			= "appId";				//应用 ID  
    private static final String sign			= "sign";				//签名（根据提供的私钥签名）
    private static final String key				= "key";				

    /**
     *	 參數封裝
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = Maps.newHashMap();
    	
    	payParam.put(orderId,channelWrapper.getAPI_ORDER_ID());
    	payParam.put(rechargeAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
    	payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
    	payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    	payParam.put(appId,channelWrapper.getAPI_MEMBERID());

        log.debug("[卓越支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 	簽名製作
     */
	protected String buildPaySign(Map<String, String> params) throws PayException {
		String pay_md5sign = null;
		List paramKeys = MapUtils.sortMapByKeyAsc(params);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
			sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
		}
		sb.append(key+"=").append(channelWrapper.getAPI_KEY());
		pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()); // 进行MD5运算，再将得到的字符串所有字符转换为大写
		log.debug("[卓越支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
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
            log.error("[卓越支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        JSONObject resJson = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("code") && "0".equalsIgnoreCase(resJson.getString("code"))) {
            try {
                result.put(JUMPURL, URLDecoder.decode(JSONObject.parseObject(resJson.getString("data")).getString("qrcodeUrl"), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                log.error("[卓越支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }else {
            log.error("[卓越支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[卓越支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[卓越支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}