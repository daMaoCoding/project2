package dc.pay.business.koubeizhifu;

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
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.Base64Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("KOUBEIZHIFU")
public final class KouBeiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KouBeiZhiFuPayRequestHandler.class);


    private static final String  uid = "uid";   //	商户编号	15000001	必须,
    private static final String  type = "type";   //	返回类型	img	必须,img html json
    private static final String  bank = "bank";   //	支付通道	AliPaySK	必须,AliPaySK|WxPaySK
    private static final String  ordid = "ordid";   //	订单编号	123456789	必须,不能重复
    private static final String  amt = "amt";   //	订单金额	10	必须,整数
    private static final String  info = "info";   //	附带信息	Abcdefg	必须,随意
    private static final String  ret = "ret";   //	通知地址	http://www.a.com/ret	必须,需能远程访问
    private static final String  sign = "sign";   //	签名	f9314e67f999c2ddbfd60fb7136b26ef	必须 uid,type,bank,amt,ordid,info,ret,jump,skey以上参数按顺序连接,采用md5(32位)加密
    private static final String  json = "json";
    private static final String  jump = "jump";

    private static final String  usip = "usip";  //
    private static final String  usid = "usid";  //用户唯一标识


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(uid,channelWrapper.getAPI_MEMBERID());
            payParam.put(type,json);
            payParam.put(bank,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(ordid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amt,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(info,Base64Utils.encodeToString(channelWrapper.getAPI_ORDER_ID().getBytes()));
            payParam.put(ret,Base64Utils.encodeToString(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL().getBytes()));
            payParam.put(jump,Base64Utils.encodeToString(channelWrapper.getAPI_WEB_URL().getBytes()));
            payParam.put(usip,channelWrapper.getAPI_Client_IP());
            payParam.put(usid,HandlerUtil.getRandomStr(5));
        }

        log.debug("[口碑支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // uid,type,bank,amt,ordid,info,ret,jump,skey以上参数按顺序连接,采用md5(32位)加密
        //info,ret,jump 先urlencode,然后链接,再md5
        String paramsStr = String.format("%s%s%s%s%s%s%s%s%s",
                params.get(uid),
                params.get(type),
                params.get(bank),
                params.get(amt),
                params.get(ordid),
                params.get(info),
                params.get(ret),
                params.get(jump),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[口碑支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("redirect")  && StringUtils.isNotBlank(jsonResultStr.getString("redirect"))){
                                result.put(JUMPURL, jsonResultStr.getString("redirect"));
                                payResultList.add(result);
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[口碑支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[口碑支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[口碑支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}