package dc.pay.business.tongxin;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 2, 2018
 */
@RequestPayHandler("TONGXIN")
public final class TongXinPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongXinPayRequestHandler.class);

    //公共
	//参数名				描述					属性				请求			说明
	//requestId			请求订单号				Str-max32			M			保证唯一
	//orgId				机构号				Str-max16			M			平台分配机构号
	//timestamp			请求时间戳				Str-max14			M			格式:yyyyMMddHHmmss
	//productId			产品ID				Str-max10			M			产品ID对应关系详见产品ID对应表
	//businessData		业务交互数据			JSON				M			对应产品交互业务数据
	//signData			数据签名				Str-max32			M			签名规则详见签名算法
	//dataSignType		业务数据加密方式			Int-max1			C			上送的业务参数是否加密，为空默认为明文传输	0-不加密，明文传输	1-DES加密，密文传输
	private static final String requestId					="requestId";
	private static final String orgId						="orgId";
	private static final String timestamp					="timestamp";
	private static final String productId					="productId";
	private static final String businessData				="businessData";
	private static final String dataSignType				="dataSignType";
	
	//扫码
	//参数名				描述				属性					请求			说明
	//merno				商户号			Str-max32			M			平台进件返回商户号
	//bus_no			业务编号			Str-max10			M			详见bus_no列表
	//amount			交易金额			Str-max32			M			交易金额，单位分
	//goods_info		商品名称			Str-max32			M			商品名称
	//order_id			订单号			Str-max32			M			商户自定义订单号
	//return_url		前端通知地址		Str-max255			M			前端跳转回调地址
	//notify_url		后台通知地址		Str-max32			M			后台通知回调地址
	private static final String merno						="merno";
	private static final String bus_no						="bus_no";
	private static final String amount						="amount";
	private static final String goods_info					="goods_info";
	private static final String order_id					="order_id";
	private static final String return_url					="return_url";
	private static final String notify_url					="notify_url";
	
	//网银 
	//参数名				描述				属性				请求			说明
	//merno				商户号			Str-max32			M			平台提供的商户号
	//bus_no			业务编号			Str-max10			M			固定0499
	//amount			交易金额			Str-max32			M			交易金额，单位(分)
	//goods_info		商品信息			Str-max32			M			支付商品信息
	//order_id			订单号			Str-max32			M			商户自定义订单号
	//cardname			银行名称			Str-max10			M			银行名称，参考银行卡支持列表
	//bank_code			银行编码			Str-max10			M			银行编码，参考银行卡支持列表
	//notify_url		商户号			Str-max32			M			后端跳转回调地址
	//card_type			卡类型			int-1				M			卡类型 1-储蓄卡 2-信用卡
	//channelid			账户类型			Int-1				M			账户类型 1-对私 2-对公
	private static final String cardname					="cardname";
	private static final String bank_code					="bank_code";
	private static final String card_type					="card_type";
	private static final String channelid					="channelid";
	
	//signature	数据签名	32	是	　
	private static final String signature					="signData";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
		String api_MEMBERID = channelWrapper.getAPI_MEMBERID();
    	if (null == api_MEMBERID || !api_MEMBERID.contains("&") || api_MEMBERID.split("&").length != 2) {
            log.error("[同心]-[请求支付]-“支付通道商号”输入数据格式为：商号&机构号" );
            throw new PayException("[同心]-[请求支付]-“支付通道商号”输入数据格式为：商号&机构号" );
		}
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(merno,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            	put(amount,channelWrapper.getAPI_AMOUNT());
            	put(goods_info,"name");
            	put(order_id,channelWrapper.getAPI_ORDER_ID());
            	put(return_url,channelWrapper.getAPI_WEB_URL());
            	put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	if (handlerUtil.isWY(channelWrapper)) {
            		put(bus_no,"0499");
            		put(cardname,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            		put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            		put(card_type,"1");
            		put(channelid,"1");
				}else{
					//二维码
//					put(merno,channelWrapper.getAPI_MEMBERID().split("&")[0]);
					put(bus_no,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
//					put(amount,channelWrapper.getAPI_AMOUNT());
//					put(goods_info,"name");
//					put(order_id,channelWrapper.getAPI_ORDER_ID());
//					put(return_url,channelWrapper.getAPI_WEB_URL());
//					put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
				}
            }
        };
        log.debug("[同心]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		String string = JSON.toJSONString(MapUtils.sortMapByKeyAsc2(api_response_params));
		signSrc.append(businessData+"=").append(string).append("&");
		signSrc.append(dataSignType+"=").append("0").append("&");
		signSrc.append(orgId+"=").append(channelWrapper.getAPI_MEMBERID().split("&")[1]).append("&");
		if (handlerUtil.isWY(channelWrapper)) {
			signSrc.append(productId+"=").append("0500").append("&");
		}else {
			signSrc.append(productId+"=").append(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]).append("&");
		}
		signSrc.append(requestId+"=").append(channelWrapper.getAPI_ORDER_ID()).append("&");
		signSrc.append(timestamp+"=").append(DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
        signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[同心]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		 Map<String, String> map = new TreeMap<String, String>() {
	            {
	            	put(requestId,channelWrapper.getAPI_ORDER_ID());
	            	put(orgId,channelWrapper.getAPI_MEMBERID().split("&")[1]);
	             	put(timestamp,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
	        		if (handlerUtil.isWY(channelWrapper)) {
	        			put(productId,"0500");
	        		}else {
	        			put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
	        		}
	             	put(businessData,JSON.toJSONString(payParam));
	             	put(signature,pay_md5sign);
	             	put(dataSignType,"0");
	            }
		 };
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
		if (StringUtils.isBlank(resultStr)) {
			log.error("[同心]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("respCode") || !"00".equals(resJson.getString("respCode"))) {
			log.error("[同心]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		if (!resJson.containsKey("key") || (!"00".equals(resJson.getString("key")) && !"05".equals(resJson.getString("key")))) {
			log.error("[同心]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		JSONObject myresult = JSONObject.parseObject(resJson.getString("result"));
		result.put(handlerUtil.isWY(channelWrapper) ? JUMPURL : QRCONTEXT, myresult.getString("url"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[同心]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[同心]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}