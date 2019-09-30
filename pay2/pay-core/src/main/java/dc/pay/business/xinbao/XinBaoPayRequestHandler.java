package dc.pay.business.xinbao;

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
import dc.pay.business.yunbao.YunBaoUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 16, 2018
 */
@RequestPayHandler("XINBAO")
public final class XinBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinBaoPayRequestHandler.class);

    //商户ID			P_UserId		是	String(32)	必须 (请联系商务配置 )或登录云宝后台，商户设置，商户资料里面查询商户ID字段
    private static final String P_UserId	 ="P_UserId";
	//商户订单号		P_OrderId		是	String(12-36)	在商户系统中唯一
    private static final String P_OrderId	 ="P_OrderId";
	//面值			P_FaceValue		是	String		面值：必须为整数，单位：元
    private static final String P_FaceValue	 ="P_FaceValue";
	//用户编号			P_CustormId		是	String		P_CustormId 加密方式：校验码_MD5(商户ID|安全码|校验码)，校验码为用户自定义的任意两位数，例如：P_CustormId ="01"+"_"+ Md5(P_UserId + "|" + salfStr + "|" + "01")
    private static final String P_CustormId	 ="P_CustormId";
	//充值渠道			P_Type			是	String(32)	2:微信扫码 4:支付宝扫码 5:快捷支付 6:QQ扫码 9:银联扫码 10:京东支付 12:微信H5(无返回参数，直接跳转支付页面) 16:京东快捷
    private static final String P_Type		 ="P_Type";
	//SDK 版本		P_SDKVersion	是	String(32)	默认3.1.3
    private static final String P_SDKVersion ="P_SDKVersion";
	//充值类型			P_RequestType	是	String(32)	0:web 1:wap 2:iPhone 3:Android
    private static final String P_RequestType="P_RequestType";
	//产品名称			P_Subject		是	String(127)	产品名称
    private static final String P_Subject	 ="P_Subject";
	//充值后网页跳转地址	P_Result_URL	是	String		充值后网页跳转地址
    private static final String P_Result_URL ="P_Result_URL";
	//充值状态通知地址	P_Notify_URL	是	String		充值状态通知地址
    private static final String P_Notify_URL ="P_Notify_URL";
//	//签名认证串		P_PostKey		是	String		签名认证串
//    private static final String P_PostKey	 ="P_PostKey";

    private static final String NM = "01";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	//P_CustormId 加密方式：校验码_MD5(商户ID|安全码|校验码)，校验码为用户自定义的任意两位数，例如：P_CustormId ="01"+"_"+ Md5(P_UserId + "|" + salfStr + "|" + "01")
    	String p_custormId =  NM + "_" + YunBaoUtil.Md5(channelWrapper.getAPI_MEMBERID() + "|" + channelWrapper.getAPI_KEY() + "|" + NM);
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(P_UserId, channelWrapper.getAPI_MEMBERID());
            	put(P_OrderId,channelWrapper.getAPI_ORDER_ID());
            	//面值：必须为整数，单位：元
            	put(P_FaceValue,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(P_CustormId,  p_custormId);
            	put(P_Type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(P_SDKVersion,"3.1.3");
            	//充值类型			P_RequestType	是	String(32)	0:web 1:wap 2:iPhone 3:Android
            	//目前使用固定值
            	put(P_RequestType,"0");
            	put(P_Subject,"name");
            	put(P_Result_URL,channelWrapper.getAPI_WEB_URL());
            	put(P_Notify_URL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[辛宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//1、以字段值和格式进行签名 P_UserId+"|"+P_OrderId+"|"+P_FaceValue+"|"+P_Type+"|"+P_SDKVersion+"|"+P_RequestType
    	//2、签名原始串中，字段名和字段值都采用原始值，不进行URL Encode。
    	//3、 平台返回的应答或通知消息可能会由于升级增加参数，请验证应答签名时注意允许这种情况。
    	String signStr = api_response_params.get(P_UserId) + "|" + api_response_params.get(P_OrderId) + "|" + api_response_params.get(P_FaceValue) + "|" + api_response_params.get(P_Type) + "|" + api_response_params.get(P_SDKVersion) + "|"  + api_response_params.get(P_RequestType);
        String signMd5 = YunBaoUtil.Md5(signStr + "|" + channelWrapper.getAPI_KEY());
        log.debug("[辛宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
			if (StringUtils.isBlank(resultStr)) {
				log.error("[辛宝]3.1.发送支付请求，获取支付请求返回值异常:返回空");
				throw new PayException("返回空");
			}
//			{"charset":"UTF-8","status":"114","message":"系统配置错误:支付通道被禁用！支付方式：支付宝扫码"}
			JSONObject resJson = JSONObject.parseObject(resultStr);
//			{"version":"2.0","charset":"UTF-8","sign_type":"MD5","status":"0","message":"交易成功","result_code":"0","err_code":"0","err_msg":"交易成功","P_ChannelId":"867","type":"6","code_url":"https://qpay.qq.com/qr/52ed3056","code_img_url":"http://orderapi4.chujiuf.top/qrc.do?content=https://qpay.qq.com/qr/52ed3056","total_fee":"300.00","out_trade_no":"emonOfI2FYuOwTKLVct","sign":"b0ddeccb42376503273299ca572d6688"}
			if (resJson == null || !resJson.containsKey("result_code") || !"0".equals(resJson.getString("result_code"))) {
				log.error("[辛宝]3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			result.put(QRCONTEXT, resJson.getString("code_url"));
		}
		payResultList.add(result);
		log.debug("[辛宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[辛宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}