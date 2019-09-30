package dc.pay.business.hengfutong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 25, 2018
 */
@RequestPayHandler("HENGFUTONG")
public final class HengFuTongRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HengFuTongRequestHandler.class);

//    参数名				说明					类型(长度）	备注
//    merchantId		商户号				String(20)	平台提供
//    corp_flow_no		订单号				String(20)	建议时间戳+商户号后四位，作为订单号开头
//    totalAmount		交易金额				Decimal (11,2)	以元为单位的整数，最小交易金额为10元（1000分）
//    notify_url		通知URL				String(255)	交易完成后的异步通知地址（后台）
//    return_url		页面回调地址			String(255)	交易完成时的同步返回地址（前台页面）
//    desc				产品描述				String(20)	
//    type				支付类型				TinyInt(2)	1 : 微信, 2 : 支付宝
//    sign				签名,小写				String(255)	MD5(merchantId+"pay"+totalAmount+corp_flow_no+商户秘钥)


  private static final String merchantId                 ="merchantId";
  private static final String corp_flow_no               ="corp_flow_no";
  private static final String totalAmount                ="totalAmount";
  private static final String notify_url           	  	 ="notify_url";
  private static final String return_url                 ="return_url";
  private static final String desc           			 ="desc";
  private static final String type             			 ="type";
  
  private static final String sign                		 ="sign";
  
  private static final String key        				 ="key";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(merchantId,channelWrapper.getAPI_MEMBERID());
            	put(corp_flow_no,channelWrapper.getAPI_ORDER_ID());
            	put(totalAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(return_url,channelWrapper.getAPI_WEB_URL());
            	put(desc,channelWrapper.getAPI_ORDER_ID());
            	put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[恒付通支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//签名规则
    	StringBuilder signSrc = new StringBuilder();
    	signSrc.append(api_response_params.get(merchantId));
    	signSrc.append("pay");
    	signSrc.append(api_response_params.get(totalAmount));
    	signSrc.append(api_response_params.get(corp_flow_no));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr =signSrc.toString();
        String signMD5 = HandlerUtil.getPhpMD5(paramsStr);
        log.debug("[恒付通支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

     protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
       /*if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }*/
    	String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,String.class,HttpMethod.GET);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[恒付通支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        
        if (StringUtils.isNotBlank(resultStr)) {
			result.put(HTMLCONTEXT, resultStr);
		}
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[恒付通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[恒付通支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}