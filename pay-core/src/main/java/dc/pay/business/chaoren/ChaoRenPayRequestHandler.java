package dc.pay.business.chaoren;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("CHAOREN")
public final class ChaoRenPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChaoRenPayRequestHandler.class);

//    参数含义				参数名			必填			加签名			说明
//    商户ID					merchantid		Y			Y				商户id，由超人后台获取
//    银行类型				type			Y			Y				支付宝转账银行卡通道: 105
//    金额					m				Y			Y				单位元（人民币）（最小支付金额为10）
//    商户订单号				orderid			Y			Y				商户系统订单号，该订单号将原样返回。该值需在商户系统内唯一。
//    下行异步通知地址			callbackurl		Y			Y				下行异步通知过程的返回地址，必须是有效网址，且不带参数。支付成功后系统会对该地址发起回调，通知支付成功的消息。
//    MD5签名				sign			Y			-				32位写MD5签名值,小写。
//    下行同步跳转地址（成功）	gotrue			N			N				下行同步通知过程的返回地址(用户支付成功，将从支付页面跳转gotrue所在的页面。没有就传空字符)。注：若提交值无该参数，或者该参数值为空，则在支付完成后，用户将停留在超人系统提示支付成功的页面。
//    下行同步通知地址（失败）	gofalse			N			N				下行同步通知过程的返回地址(在支付失败后支付系统将会跳转到的商户系统连接地址)。注：若提交值无该参数，或者该参数值为空，则在支付完成后，用户将停留在超人系统提示支付失败的页面。
//    商户用户标识			uid				N							商户系统用户ID，该值需在商户系统内唯一，可减少错单率。将原样返回。

    private static final String merchantid               ="merchantid";
    private static final String type           			 ="type";
    private static final String m           			 ="m";
    private static final String orderid           		 ="orderid";
    private static final String callbackurl          	 ="callbackurl";
    private static final String gotrue              	 ="gotrue";
    private static final String gofalse            		 ="gofalse";
    private static final String sign                	 ="sign";
    private static final String key                 	 ="key";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantid, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(m,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[超人支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s", 
        		api_response_params.get(merchantid)+"&"+
        		api_response_params.get(type)+"&"+
        		api_response_params.get(m)+"&"+
        		api_response_params.get(orderid)+"&"+
        		api_response_params.get(callbackurl)+"&"+
        		channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[超人支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[超人支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[超人支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}