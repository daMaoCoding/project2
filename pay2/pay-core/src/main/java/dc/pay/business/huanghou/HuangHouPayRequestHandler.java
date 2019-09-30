package dc.pay.business.huanghou;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 22, 2018
 */
@RequestPayHandler("HUANGHOU")
public final class HuangHouPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuangHouPayRequestHandler.class);

    //接口名字		apiName
    private static final String apiName		  ="apiName";
    //接口版本		apiVersion
    private static final String apiVersion	  ="apiVersion";
    //商户(合作伙伴)ID	platformID
    private static final String platformID	  ="platformID";
    //商户账号		merchNo
    private static final String merchNo		  ="merchNo";
    //商户订单号		orderNo
    private static final String orderNo		  ="orderNo";
    //交易日期		tradeDate
    private static final String tradeDate	  ="tradeDate";
    //订单金额		amt
    private static final String amt		      ="amt";
    //支付结果通知地址	merchUrl
    private static final String merchUrl	  ="merchUrl";
    //商户参数		merchParam
    private static final String merchParam	  ="merchParam";
    //交易摘要		tradeSummary
    private static final String tradeSummary  ="tradeSummary";
    //银行代码		不进行签名，支付系统根据该银行代码直接跳转银行网银，不输或输入的银行代码不存在则展示支付首页让用户选择支付方式。
    private static final String bankCode	  ="bankCode";
    //选择支付方式	不进行签名，根据选择的支付方式直接对应页面。不输入或选择支付方式不存在则认为是该商户所拥有的全部方式。
    private static final String choosePayType ="choosePayType";
    //客户端IP	customerIP	ans（..20）	必输，客户端ip地址
	private static final String customerIP	  ="customerIP";

    /**
     * 封装第三方所需要的参数
     * 
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Feb 21, 2018
	 */
	@Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(apiName,HandlerUtil.isWY(channelWrapper) ? "WEB_PAY_B2C" : "WAP_PAY_B2C");
            	put(apiVersion,"1.0.0.0");
            	put(platformID, channelWrapper.getAPI_MEMBER_PLATFORMID());
            	put(merchNo, channelWrapper.getAPI_MEMBERID());
            	put(orderNo,channelWrapper.getAPI_ORDER_ID());
            	//YYYYMMDD
            	put(tradeDate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                put(amt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(merchUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                //merchParam可以为空，但必须存在！
                put(merchParam,"abcd");
                //如：商品名称|商品数量
                put(tradeSummary,"pay");
                //银行代码		不进行签名，支付系统根据该银行代码直接跳转银行网银，不输或输入的银行代码不存在则展示支付首页让用户选择支付方式。
                put(bankCode,HandlerUtil.isYLKJ(channelWrapper) ? "" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
    			/**
                 * bankCode为空，提交表单后浏览器在新窗口显示支付系统收银台页面，在这里可以通过账户余额支付或者选择银行支付；
                 * bankCode不为空，取值只能是接口文档中列举的银行代码，提交表单后浏览器将在新窗口直接打开选中银行的支付页面。
                 * 无论选择上面两种方式中的哪一种，支付成功后收到的通知都是同一接口。
                 **/
                //选择支付方式	不进行签名，根据选择的支付方式直接对应页面。不输入或选择支付方式不存在则认为是该商户所拥有的全部方式。
                put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[皇后]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    
    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Feb 21, 2018
     */
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		// 输入数据组织成字符串
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(apiName+"=").append(api_response_params.get(apiName)).append("&");
		signSrc.append(apiVersion+"=").append(api_response_params.get(apiVersion)).append("&");
		signSrc.append(platformID+"=").append(api_response_params.get(platformID)).append("&");
		signSrc.append(merchNo+"=").append(api_response_params.get(merchNo)).append("&");
		signSrc.append(orderNo+"=").append(api_response_params.get(orderNo)).append("&");
		signSrc.append(tradeDate+"=").append(api_response_params.get(tradeDate)).append("&");
		signSrc.append(amt+"=").append(api_response_params.get(amt)).append("&");
		signSrc.append(merchUrl+"=").append(api_response_params.get(merchUrl)).append("&");
		signSrc.append(merchParam+"=").append(api_response_params.get(merchParam)).append("&");
		signSrc.append(tradeSummary+"=").append(api_response_params.get(tradeSummary));
		if (StringUtils.isNotBlank(api_response_params.get(customerIP))) {
			signSrc.append("&").append(customerIP+"=").append(api_response_params.get(customerIP));
		}
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[皇后]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
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
     * Feb 21, 2018
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        Map<String,String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
			payResultList.add(result);
        }else{
        	log.error("[皇后]3.1.发送支付请求，获取支付请求返回值异常:返回null，入参数："+JSON.toJSONString(payParam));
    		throw new PayException("目前只支持网银、银联，入参数："+JSON.toJSONString(payParam));
        }
        log.debug("[皇后]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
	 * @param resultListMap
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Feb 21, 2018
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
        log.debug("[皇后]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}