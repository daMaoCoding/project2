package dc.pay.business.aimisensecond;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Mar 19, 2018
 */
@RequestPayHandler("AIMISENSECOND")
public final class AiMiSenSecondPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AiMiSenSecondPayRequestHandler.class);

	//src_code			是			商户唯一标识
	//out_trade_no		是			接入方交易订单号
	//total_fee			是			订单总金额，单位分
	//time_start		是			发起交易的时间， 时间格式为：YYYYMMDDHHmmSS，如20170101120000
	//goods_name		是			商品名称
	//trade_type		是			交易类型，
	//finish_url		是			支付完成页面的url，有效性根据实际通道而定
	//mchid				否			平台商户号,注意:out_mchid 与mchid 两个参数必须传一个
	//extend			否			扩展域，此字段是一个json 格式，具体参数如下表（网关支付必传）
	//bankName			是			银行名称总行名称，值范围：北京农村商业银行, 农业银行, 华夏银行,
	//cardType			是			卡类型，目前只支持借记卡，取值“借记卡”
	private static final  String  src_code		= "src_code";			
	private static final String  out_trade_no	= "out_trade_no";		
	private static final String  total_fee		= "total_fee";			
	private static final String  time_start		= "time_start";			
	private static final String  goods_name		= "goods_name";			
	private static final String  trade_type		= "trade_type";			
	private static final String  finish_url		= "finish_url";			
	private static final String  mchid			= "mchid";			
	private static final String  extend			= "extend";			
	private static final String  bankName		= "bankName";			
	private static final String  cardType		= "cardType";			
	private static final String  key		= "key";			

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
//        	throw new PayException("商户号格式错误：mchid & src_code");
            throw new PayException("商户号格式错误：mchid&src_code");
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(src_code,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(out_trade_no, channelWrapper.getAPI_ORDER_ID());
                put(total_fee, channelWrapper.getAPI_AMOUNT());
                put(time_start, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(goods_name, "PAY");
                put(trade_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(finish_url, channelWrapper.getAPI_WEB_URL());
                if(handlerUtil.isWY(channelWrapper)){
                    HashMap<String, String> extendMap = Maps.newHashMap();
                    extendMap.put(bankName,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                    extendMap.put(cardType,"借记卡");
                    put(extend, JSON.toJSONString(extendMap));
                }
            }
        };
        log.debug("[新艾米森]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> payParam) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append(key+"=" + channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());
        log.debug("[新艾米森]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
        if (StringUtils.isBlank(resultStr)) {
        	log.error("[新艾米森]3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
        	throw new PayException("第三方返回异常:返回空"+",参数："+JSON.toJSONString(payParam));
        }
        JSONObject resJson = null;
		try {
			resJson = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			log.error("[新艾米森]3.2.发送支付请求，及获取支付请求结果：" + resJson.toJSONString() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resJson.toJSONString());
		}
		if (!"0000".equalsIgnoreCase(resJson.getString("respcd"))) {
            log.error("[新艾米森]3.3.发送支付请求，及获取支付请求结果：" + resJson.toJSONString() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resJson.toJSONString());
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        HashMap<String, String> result = Maps.newHashMap();
        result.put((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) ?  JUMPURL : QRCONTEXT, resJson.getJSONObject("data").getString("pay_params"));
        payResultList.add(result);
        log.debug("[新艾米森]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[新艾米森]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}