package dc.pay.business.threevfu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 2, 2018
 */
@RequestPayHandler("THREEVFU")
public final class ThreeVFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ThreeVFuPayRequestHandler.class);

	//字段名					参数						类型（字节长度）				是否必填			示例值	备注
	//商户号					partnerId				int(3~8)				是				10000	我方分配给每个商户的商户号
	//商户订单号				channelOrderId			String(3~32)			是				123456789	商户订单号，32个字符以内
	//交易起始时间				timeStamp				String(13)				是				1516326720051	支付时间戳。通常为13位长度，后续校验需用到
	//商品名称					body					String(<50)				是				测试/test	商品名称，参数值不要传敏感词汇，如：金币，充值等，可能被风控导致下单失败。
	//交易金额					totalFee				int(10)					是				100	商品价格，以分为单位(网银支付最低为100分即1元，银联wap,银联扫码最低为1000分即10元)
	//交易类型					payType					int(10)					是				101	微信H5:  101
	//签名					sign					String(20~100)			是				47b8795f78fd51cebc11ad38c4483a1b	验签使用，具体规则请看文档下方说明
	//异步回调通知地址			notifyUrl				String					是				https://www.baidu.com/	支付完成通知回调地址
	//页面跳转通知页面路径			returnUrl				String					是				https://www.baidu.com/	支付结果页面通知地址
	//银行代号					bankSegment				String					是				1003	注：此参数为网关支付时是必传字段，其他支付类型无需传。银行代号，具体请看支持银行表
    private static final String partnerId				="partnerId";
    private static final String channelOrderId			="channelOrderId";
    private static final String timeStamp				="timeStamp";
    private static final String body					="body";
    private static final String totalFee				="totalFee";
    private static final String payType					="payType";
    private static final String notifyUrl				="notifyUrl";
    private static final String returnUrl				="returnUrl";
    private static final String bankSegment				="bankSegment";
    private static final String key						="key";
//    private static final String sign					="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(partnerId, channelWrapper.getAPI_MEMBERID());
            	put(timeStamp ,System.currentTimeMillis()+"");
                put(channelOrderId,channelWrapper.getAPI_ORDER_ID());
                put(totalFee ,channelWrapper.getAPI_AMOUNT());
                put(body ,"test");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl ,channelWrapper.getAPI_WEB_URL());
                if (handlerUtil.isWY(channelWrapper)) {
                	put(payType,"114");
                    put(bankSegment,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}else {
					put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
            }
        };
        log.debug("[3V支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(partnerId+"=").append(api_response_params.get(partnerId)).append("&");
		signSrc.append(timeStamp+"=").append(api_response_params.get(timeStamp)).append("&");
		signSrc.append(totalFee+"=").append(api_response_params.get(totalFee)).append("&");
		signSrc.append(key+"=").append(channelWrapper.getAPI_KEY().split("-")[0]);
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"utf-8");
        log.debug("[3V支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5)+"，参数："+paramsStr);
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
    	String tmpStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
    	if (StringUtils.isBlank(tmpStr)) {
    		log.error("[3V支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
    		throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
    	}
    	JSONObject jsonObject = JSONObject.parseObject(tmpStr);
    	//支付成功返回：0；支付失败则返回其它
    	if (!jsonObject.containsKey("return_code") || !"0000".equals(jsonObject.getString("return_code"))) {
    		log.error("[3V支付]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL());
    		throw new PayException(tmpStr);
    	}
        String dataStr = jsonObject.getString("payParam");
        if (StringUtils.isBlank(dataStr)) {
        	log.error("[3V支付]-[请求支付]-3.4.发送支付请求，获取支付请求返回值异常:payParam返回空"+",参数："+JSON.toJSONString(payParam));
        	throw new PayException("payParam返回空"+",参数："+JSON.toJSONString(payParam));
        }
        JSONObject data = JSONObject.parseObject(dataStr);
        Map<String,String> result = Maps.newHashMap();
        //按不同的请求接口，向不同的属性设置值
        result.put((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) ? JUMPURL : QRCONTEXT, (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) ? data.getString("pay_info") : QRCodeUtil.decodeByUrl(data.getString("code_img_url")));
        result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[3V支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[3V支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}