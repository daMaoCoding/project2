package dc.pay.business.juchuang;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 11, 2018
 */
@RequestPayHandler("JUCHUANG")
public final class JuChuangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuChuangPayRequestHandler.class);

    //快捷支付		A、快捷支付表格中所有参数都必须提交。即使参数值为空，也需要提交。
    
    //qq、银联
	
	//支付宝
	//外部订单号		outOrderNo	String	N	商户系统的订单编号
	private static final String outOrderNo  ="outOrderNo";
	//商品名称		goodsClauses	String	N	商户系统的商品名称
	private static final String goodsClauses  ="goodsClauses";
	//交易金额		tradeAmount	double	N	商户 商品价格（元）支持小数
	private static final String tradeAmount  ="tradeAmount";
	//商户code		code		String	N	点击头像，查看code
	private static final String code  ="code";
	//notifyUrl	通知地址	50	否	交易成功，则给商户发送异步通知。
	private static final String notifyUrl  ="notifyUrl";
	//支付类型		payCode		String	N	alipay
	private static final String payCode  ="payCode";

	private static final String key  ="key";
	
    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Mar 11, 2018
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(outOrderNo,channelWrapper.getAPI_ORDER_ID());
            	put(goodsClauses,"goodsClauses");
            	put(tradeAmount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(code, channelWrapper.getAPI_MEMBERID());
            	put(payCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[聚创]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Mar 11, 2018
     */
    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(code+"=").append(api_response_params.get(code)).append("&");
		signSrc.append(goodsClauses+"=").append(api_response_params.get(goodsClauses)).append("&");
		signSrc.append(notifyUrl+"=").append(api_response_params.get(notifyUrl)).append("&");
		signSrc.append(outOrderNo+"=").append(api_response_params.get(outOrderNo)).append("&");
		signSrc.append(payCode+"=").append(api_response_params.get(payCode)).append("&");
		signSrc.append(tradeAmount+"=").append(api_response_params.get(tradeAmount)).append("&");
		signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚创]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    /**
     * 生成返回给RequestPayResult对象detail字段的值
     * 
     * @param payParam
     * @param pay_md5sign
     * @return
     * @throws PayException
     * @author andrew
     * Mar 11, 2018
     */
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8").trim();
		if (StringUtils.isBlank(resultStr)) {
			log.error("[聚创]3.1.发送支付请求，获取支付请求返回值异常:返回空");
			throw new PayException("第三方返回异常:返回空");
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("payState") || !"success".equals(resJson.getString("payState"))) {
			String unicodeToString = UnicodeUtil.unicodeToString(resultStr);
			log.error("[聚创]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(unicodeToString);
		}
		result.put(QRCONTEXT, resJson.getString("url"));
		payResultList.add(result);
		log.debug("[聚创]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
		return payResultList;
	}
    
    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Mar 11, 2018
     */
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
        log.debug("[聚创]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}