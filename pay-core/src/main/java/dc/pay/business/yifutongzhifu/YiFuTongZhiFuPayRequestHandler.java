package dc.pay.business.yifutongzhifu;

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
 * @author sunny
 * 03 19, 2019
 */
@RequestPayHandler("YIFUTONGZHIFU")
public final class YiFuTongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiFuTongZhiFuPayRequestHandler.class);

//    #	参数名			含义				类型				说明
//    1	custno			商户登录帐号		string(24)		必填。您的商户唯一标识，登录平台的账号。
//    2	price			价格				float			必填。单位：元（保留2位小数）
//    3	istype			支付渠道			int				必填。1：支付宝；
//    4	notify_url		通知回调网址		string(255)		必填。会员支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www.aaa.com/notify
//    5	return_url		跳转网址			string(255)		必填。会员支付成功后，我们会让会员浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.aaa.com/return
//    6	orderid			商户自定义订单号	string(50)		必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：2018090717121,不同的订单订单号不能重复
//    7	orderuid		商户会员登录账号	string(100)		必填。我们会显示在您后台的订单列表中，方便您看到是哪个会员的付款，方便后台对账。强烈建议填写 会员登录帐号。例：test888。如果会员是登录系统后再充值，该值可直接通过接口传入，不需要会员填写。
//    8	alipayname		商户会员姓名		string(100)		选填。我们会显示在您后台的订单列表中。方便特殊情况下，平台客服快速对账需要。 由会员充值时和充值金额一起填写，或由接口自动传入。
//    9	key				秘钥				string(32)		必填。按下面要求，连Token一起，按参数名字母升序排序。把参数值拼接在一起。做md5-32位加密，取字符串小写。得到key。网址类型的参数值不要urlencode。

    private static final String custno               		="custno";
    private static final String price           			="price";
    private static final String istype           			="istype";
    private static final String notify_url           		="notify_url";
    private static final String return_url          		="return_url";
    private static final String orderid              		="orderid";
    private static final String orderuid            		="orderuid";
    private static final String key                 		="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(custno, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(orderuid,System.currentTimeMillis()/100000+"");
                put(istype,"1");
            }
        };
        log.debug("[易付通支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s%s%s",
        		api_response_params.get(istype),
        		api_response_params.get(notify_url),
        		api_response_params.get(orderid),
        		api_response_params.get(orderuid),
        		api_response_params.get(price),
        		api_response_params.get(return_url),
        		channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[易付通支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[易付通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[易付通支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}