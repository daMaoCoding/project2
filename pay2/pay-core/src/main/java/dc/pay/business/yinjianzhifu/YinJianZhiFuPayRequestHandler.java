package dc.pay.business.yinjianzhifu;

import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.*;
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;


/**
 * @author cobby
 * Jan 21, 2019
 */
@RequestPayHandler("YINJIANZHIFU")
public final class YinJianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YinJianZhiFuPayRequestHandler.class);

//字段名            变量名                类型             必填        描述
//商户号            mch_id                String(32)        是         商户号，唯一标识
//商品描述          body                  String(32)        否         商品或支付单简要描述
//商品详情          detail                String(8192)      否         商品详情
//附加数据          attach                String(127)       否         附加数据
//商户订单号        out_trade_no          String(32)        是         商户系统内部的订单号,32 个字符内、可包含字母,其他说 明见商户订单号
//总金额            amount                Int               是         订单总金额，单位为分，只能为整数，详见 支付金额
//货币类型          fee_type              String(16)        否         默认人民币：CNY，其他值列表详见 货币类型
//IP终端          spbill_create_ip      String(16)        是         调用支付API的机器IP
//商品标记          goods_tag             String(32)        否         商品标记，代金券或立减优惠功能的参数
//通知地址          notify_url            String(256)       是         接收支付结果异步通知回调地址，PC 网站必填，POS 机器扫码支付填写空字符串即可。示例：http://www.xxxx.com
//页面回调地址      return_url            String(256)       是         页面回调的地址，该地址不带支付结果参数，最终支付 结果需要调用查询接口进行获取
//支付类型          payment_type          String(32)        是         详细说明见 1.3 参数规定
//指定支付方式      limit_pay             String(32)        否         no_credit：指定不能使用信用卡支付
//银行编码          bank_type             String            否         当payment_type为trade.gateway 时必填 详细说明见附件-银行编码
//随机字符串        nonce_str             String(32)        是         随机字符串，不长于32位
//签名类型          sign_type             String            否         签名类型，目前支持HMAC-SHA256和MD5，默认为MD5
//签名              sign                  String(32)        是         签名，详见 签名生成算法

    private static final String  mch_id                       ="mch_id";
    private static final String  body                         ="body";
    //    private static final String  detail                       ="detail";
//    private static final String  attach                       ="attach";
    private static final String  out_trade_no                 ="out_trade_no";
    private static final String  amount                       ="amount";
    private static final String  fee_type                     ="fee_type";
    private static final String  spbill_create_ip             ="spbill_create_ip";
    //    private static final String  goods_tag                    ="goods_tag";
    private static final String  notify_url                   ="notify_url";
    private static final String  return_url                   ="return_url";
    private static final String  payment_type                 ="payment_type";
    //    private static final String  limit_pay                    ="limit_pay";
    private static final String  bank_type                    ="bank_type";
    private static final String  nonce_str                    ="nonce_str";
    private static final String  sign_type                    ="sign_type";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = new TreeMap<String, String>() {
			{
				put(mch_id, channelWrapper.getAPI_MEMBERID());
				put(body,"body");
				put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
				put(amount,  channelWrapper.getAPI_AMOUNT());
				put(fee_type,"CNY");
				put(spbill_create_ip,channelWrapper.getAPI_Client_IP());
				put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
				put(return_url,channelWrapper.getAPI_WEB_URL());
				put(nonce_str,handlerUtil.getRandomStr(5));
				put(payment_type,handlerUtil.isWY(channelWrapper) ? "" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				put(bank_type,handlerUtil.isWY(channelWrapper) ? channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() : "");
			}
		};
		log.debug("[银简支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		StringBuilder signSrc = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
			if (!sign_type.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
				signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
		}
		signSrc.append("key="+channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[银简支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
		return signMd5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
		if (StringUtils.isBlank(resultStr)) {
			log.error("[银简支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
			throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("return_code") || !"SUCCESS".equals(resJson.getString("return_code"))) {
			log.error("[银简支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper) || handlerUtil.isFS(channelWrapper)) {
			result.put(JUMPURL, resJson.getString("prepay_url"));
		}else{
			result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(resJson.getString("code_img_url")));
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[银简支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
		log.debug("[银简支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
}