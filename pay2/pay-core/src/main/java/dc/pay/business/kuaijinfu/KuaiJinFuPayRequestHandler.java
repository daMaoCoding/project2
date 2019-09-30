package dc.pay.business.kuaijinfu;

import java.security.MessageDigest;
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
import dc.pay.utils.ValidateUtil;

/**
 * 
 * 
 * @author sunny
 * Dec 20, 2018
 */
@RequestPayHandler("KUAIJINFU")
public final class KuaiJinFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiJinFuPayRequestHandler.class);

//    参数			类型		是否必填		描述					
//    app_id		字符串	必传			商户应用id，请登录商户后台商户管理->商户主页里查看	3577154428
//    pay_method	字符串	必传			支付方式，pc_alipay(PC支付宝)、wap_alipay(WAP支付宝)、pc_wechatpay(PC微信)、wap_wechatpay(WAP微信)	pc_alipay
//    order_id		字符串	必传			商户订单号，请确保订单号唯一	201807050911453635
//    price			浮点型	必传			订单金额，单位为元，精确到小数点后两位，最小金额1.00元，最大金额5000.00元	88.88
//    subject		字符串	必传			订单标题	测试订单
//    body			字符串	可选			订单描述	这是一个测试订单
//    return_url	网址		必传			支付同步跳转地址，必须是可外网访问的绝对url，支付完成后会跳转到该地址	http://www.kuaijinfu.com/pay/return.do
//    notify_url	网址		必传			支付异步通知地址，必须是可外网访问的绝对url，支付成功后会异步POST请求该地址	http://www.kuaijinfu.com/pay/notify.do
//    sign			字符串	必传			签名字符串，签名规则如下	64F7C22310C19A85233DC0C0451C7186


    private static final String app_id               ="app_id";
    private static final String pay_method           ="pay_method";
    private static final String order_id             ="order_id";
    private static final String price           	 ="price";
    private static final String subject              ="subject";
    private static final String body                 ="body";
    private static final String return_url           ="return_url";
    private static final String notify_url           ="notify_url";
    private static final String sign                 ="sign";
//    private static final String pay_productdesc             ="pay_productdesc";
//    private static final String pay_producturl              ="pay_producturl";
//    private static final String pay_md5sign                 ="pay_md5sign";

    private static final String token        ="token";
    //signature    数据签名    32    是    　
    

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(app_id, channelWrapper.getAPI_MEMBERID());//商户号
                put(pay_method, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());//支付方式
                put(order_id, channelWrapper.getAPI_ORDER_ID());//订单号
                put(price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));//订单金额
                put(subject,channelWrapper.getAPI_ORDER_ID());//标题
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[快金付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> params) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(params);
    	StringBuilder signSrc = new StringBuilder();
    	for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))))  //
              continue;
            signSrc.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
    	
    	signSrc.append(token +"="+ getMD5ofStr(channelWrapper.getAPI_KEY(),"UTF-8"));
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[快金付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        //String payresult = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        String payresult = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
        JSONObject resJSON = JSON.parseObject(payresult);
        if (resJSON.containsKey("status") && resJSON.getString("status").equals("success") && resJSON.containsKey("jump") && StringUtils.isNotBlank(resJSON.getString("jump"))) {
			String wxQRContext =resJSON.getString("jump");
			if(StringUtils.isNotBlank(wxQRContext) && wxQRContext.contains("<form") && !wxQRContext.contains("{")){
				result.put(HTMLCONTEXT, HandlerUtil.UrlDecode(wxQRContext));
			}
			
        } else {
            throw new PayException(payresult);
        }
        payResultList.add(result);
        log.debug("[快金付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[快金付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    
    public static String getMD5ofStr(String str, String encode) {
        try{
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes(encode));
            byte[] digest = md5.digest();

            StringBuffer hexString = new StringBuffer();
            String strTemp;
            for (int i = 0; i < digest.length; i++) {
                // byteVar &
                // 0x000000FF的作用是，如果digest[i]是负数，则会清除前面24个零，正的byte整型不受影响。
                // (...) | 0xFFFFFF00的作用是，如果digest[i]是正数，则置前24位为一，
                // 这样toHexString输出一个小于等于15的byte整型的十六进制时，倒数第二位为零且不会被丢弃，这样可以通过substring方法进行截取最后两位即可。
                strTemp = Integer.toHexString(
                        (digest[i] & 0x000000FF) | 0xFFFFFF00).substring(6);
                hexString.append(strTemp);
            }
            return hexString.toString();
        }catch(Exception e){
            e.printStackTrace();
            return "";
        }

    }

}