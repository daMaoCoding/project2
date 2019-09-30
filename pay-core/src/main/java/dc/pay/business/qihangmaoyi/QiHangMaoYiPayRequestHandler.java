package dc.pay.business.qihangmaoyi;

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
@RequestPayHandler("QIHANGMAOYI")
public final class QiHangMaoYiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QiHangMaoYiPayRequestHandler.class);

//    #	参数名					含义					类型					说明
//    1	uid						商户uid				int(10)				必填。您的商户唯一标识，在“账户信息”-“支付信息”中获取。
//    2	price					价格					float				必填。单位：元。精确小数点后2位
//    3	type					支付渠道				string(16)			选填（如不填则默认alipayBank，暂不参与签名）。alipay：支付宝转账包；alipayBank：支付宝转银行；alipay2：支付宝原生码；alipay3：支付宝付款码；912：聚合企业扫码；913：支付宝红包码；
//    3	notify_url				通知回调网址			string(255)			必填。用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www .aaa.com/qpay_notify
//    4	return_url				跳转网址				string(255)			必填。用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.aaa .com/qpay_return
//    5	orderid					商户自定义订单号		string(50)			必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201710192541
//    9	key						秘钥					string(32)			必填。把使用到的所有参数，连Token一起，按 参数名 字母升序排序。把 参数值 拼接在一起。做md5-32位加密，取字符串小写。得到key。网址类型的参数值不要urlencode。

    private static final String uid                 ="uid";
    private static final String price           	="price";
    private static final String type           		="type";
    private static final String notify_url          ="notify_url";
    private static final String return_url          ="return_url";
    private static final String orderid             ="orderid";
    private static final String key                 ="key";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(return_url,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[起航贸易支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s%s", 
        	   api_response_params.get(notify_url),
        	   api_response_params.get(orderid),
        	   api_response_params.get(price),
        	   api_response_params.get(return_url),
        	   channelWrapper.getAPI_KEY(),
        	   api_response_params.get(uid)
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[起航贸易支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[起航贸易支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[起航贸易支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}