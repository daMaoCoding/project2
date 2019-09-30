package dc.pay.business.jinduo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 * Jul 19, 2018
 */
@RequestPayHandler("JINDUO")
public final class JinDuoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinDuoPayRequestHandler.class);

    //参数名                      参数                   可空             加入签名             说明
    //商户ID                      partner                 N                 Y                   商户id,由分配
    //银行类型                    banktype                N                 Y                   银行类型，具体参考附录1,default为跳转到接口进行选择支付
    //金额                        paymoney                N                 Y                   单位元（人民币）
    //商户订单号                  ordernumber             N                 Y                   商户系统订单号，该订单号将作为接口的返回数据。该值需在商户系统内唯一
    //订单标题                    subject                 N                 Y                   订单标题，下行中会原样返回。若该值包含中文，请注意编码
    //下行异步通知地址            callbackurl             N                 Y                   下行异步通知的地址，需要以http://开头且没有任何参数
    //下行同步通知地址            hrefbackurl             Y                 N                   下行同步通知过程的返回地址(在支付完成后接口将会跳转到的商户系统连接地址)。注：若提交值无该参数，或者该参数值为空，则在支付完成后，接口将不会跳转到商户系统。
    //MD5签名                     sign                    N                 N                   32位小写MD5签名值，GB2312编码
    private static final String partner                 ="partner";
    private static final String banktype                ="banktype";
    private static final String paymoney                ="paymoney";
    private static final String ordernumber             ="ordernumber";
    private static final String subject                 ="subject";
    private static final String callbackurl             ="callbackurl";
    private static final String hrefbackurl             ="hrefbackurl";
    
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ordernumber,channelWrapper.getAPI_ORDER_ID());
                put(subject,"0");
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(hrefbackurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[金多]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
		signSrc.append(banktype+"=").append(api_response_params.get(banktype)).append("&");
		signSrc.append(paymoney+"=").append(api_response_params.get(paymoney)).append("&");
		signSrc.append(ordernumber+"=").append(api_response_params.get(ordernumber)).append("&");
		signSrc.append(subject+"=").append(api_response_params.get(subject)).append("&");
		signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl));
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[金多]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWapOrApp(channelWrapper)) {
			String html = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString();
			result.put(HTMLCONTEXT, html);
		}
//		else{
//			String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
//			if (StringUtils.isBlank(resultStr)) {
//				log.error("[金多]3.1.发送支付请求，获取支付请求返回值异常:返回空");
//				throw new PayException("返回空");
//			}
//			System.out.println("请求返回=========>"+resultStr);
//			JSONObject resJson = JSONObject.parseObject(resultStr);
//			
//	         //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                //按不同的请求接口，向不同的属性设置值
//                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name())||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAPAPP_.name())) {
//                    result.put(JUMPURL, resJson.getString("barCode"));
//                }else{
//                    result.put(QRCONTEXT, resJson.getString("barCode"));
//                }
////                result.put("第三方返回",resJson.toString()); //保存全部第三方信息，上面的拆开没必要
//            }else {
//                log.error("[通扫]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//		}
		payResultList.add(result);
		log.debug("[金多]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[金多]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}