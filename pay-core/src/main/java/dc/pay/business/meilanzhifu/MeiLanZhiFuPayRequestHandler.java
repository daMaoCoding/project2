package dc.pay.business.meilanzhifu;

import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
 * Dec 5, 2017
 */
@RequestPayHandler("MEILANZHIFU")
public final class MeiLanZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MeiLanZhiFuPayRequestHandler.class);

    private static final String   app_id            = "app_id";           //参数名称：商家号商户签约时，平台分配给商家的唯一身份标识。例如：1803110116
    private static final String   trade_type        = "trade_type";       //参数名称：交易类型
    private static final String   total_amount      = "total_amount";     //参数名称：订单金额 订单总金额，单位为分
    private static final String   out_trade_no      = "out_trade_no";     //参数名称：商家订单号
    private static final String   notify_url        = "notify_url";       //参数名称：异步通知地址下行异步通知的地址，需要以http:开头且没有任何参数
    private static final String   interface_version = "interface_version";//参数名称：接口版本 固定值：V2.0(大写)。
    private static final String   sign = "sign";    //参数名称：签名数据32位小写MD5签名值，GB2312编码。




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){
            payParam.put(app_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(total_amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(interface_version,"V2.0" );
        }
        log.debug("[澜湄支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
//      app_id=1803110116&notify_url=http://www.baidu.com/notify.php&
// out_trade_no=201803130890&total_amount=100&trade_type=WEIXIN_NATIVEaef5ef05374ad5043f9cee3a1789fe91
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || interface_version.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
               continue;
            signSrc.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String signStr = signSrc.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[澜湄支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
			/*if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {*/
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
			/*}else{

				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
				String qrContent=null;
				if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
					HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
					if(payUrlInput!=null ){
						String qrContentSrc = payUrlInput.getValueAttribute();
						if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
					}
				}
				if(StringUtils.isNotBlank(qrContent)){
					result.put(QRCONTEXT, qrContent);
					payResultList.add(result);
				}else {  throw new PayException(endHtml.asXml()); }

				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);

				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
					result.put(HTMLCONTEXT,resultStr);
					payResultList.add(result);
				}else if(StringUtils.isNotBlank(resultStr) ){
					JSONObject jsonResultStr = JSON.parseObject(resultStr);
					if(null!=jsonResultStr && jsonResultStr.containsKey("return_code") && "success".equalsIgnoreCase(jsonResultStr.getString("return_code"))
							&& jsonResultStr.containsKey("mweb_url") && StringUtils.isNotBlank(jsonResultStr.getString("mweb_url"))){
						if(HandlerUtil.isWapOrApp(channelWrapper)){
							result.put(JUMPURL, jsonResultStr.getString("mweb_url"));
						}else{
							result.put(QRCONTEXT, jsonResultStr.getString("mweb_url"));
						}
						payResultList.add(result);
					}else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}

			}*/
        } catch (Exception e) {
            log.error("[澜湄支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[澜湄支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


	protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
		RequestPayResult requestPayResult = new RequestPayResult();
		if (null != resultListMap && !resultListMap.isEmpty()) {
			if (resultListMap.size() == 1) {
				Map<String, String> resultMap = resultListMap.get(0);
				requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
			}
			if (ValidateUtil.requestesultValdata(requestPayResult)) {
				requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
			} else {
				throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
			}
		} else {
			throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
		log.debug("[澜湄支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
}