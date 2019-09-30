package dc.pay.business.duola;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 1, 2018
 */
@RequestPayHandler("DUOLA")
public final class DuoLaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DuoLaPayRequestHandler.class);

    //参数                 类型              说明        允许为空
    //shop_id             字符串           商家ID        否
    //user_id             字符串           商家用户ID        否
    //money               字符串           订单金额，单位元，如：0.01表示一分钱；        否
    //type                字符串           微信：wechat，支付宝：alipay        否
    //shop_no             字符串           商家订单号，长度不超过40；        是
    //notify_url          字符串           订单支付成功回调地址（具体参数详见接口2，如果为空，平台会调用商家在WEB端设置的订单回调地址；否则，平台会调用该地址，WEB端设置的地址不会被调用）；        是
    //return_url          字符串           二维码扫码支付模式下：支付成功页面‘返回商家端’按钮点击后的跳转地址;如果商家采用自有界面，则忽略该参数；        是
    //sign                字符串           验签字符串，MD5（shop_id + user_id + money + type +sign_key）；字符串相加再计算MD5一次，MD5为32位小写；shop_id 和sign_key登陆商家后台可以查看；        否
    private static final String shop_id                       ="shop_id";
    private static final String user_id                       ="user_id";
    private static final String money                         ="money";
    private static final String type                          ="type";
    private static final String shop_no                       ="shop_no";
    private static final String notify_url                    ="notify_url";
    private static final String return_url                    ="return_url";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String api_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == api_MEMBERID || !api_MEMBERID.contains("&") || api_MEMBERID.split("&").length != 2) {
//            log.error("[哆啦]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商家标识shop_id&商户号user_id（登陆账号）" );
//            throw new PayException("[哆啦]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商家标识shop_id&商户号user_id（登陆账号）" );
//        }
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[哆啦]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：商家密钥sign_key-商家标识shop_id" );
            throw new PayException("[哆啦]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：商家密钥sign_key-商家标识shop_id" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(shop_id, channelWrapper.getAPI_KEY().split("-")[1]);
                put(user_id, channelWrapper.getAPI_MEMBERID());
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(shop_no,channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[哆啦]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(shop_id));
        signSrc.append(api_response_params.get(user_id));
        signSrc.append(api_response_params.get(money));
        signSrc.append(api_response_params.get(type));
        signSrc.append(channelWrapper.getAPI_KEY().split("-")[0]);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[哆啦]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),"application/json");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[哆啦]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[哆啦]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[哆啦]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("pay_url") && StringUtils.isNotBlank(resJson.getString("pay_url"))) {
            String pay_url = resJson.getString("pay_url");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, pay_url);
            }else{
                result.put(QRCONTEXT, pay_url);
            }
        }else {
            log.error("[哆啦]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[哆啦]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[哆啦]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}