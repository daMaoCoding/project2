package dc.pay.business.liebao;

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
@RequestPayHandler("LIEBAO")
public final class LieBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LieBaoPayRequestHandler.class);

//    参数名				必选				类型				说明
//    group_id			是				string			集团会员编号
//    ip				是				string			支付者的IP,并非你服务器IP
//    user_order_sn		是				string			订单编号,必须保证唯一,不能超过24位,建议数字,不能包含下划线”_”
//    subject			是				string			订单名称
//    money				是				string			订单金额
//    product_type		是				string			产品类型,手机网站支付 H5,扫码支付 QRCODE
//    pay_type			是				string			支付宝ALI,微信WX,银联UNI
//    notify_url		是				string			回调通知地址,请将此参数转码,收到请返回OK,未返回最多通知3次
//    return_url		否				string			同步跳转地址,请将此参数转码,产品类型为H5的话,请传你实际的同步跳转地址,如不填写将跳转本站地址,扫码无须此参数
//    sign				是				string			签名 md5(group_id=1234567879&money=0.01&user_order_sn=2587413691+KEY) 参数按此顺序,只要group_id,money,user_order_sn三个参数参与,参数名小写

    private static final String group_id               	="group_id";
    private static final String ip           			="ip";
    private static final String user_order_sn           ="user_order_sn";
    private static final String subject           		="subject";
    private static final String money          			="money";
    private static final String product_type            ="product_type";
    private static final String pay_type            	="pay_type";
    private static final String notify_url           	="notify_url";
    private static final String return_url            	="return_url";
    private static final String sign                	="sign";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(group_id, channelWrapper.getAPI_MEMBERID());
                put(user_order_sn,channelWrapper.getAPI_ORDER_ID());
                put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                if(HandlerUtil.isWapOrApp(channelWrapper)){
                	put(product_type,"H5");
                }else if(HandlerUtil.isWEBWAPAPP_SM(channelWrapper)){
                	put(product_type,"QRCODE");
                }
                put(subject,channelWrapper.getAPI_ORDER_ID());
                put(ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[猎豹支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s", 
        		group_id+"="+api_response_params.get(group_id)+"&",
        		money+"="+api_response_params.get(money)+"&",
        		user_order_sn+"="+api_response_params.get(user_order_sn),
        		channelWrapper.getAPI_KEY()
        		);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[猎豹支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[猎豹支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(EMPTYRESPONSE);
        }
        resultStr = java.net.URLDecoder.decode(resultStr);
        resultStr=resultStr.replaceAll("\\\\", "");
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[猎豹支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[猎豹支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (null != resJson && resJson.containsKey("statusCode") && resJson.getString("statusCode").equals("200")) {
        	JSONObject code_url = resJson.getJSONObject("data");
        	if(HandlerUtil.isWapOrApp(channelWrapper)){
        		result.put(JUMPURL, code_url.getString("qrcode"));
        	}else if(HandlerUtil.isWEBWAPAPP_SM(channelWrapper)){
        		result.put(QRCONTEXT, code_url.getString("qrcode"));
        	}
        }else {
            log.error("[猎豹支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        payResultList.add(result);
        log.debug("[猎豹支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[猎豹支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}