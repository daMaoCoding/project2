package dc.pay.business.shanshuifu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 7, 2018
 */
@RequestPayHandler("SHANSHUIFU")
public final class ShanShuiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShanShuiFuPayRequestHandler.class);

    //参数名称        含义                    参数长度        是否必填      备注                   样例
    //merId           商户ID                  String(16)      是            签约合作方的唯一用户号      2088001159940003
    //appId           应用ID                  String(16)      是            商户网站使用的编码格式，统一使用UTF-8      908265290880774144
    //orderNo         商户网站唯一订单号      String(16)      是            商户网站唯一订单号（确保在商户系统中唯一）。      
    //totalFee        交易金额                Number(12)      是            金额单位（元）      
    //channeltype     通道类型                String(16)      是            对接前，请确认使用什么通道编码跟场景
    //authCode        反扫二维码              String(64)      否                  
    //title           商品名称                String(256)     否                  
    //attach          附加描述                String(200)     否            可放透传的信息。存放原订单号和原订单付款金额等信息。      
    //orderRepeat     订单号是否重复          String(1)       否            默认为0可重复，1为不可重复      
    //openid          公众号openid            String(32)      否            微信公众哈哦支付是必填      对应绑定主体公众号获取的用户openid
    //notifyUrl      异步通知                 String（256）   否            不可带参数      http://www.baidu.com
    //returnUrl      同步跳转                 String（256）   否            不可带参数      http://www.baidu.com
    //sign            签名                    String(256)     是            参见“签名机制”      e8qdwl9caset5zugii2r7q0k8ikopxor
    private static final String merId                  ="merId";
    private static final String appId                  ="appId";
    private static final String orderNo                ="orderNo";
    private static final String totalFee               ="totalFee";
    private static final String channeltype            ="channeltype";
//    private static final String authCode               ="authCode";
    private static final String title                  ="title";
//    private static final String attach                 ="attach";
    private static final String orderRepeat            ="orderRepeat";
//    private static final String openid                 ="openid";
    private static final String notifyUrl              ="notifyUrl";
    private static final String returnUrl              ="returnUrl";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == api_MEMBERID || !api_MEMBERID.contains("&") || api_MEMBERID.split("&").length != 2) {
            log.error("[山水付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户ID&APPID" );
            throw new PayException("[山水付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户ID&APPID" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(merId, api_MEMBERID.split("&")[0]);
                put(appId, api_MEMBERID.split("&")[1]);
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(totalFee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(channeltype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                put(attach,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(orderRepeat,"1");
                put(title ,"title");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[山水付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[山水付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[山水付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
            throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
        }
        JSONObject resJson = JSONObject.parseObject(resultStr);
        if (!resJson.containsKey("retCode") || !"100".equals(resJson.getString("retCode"))) {
            log.error("[山水付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("qrcode"));
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[山水付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[山水付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}