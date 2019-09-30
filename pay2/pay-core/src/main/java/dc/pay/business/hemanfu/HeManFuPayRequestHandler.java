package dc.pay.business.hemanfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 26, 2018
 */
@RequestPayHandler("HEMANFU")
public final class HeManFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeManFuPayRequestHandler.class);

	//参数名称					参数含义				是否必填			参数说明
	//pay_memberid			商户ID				是	
	//pay_orderid			订单号				是				可以为空，为空时系统自动生成订单号，如果不为空请保证订单号不重复，此字段可以为空，但必须参加加密
	//pay_amount			金额					是				订单金额，单位：元，精确到分
	//pay_applydate			订单提交时间			是				订单提交的时间: 如： 2014-12-26 18:18:18
	//pay_bankcode			银行编号				是				收银台模式/非网银直连：填0	网银的直连模式：详见银行编码
	// pay_notifyurl		服务端返回地址			是				服务端返回地址（POST返回数据）
	//pay_callbackurl		页面返回地址			是				页面跳转返回地址（POST返回数据）
	//tongdao				调用通道的模式			是				收银台模式：填0	直联模式：填ZL
	//cashier				是否调用系统收银台		是				该值为调用收银台	cashier=1   银联wap
	//pay_reserved1			扩展字段1				否				此字段在返回时按原样返回
	//pay_reserved2			扩展字段2				否				此字段在返回时按原样返回
	//pay_reserved3			扩展字段3				否				此字段在返回时按原样返回
	//pay_productname		商品名称				否	
	//pay_productnum		商户品数量				否	
	//pay_productdesc		商品描述				否	
	//pay_producturl		商户链接地址			否	
	//pay_md5sign			MD5签名字段			是				请看MD5签名字段格式
	private static final String pay_memberid			="pay_memberid";
	private static final String pay_orderid				="pay_orderid";
	private static final String pay_amount				="pay_amount";
	private static final String pay_applydate			="pay_applydate";
	private static final String pay_bankcode			="pay_bankcode";
	private static final String  pay_notifyurl			="pay_notifyurl";
	private static final String pay_callbackurl			="pay_callbackurl";
	private static final String tongdao					="tongdao";
	private static final String cashier					="cashier";
//	private static final String pay_reserved1			="pay_reserved1";
//	private static final String pay_reserved2			="pay_reserved2";
//	private static final String pay_reserved3			="pay_reserved3";
//	private static final String pay_productname			="pay_productname";
//	private static final String pay_productnum			="pay_productnum";
//	private static final String pay_productdesc			="pay_productdesc";
//	private static final String pay_producturl			="pay_producturl";
//	private static final String pay_md5sign				="pay_md5sign";


	//signature	数据签名	32	是	　
//	private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_applydate, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(pay_bankcode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
                //目前不支持直连模式
                //还有支付宝wap可以接入，那个是只能调收银台模式
                put(tongdao,handlerUtil.isWapOrApp(channelWrapper) ? "0" : "ZL");
//                put(tongdao, "0");
                put(cashier,handlerUtil.isWY(channelWrapper) ? "5" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[合满付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(pay_amount+"=>").append(api_response_params.get(pay_amount)).append("&");
		signSrc.append(pay_applydate+"=>").append(api_response_params.get(pay_applydate)).append("&");
		signSrc.append(pay_bankcode+"=>").append(api_response_params.get(pay_bankcode)).append("&");
		signSrc.append(pay_callbackurl+"=>").append(api_response_params.get(pay_callbackurl)).append("&");
		signSrc.append(pay_memberid+"=>").append(api_response_params.get(pay_memberid)).append("&");
		signSrc.append(pay_notifyurl+"=>").append(api_response_params.get(pay_notifyurl)).append("&");
		signSrc.append(pay_orderid+"=>").append(api_response_params.get(pay_orderid)).append("&");
		signSrc.append("key=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[合满付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		if (handlerUtil.isWY(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
			if (StringUtils.isBlank(resultStr)) {
				log.error("[合满付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
				throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
			}
			JSONObject jsonObject = JSONObject.parseObject(resultStr);
			//只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("returncode") && "00".equalsIgnoreCase(jsonObject.getString("returncode"))  && jsonObject.containsKey("txcode") && StringUtils.isNotBlank(jsonObject.getString("txcode"))) {
                result.put(QRCONTEXT, jsonObject.getString("txcode"));
                result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
            }else {
                log.error("[合满付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[合满付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[合满付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}