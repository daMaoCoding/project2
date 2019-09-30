package dc.pay.business.huifuzhifu;

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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;


import java.util.*;
import java.util.stream.Collectors;

@RequestPayHandler("HUIFUZHIFU")
public final class HuiFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiFuZhiFuPayRequestHandler.class);


     private static final String      service = "service";           //服务类型		10	int	是	01微信公众号,02微信主扫,03微信反扫,04微信H5,05微信APP,06支付宝服务窗体,07支付宝扫码,08支付宝反扫,09网银,10 QQ H5,11 QQ扫码,12银联扫码,13快捷支付,14支付宝H5
     private static final String      version = "version";           //接口版本		2.0	String	是	固定值
     private static final String      merchant_no = "merchant_no";           //商户编号		5211	String	是	慧富商户编号
     private static final String      total = "total";           //支付金额		100	int	是	不能包含小数点，如100.0应写为100
     private static final String      name = "name";           //订单名称		测试支付	String	是
     private static final String      remark = "remark";           //订单备注		测试支付	String	是
     private static final String      out_trade_no = "out_trade_no";           //订单号		32452356323	String	是	系统唯一
     private static final String      create_ip = "create_ip";           //ip		127.0.0.1	String	是	支付客户端ip
     private static final String      out_notify_url = "out_notify_url";           //通知地址		http://test.huifu.cn/callback	String	是	异步通知地址
     private static final String      nonce_str = "nonce_str";           //随机字符串		ASDFGSAFAG1234	String	是	随机字符串,不长于32位
     private static final String      sign_type = "sign_type";           //签名类型		MD5	是	String
     private static final String      sign = "sign";           //签名		1243ERFGAF423y4rf	是	String	MD5签名,sign =md5(string1&key=6ooycfef).toUpperCase



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(version,"2.0");
            payParam.put(merchant_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(total,channelWrapper.getAPI_AMOUNT());
            payParam.put(name,channelWrapper.getAPI_ORDER_ID());
            payParam.put(remark,channelWrapper.getAPI_MEMBERID());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(create_ip,channelWrapper.getAPI_Client_IP());
            payParam.put(out_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(nonce_str,channelWrapper.getAPI_ORDER_ID());
            payParam.put(sign_type,"MD5");
        }
        log.debug("[慧富支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign;
        Set<String> set = params.keySet().parallelStream().collect(Collectors.toCollection(TreeSet::new));
        StringBuilder str = new StringBuilder("");
        for(String key:set) {
            if(StringUtils.equalsIgnoreCase("sign",key)) {
                continue;
            }
            if(Objects.isNull(params.get(key)) || StringUtils.isBlank(params.get(key))) {
                continue;
            }
            str = str.append(String.format("%s=%s",key,params.get(key)));
            str = str.append("&");
        }
        str = str.append("key=").append(channelWrapper.getAPI_KEY());
        pay_md5sign =  DigestUtils.md5Hex(str.toString()).toUpperCase();
        log.debug("[慧富支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( HandlerUtil.isWY(channelWrapper) ||  HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper) ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("errcode") && "0".equalsIgnoreCase(jsonResultStr.getString("errcode")) && jsonResultStr.containsKey("code_url")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("code_url"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("code_url")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
            }
        } catch (Exception e) { 
             log.error("[慧富支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[慧富支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[慧富支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}