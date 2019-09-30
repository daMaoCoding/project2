package dc.pay.business.lakazhifu;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
@RequestPayHandler("LAKAZHIFU")
public final class LaKaZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LaKaZhiFuPayRequestHandler.class);
/*
    	参数名					字段名称			是否必填			说明
    mer_id					商户号				Y	   		支付分配给商户的商户号
    timestamp				请求时间			Y			时间戳,格式yyyy-MM-dd HH:mm:ss
    terminal				支付类型			Y			请看4.1.2 支付类型表
    version					版本号				Y			01
    amount					金额				Y			代付金额(单位 分)
    backurl					同步返回的url		Y			支付成功的同步返回的url
    failUrl					同步返回的url		Y	 		支付失败的同步返回的url
    ServerUrl				异步返回的url		Y	 		以form-data形式将回调的数据post提交到该地址  商户处理数据
    businessnumber			商品订单号			Y	 		商品订单号（['A~Z,a~z,0~9']组成的10到64位字符串）
    goodsName				商品名称（描述）		Y			商品名称（描述）,建议使用英文
    sign					签名				Y			签名
    sign_type				签名算法类型			Y			默 认 md5
*/
    private static final String mer_id			= "mer_id";					//商户号			Y	   		支付分配给商户的商户号
    private static final String timestamp		= "timestamp";				//请求时间			Y			时间戳,格式yyyy-MM-dd HH:mm:ss
    private static final String terminal		= "terminal";				//支付类型			Y			请看4.1.2 支付类型表
    private static final String version			= "version";				//版本号			Y			01
    private static final String amount			= "amount";					//金额			Y			代付金额(单位 分)
    private static final String backurl			= "backurl";				//同步返回的url		Y			支付成功的同步返回的url
    private static final String failUrl			= "failUrl";				//同步返回的url		Y	 		支付失败的同步返回的url
    private static final String ServerUrl		= "ServerUrl";				//异步返回的url		Y	 		以form-data形式将回调的数据post提交到该地址  商户处理数据
    private static final String businessnumber	= "businessnumber";			//商品订单号		Y	 		商品订单号（['A~Z,a~z,0~9']组成的10到64位字符串）
    private static final String goodsName		= "goodsName";				//商品名称（描述）		Y			商品名称（描述）,建议使用英文
    private static final String sign			= "sign";					//签名			Y			签名
    private static final String sign_type		= "sign_type";				//签名算法类型		Y			默 认 md5

    /**
     *	 參數封裝
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = Maps.newHashMap();
    	
    	payParam.put(mer_id,channelWrapper.getAPI_MEMBERID());
    	payParam.put(timestamp,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
    	payParam.put(terminal,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
    	payParam.put(version,"01");
    	payParam.put(amount,channelWrapper.getAPI_AMOUNT());
    	payParam.put(backurl,channelWrapper.getAPI_WEB_URL());
    	payParam.put(failUrl,channelWrapper.getAPI_WEB_URL());
    	payParam.put(ServerUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    	payParam.put(businessnumber,channelWrapper.getAPI_ORDER_ID());
    	payParam.put(goodsName,"name");
    	payParam.put(sign_type,"md5");

        log.debug("[拉卡支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
            if (!sign_type.equals(paramKeys.get(i)) && StringUtils.isNotBlank(params.get(paramKeys.get(i)))) {
    			sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
            }
		}
		sb.append(channelWrapper.getAPI_KEY());
		pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()); // 进行MD5运算，再将得到的字符串所有字符转换为大写
		log.debug("[拉卡支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
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
            log.error("[拉卡支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        JSONObject resJson = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("result") && "success".equalsIgnoreCase(resJson.getString("result")) 
                && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
            try {
            	JSONObject jsonObject = JSONObject.parseObject(resJson.getString("data"));
                String code_url = jsonObject.getString("trade_qrcode");
                result.put(JUMPURL, URLDecoder.decode(code_url, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                log.error("[拉卡支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }else {
            log.error("[拉卡支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[拉卡支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[拉卡支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}