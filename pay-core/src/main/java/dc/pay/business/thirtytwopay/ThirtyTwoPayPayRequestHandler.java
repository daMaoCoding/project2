package dc.pay.business.thirtytwopay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 17, 2018
 */
@RequestPayHandler("THIRTYTWOPAY")
public final class ThirtyTwoPayPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ThirtyTwoPayPayRequestHandler.class);

	//P_UserID			必填商户编号如1000001
	//P_OrderID			必填商户定单号（要保证唯一），长度最长32 字符
    //P_CardID			非必填点卡交易时的充值卡卡号
    //P_CardPass		非必填点卡交易时的充充值卡卡密
	//P_FaceValue		必填申明交易金额
	//P_ChannelID		必填支付方式，支付方式编码：参照附录6.1
	//P_Price			必填商品售价
	//P_Description		非必填支付方式为网银时的银行编码，参照附录6.2
	//P_Result_URL		必填支付后异步通知地址，URL 参数是以http://或https://开头的完整URL 地址(后台处理) 提交的url 地址必须外网能访问到,否则无法通知商户
	//P_PostKey			必填MD5 签名结果
	private static final String P_UserID				="P_UserID";
	private static final String P_OrderID				="P_OrderID";
	private static final String P_CardID				="P_CardID";
	private static final String P_CardPass				="P_CardPass";
	private static final String P_FaceValue				="P_FaceValue";
	private static final String P_ChannelID				="P_ChannelID";
	private static final String P_Price					="P_Price";
	private static final String P_Description			="P_Description";
	private static final String P_Result_URL			="P_Result_URL";
//	private static final String P_PostKey				="P_PostKey";

	//signature	数据签名	32	是	　
//	private static final String signature  ="P_PostKey";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(P_UserID, channelWrapper.getAPI_MEMBERID());
                put(P_OrderID,channelWrapper.getAPI_ORDER_ID());
                put(P_FaceValue,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(P_ChannelID,(handlerUtil.isWY(channelWrapper) && !handlerUtil.isWebWyKjzf(channelWrapper)) ? "1" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(P_Price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(P_Description,handlerUtil.isWY(channelWrapper) ? channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() : "");
                put(P_Result_URL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[32Pay]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
    	String paramsStr = api_response_params.get(P_UserID) + "|" + api_response_params.get(P_OrderID) + "|" + 
    					(null == api_response_params.get(P_CardID)  ? "" : api_response_params.get(P_CardID)) + "|" + (null == api_response_params.get(P_CardPass) ? "" : api_response_params.get(P_CardPass)) + "|" + 
    					api_response_params.get(P_FaceValue) + "|"  + api_response_params.get(P_ChannelID);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr+"|"+channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[32Pay]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        String qrtext=null;
        String resultStr = null;
		if (handlerUtil.isFS(channelWrapper) || HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            payResultList.add(result);
		}else{
		    try {
                resultStr =handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(),channelWrapper.getAPI_ORDER_ID(),payParam).asXml();
                Elements elements = Jsoup.parse(resultStr).select("img");
                if (null != elements &&  elements.size() > 0 && StringUtils.isNotBlank(elements.first().attr("src"))) {
                    String src =elements.first().attr("src") ;
                    qrtext = HandlerUtil.UrlDecode(src.substring(src.indexOf("url=")+4));
                }else {throw new PayException(resultStr);}
                if (StringUtils.isBlank(qrtext)) { throw new PayException(resultStr); }else {
                    result.put(QRCONTEXT, qrtext);
                    payResultList.add(result);
                }
            }catch (Exception e){ if(StringUtils.isBlank(resultStr)){throw new PayException(EMPTYRESPONSE);}else{throw new PayException(resultStr); } }
		}
		log.debug("[32Pay]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[32Pay]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}