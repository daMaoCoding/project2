package dc.pay.business.anquanfu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

@RequestPayHandler("ANQUANFU")
public final class AnQuanFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AnQuanFuPayRequestHandler.class);

     private static final String  app_id   = "app_id";
     private static final String  create_time   = "create_time";
     private static final String  out_trade_no   = "out_trade_no";
     private static final String  subject   = "subject";
     private static final String  total_amount   = "total_amount";
     private static final String  body   = "body";
     private static final String  return_url   = "return_url";
     private static final String  notify_url   = "notify_url";

//     private static final String  sign   = "sign";
     private static final String  type   = "type";
     private static final String  bank_code   = "bank_code";
     private static final String  pay_type   = "pay_type";
     private static final String  client_ip    = "client_ip";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//            	if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY") && !channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_KJZF")){
            	if(handlerUtil.isWY(channelWrapper) && !handlerUtil.isWebWyKjzf(channelWrapper)){
                    put(bank_code ,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(type, "");
                    put(pay_type, "80003");
                }else{
                    put(type, "2");
                    put(pay_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(app_id,channelWrapper.getAPI_MEMBERID());
                put(create_time,new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(subject,"PAY");
                put(total_amount,channelWrapper.getAPI_AMOUNT());
                put(body,"PAYBody");
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(client_ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[安全付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map params) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        signSrc.append("app_id=").append(params.get(app_id)).append("&");
        signSrc.append("body=").append(params.get(body)).append("&");
        signSrc.append("create_time=").append(params.get(create_time)).append("&");
        signSrc.append("notify_url=").append(params.get(notify_url)).append("&");
        signSrc.append("out_trade_no=").append(params.get(out_trade_no)).append("&");
        signSrc.append("pay_type=").append(params.get(pay_type)).append("&");
        signSrc.append("return_url=").append(params.get(return_url)).append("&");
        signSrc.append("subject=").append(params.get(subject)).append("&");
        signSrc.append("total_amount=").append(params.get(total_amount));
        String signInfo = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY(),"SHA256withRSA");	// 签名
        } catch (Exception e) {
            log.error("[安全付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        log.debug("[安全付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
    	Map result = Maps.newHashMap();
    	if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) ||HandlerUtil.isWebYlKjzf(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
			String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
			if (StringUtils.isBlank(resultStr)) {
				log.error("[安全付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("第三方返回异常:返回空"+",参数："+JSON.toJSONString(payParam));
			}
	        //兼容安全付支付宝转到快捷支付
	        //<script type="text/javascript">window.location.href='http://api.91jingcheng.com/trade/redirect/a46d8b7ac7468429aecb12b14bf9bd31ce67d16f69c2e5123015d3f936eb3451';</script>
	        if(resultStr.contains("location.href")){
	            String kjURL = resultStr.substring(resultStr.indexOf("window.location.href='") + 22, resultStr.indexOf("';</script>"));
	            result.put(HandlerUtil.isWebYlKjzf(channelWrapper) ? JUMPURL : QRCONTEXT, kjURL);
	        }else{
	            JSONObject resJson = JSONObject.parseObject(resultStr);
	            if (resJson == null || !"SUCCESS".equalsIgnoreCase(resJson.getString("ret_code"))) {
	            	log.error("[安全付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            	throw new PayException(JSON.toJSONString(resultStr));
	            }
	            result.put(QRCONTEXT, QRCodeUtil.decodeByBase64(resJson.getString("code_url")));
	        }
	    }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
	    payResultList.add(result);
        log.debug("[安全付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[安全付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}