package dc.pay.business.jinfu;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 16, 2018
 */
@RequestPayHandler("JINFU")
public final class JinFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinFuPayRequestHandler.class);

    //参数名         类型        是否必填                备注
    //money          String        是                订单金额
    //ip             String        是                生成二维码人的IP地址 非本接口调用者服务器地址
    //group_id       Int           是                接口使用者ID 由系统开发商提供
    //notify_url     String        是                用于接收支付成功消息的地址
    //pay_code       String        否                支付方式 默认值alipay 暂无其他可选值
    //user_order_sn  String        是                订单号 本系统不提供订单号重复判断 需接口使用者自行判断
    //subject        String        是                订单标题
    //sign           String        是                签名 签名示例:str = "group_id=20180726343869&money=0.01&notify_url=http://jinfu.xiamenrongluo.com/mobile/Service/test"    str = str+"&pay_code=alipay&subject=ceshi&user_order_sn=001"       sign = md5(str+密钥)
    private static final String money                      ="money";
    private static final String ip                         ="ip";
    private static final String group_id                   ="group_id";
    private static final String notify_url                 ="notify_url";
    private static final String pay_code                   ="pay_code";
    private static final String user_order_sn              ="user_order_sn";
    private static final String subject                    ="subject";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ip,  channelWrapper.getAPI_Client_IP());
                put(group_id, channelWrapper.getAPI_MEMBERID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(user_order_sn,channelWrapper.getAPI_ORDER_ID());
                put(subject,"name");
            }
        };
        log.debug("[金福]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        String paramsStr = signSrc.toString();
        //去除最后一个&符
        paramsStr = paramsStr.substring(0,paramsStr.length()-1);
        paramsStr = paramsStr+channelWrapper.getAPI_KEY();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[金福]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[金福]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[金福]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        JSONObject resJson = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("statusCode") && "200".equalsIgnoreCase(resJson.getString("statusCode"))  && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
            String dataStr = resJson.getString("data");
            if (StringUtils.isBlank(dataStr)) {
                log.error("[金福]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject dataJson = JSONObject.parseObject(dataStr);
            result.put(HandlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, dataJson.getString("qrcode"));
        }else {
            log.error("[金福]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[金福]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[金福]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}