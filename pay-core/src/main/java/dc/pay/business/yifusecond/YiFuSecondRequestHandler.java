package dc.pay.business.yifusecond;

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
 * Mar 9, 2018
 */
@RequestPayHandler("YIFUSECOND")
public final class YiFuSecondRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiFuSecondRequestHandler.class);

  	//版本号			version		N	Y		固定值3.0
  	private static final String version				="version";
  	//接口名称			method		N	Y		Rh.online.interface
  	private static final String method				="method";
  	//商户ID			partner		N	Y		商户id,由易付2分配
  	private static final String partner				="partner";
  	//银行类型			banktype	N	Y		银行类型，具体参考附录1,default为跳转到易付2接口进行选择支付
  	private static final String banktype				="banktype";
  	//金额			paymoney	N	Y		单位元（人民币）
  	private static final String paymoney				="paymoney";
  	//商户订单号		ordernumber	N	Y		商户系统订单号，该订单号将作为易付2接口的返回数据。该值需在商户系统内唯一，易付2系统暂时不检查该值是否唯一
  	private static final String ordernumber				="ordernumber";
  	//下行异步通知地址	callbackurl	N	Y		下行异步通知的地址，需要以http://开头且没有任何参数
  	private static final String callbackurl				="callbackurl";
  	//是否显示收银台		isshow		Y	N		该参数为支付宝扫码、微信、QQ钱包专用，默认为1，跳转到网关页面进行扫码，如设为0，则网关只返回二维码图片地址供用户自行调用
  	private static final String isshow				="isshow";

	/**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Mar 9, 2018
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(version, "3.0");
            	put(method, "Rh.online.interface");
            	put(partner, channelWrapper.getAPI_MEMBERID());
            	put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(paymoney , HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(ordernumber,channelWrapper.getAPI_ORDER_ID());
            	put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	//该参数为支付宝扫码、微信、QQ钱包专用，默认为1，跳转到网关页面进行扫码，如设为0，则网关只返回二维码图片地址供用户自行调用
            	put(isshow,"0");
            }
        };
        log.debug("[易付2]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Mar 9, 2018
     */
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
    	//version={0}&method={1}&partner={2}&banktype={3}&paymoney={4}&ordernumber={5}&callbackurl={6}key
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(version+"=").append(api_response_params.get(version)).append("&");
		signSrc.append(method+"=").append(api_response_params.get(method)).append("&");
		signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
		signSrc.append(banktype+"=").append(api_response_params.get(banktype)).append("&");
		signSrc.append(paymoney+"=").append(api_response_params.get(paymoney)).append("&");
		signSrc.append(ordernumber+"=").append(api_response_params.get(ordernumber)).append("&");
		signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl));
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[易付2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5)+"，参数："+paramsStr);
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
     * Mar 9, 2018
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        Map<String,String> result = Maps.newHashMap();
		if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }
        payResultList.add(result);
        log.debug("[易付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Mar 9, 2018
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
        log.debug("[易付2]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}