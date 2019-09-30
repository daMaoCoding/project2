package dc.pay.business.wufu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

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
 * Dec 18, 2017
 */
@RequestPayHandler("WUFU")
public final class WuFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WuFuPayRequestHandler.class);

	//wap跳转属性
	private static final String JUMPURL = "JUMPURL";
	//  svcName			服务名称		是	String(36)	gatewayPay 网银支付
    private static final String svcName  ="svcName";
    //	merId			商户编号		是	String(36)	商户编号	
    private static final String merId  ="merId";
    //	merchOrderId	商户订单号		是	String(36)	提交的订单号必须在自身账户交易中唯一
    private static final String merchOrderId  ="merchOrderId";
    //	tranType		交易类型		是	String(36)	交易类型见表 2.1
    private static final String tranType  ="tranType";
    //	pName			商品名称		是	String(50)	商品名称
    private static final String pName  ="pName";
    //	amt				订单金额		是	String(50)	单位分
    private static final String amt  ="amt";
    //	notifyUrl		异步通知地址	否	String(200)	交易成功后发送异步通知给此地址
    private static final String notifyUrl  ="notifyUrl";
    //	retUrl			页面返回地址	否	String(200)	交易成功后浏览器会跳转至此地址
//    private static final String retUrl  ="retUrl";
    //	showCashier		是否显示收银	1 是 0  否		1 是 0  否		ALIPAY_NATIVE  ,WEIXIN_NATIVE,QQ_NATIVE,UNIONPAY_NATIVE 类型的交易下单成功后默认返回 json 格式	原生支付链接串,如需直接在第三方方收银台显示二维码请填入值为 1
    private static final String showCashier  ="showCashier";
    //	merData			商户自定义返	否	String(200)	交易成功后给商户的自定义返回数据
//    private static final String merData  ="merData";
//    //	pcQuickPay 		网银快捷 PC	
//    private static final String pcQuickPay  ="pcQuickPay";
//    //	wapQuickPay 	网银快捷 WAP
//    private static final String wapQuickPay  ="wapQuickPay";

    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Dec 18, 2017
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	String api_CHANNEL_BANK_NAME_FlAG = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
    	if (null == api_CHANNEL_BANK_NAME_FlAG || !api_CHANNEL_BANK_NAME_FlAG.contains(",") || api_CHANNEL_BANK_NAME_FlAG.split(",").length != 2) {
            log.error("[五福]-[请求支付]-1.1.组装请求参数格式：服务名称,交易类型。如：支付宝,支付宝扫码==>UniThirdPay,ALIPAY_NATIVE" );
            throw new PayException("[五福]-[请求支付]-1.1.组装请求参数格式：服务名称,交易类型。如：支付宝,支付宝扫码==>UniThirdPay,ALIPAY_NATIVE" );
		}
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(svcName, api_CHANNEL_BANK_NAME_FlAG.split(",")[0]);
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(merchOrderId,channelWrapper.getAPI_ORDER_ID() );
                put(amt,  channelWrapper.getAPI_AMOUNT());
                put(pName,"pName");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(tranType,api_CHANNEL_BANK_NAME_FlAG.split(",")[1]);
//                如需直接在第三方方收银台显示二维码请填入值为 1	目前本项目使用的是自由处理
                put(showCashier,handlerUtil.isFS(channelWrapper) ? "1" : "0");
            }
        };
        log.debug("[五福]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Dec 18, 2017
     */
    protected String buildPaySign(Map api_response_params) throws PayException {
    	StringBuffer signSrc= new StringBuffer();
    	signSrc.append(api_response_params.get(amt));
    	signSrc.append(api_response_params.get(merId));
    	signSrc.append(api_response_params.get(merchOrderId));
    	signSrc.append(api_response_params.get(notifyUrl));
    	signSrc.append(api_response_params.get(pName));
    	signSrc.append(api_response_params.get(showCashier));
    	signSrc.append(api_response_params.get(svcName));
    	signSrc.append(api_response_params.get(tranType));
    	signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"UTF-8");
        log.debug("[五福]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
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
     * Dec 18, 2017
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> result = Maps.newHashMap();
        if(handlerUtil.isWY(channelWrapper)){
        	StringBuffer htmlContent = handlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        	//保存第三方返回值
        	result.put(HTMLCONTEXT, htmlContent.toString());
        }else{
    		String tmpStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            if (StringUtils.isBlank(tmpStr)) {
            	log.error("[五福]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空");
            	throw new PayException("第三方返回异常:返回空");
            }
            //如果是wap支付方式
            if(handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isFS(channelWrapper)){
//            	if (tmpStr.contains("QRbg_error")) {
//            		log.error("[五福]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + tmpStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(tmpStr);
//				}
            	//保存第三方返回值
            	result.put(HTMLCONTEXT, tmpStr);
            }else {
            	JSONObject jsonObject = JSONObject.parseObject(tmpStr);
            	if (!jsonObject.containsKey("retCode") || !"000000".equals(jsonObject.getString("retCode"))) {
            		log.error("[五福]-[请求支付]-3.2.发送支付请求，获取支付请求返回值异常:"+tmpStr);
            		throw new PayException(tmpStr);
            	}
          		result.put(handlerUtil.isFS(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject.getString("payUrl"));
			}
            result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[五福]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Dec 18, 2017
     */
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (CollectionUtils.isEmpty(resultListMap) || resultListMap.size() != 1) {
        	throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
        Map<String, String> qrMap = resultListMap.get(0);
        if (null != qrMap && qrMap.containsKey(QRCONTEXT)) {
        	requestPayResult.setRequestPayQRcodeContent(qrMap.get(QRCONTEXT));
        }else if (null != qrMap && qrMap.containsKey(HTMLCONTEXT)) {
        	requestPayResult.setRequestPayHtmlContent(qrMap.get(HTMLCONTEXT));
        }else if (null != qrMap && qrMap.containsKey(JUMPURL)) {
            requestPayResult.setRequestPayJumpToUrl(qrMap.get(JUMPURL));
        }
        requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
        requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
        requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
        requestPayResult.setRequestPayQRcodeURL(null);
        requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
        if (!ValidateUtil.requestesultValdata(requestPayResult)) {
        	throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        log.debug("[五福]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}