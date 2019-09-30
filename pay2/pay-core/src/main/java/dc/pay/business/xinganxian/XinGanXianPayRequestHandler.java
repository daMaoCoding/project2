package dc.pay.business.xinganxian;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 20, 2018
 */
@RequestPayHandler("XINGANXIAN")
public final class XinGanXianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinGanXianPayRequestHandler.class);

	//字段					类型					签名			注释
	//merchant_id			String				是			商户号
	//payment_way			String(20)			是			支付方式，参考附录1通道编码
	//order_amount			float				是			订单金额（例：100.98）
	//source_order_id		String(50)			是			商家订单号
	//goods_name			String(50)			是			商品名称
	//bank_code				String(7)			是			网银直连银行代码
	//client_ip				String(20)			是			客户端ip
	//notify_url			String(200)			是			异步通知url
	//return_url			String(200)			是			页面跳转同步通知地址
	//sign					String				否			签名数据
	private static final String merchant_id						="merchant_id";
	private static final String payment_way						="payment_way";
	private static final String order_amount					="order_amount";
	private static final String source_order_id					="source_order_id";
	private static final String goods_name						="goods_name";
	private static final String bank_code						="bank_code";
	private static final String client_ip						="client_ip";
	private static final String notify_url						="notify_url";
	private static final String return_url						="return_url";

	//signature	数据签名	32	是	　
	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_id, channelWrapper.getAPI_MEMBERID());
                put(payment_way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(client_ip,  HandlerUtil.getRandomIp(channelWrapper));
                put(goods_name,"name");
                put(source_order_id,channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(order_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                if (handlerUtil.isWY(channelWrapper)) {
                	put(payment_way,"3");
                	put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else if (handlerUtil.isYLWAP(channelWrapper)) {
                	put(payment_way,handlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) ? "21" : "10");
				}else {
					put(payment_way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
            }
        };
        log.debug("[新干线]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
    	api_response_params.put("token", channelWrapper.getAPI_KEY());
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        String paramsStr = signSrc.toString();
        //去除最后一个&符
        paramsStr = paramsStr.substring(0,paramsStr.length()-1);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新干线]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
		if (StringUtils.isBlank(resultStr)) {
			log.error("[新干线]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("rec_code") || !"1".equals(resJson.getString("rec_code")) || !"success".equals(resJson.getString("msg"))  ) {
			log.error("[新干线]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(UnicodeUtil.unicodeToString(resultStr));
		}
		String pay_url = resJson.getString("pay_url");
		if (handlerUtil.isWY(channelWrapper) || handlerUtil.isFS(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper)) {
			result.put("html".equals(resJson.getString("pay_type")) ? HTMLCONTEXT : JUMPURL, pay_url);
		}else {
			result.put(QRCONTEXT, pay_url);
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[新干线]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[新干线]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}