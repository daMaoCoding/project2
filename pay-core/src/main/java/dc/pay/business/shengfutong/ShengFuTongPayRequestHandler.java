package dc.pay.business.shengfutong;

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
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("SHENGFUTONGZHIFU")
public final class ShengFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShengFuTongPayRequestHandler.class);


    private static  final String action="action";   // 请求方式      string 微信：WxCode/WxSao/WxJsApi  支付宝：AliCode/AliSao  QQ支付：QQCode/QQSao 银联：USao N
    private static  final String txnamt="txnamt";   // 交易金额      int  订单金额，单位为分 N
    private static  final String merid="merid";   // 商户号        string  商户号，接入手机支付平台时分配 N
    private static  final String orderid="orderid";   // 商户订单号    string 由商户生成，必需唯一，长度8-32位，由字母和数    N
    private static  final String backurl="backurl";   // 通知URL       string

     private static  final String sign="sign";  //req+商户密钥,进行md5加密（小写）
     private static  final String req="req";   //req为json格式的业务请求信息做Base64处理


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newLinkedHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(action,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(merid,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(txnamt, channelWrapper.getAPI_AMOUNT());
            payParam.put(backurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        Map<String, String> reqParam = Maps.newHashMap();
        reqParam.put(req, Base64.encodeToString(JSON.toJSONString(payParam).getBytes()));
        log.debug("[盛付通支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(reqParam));
        return reqParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String signMd5 = HandlerUtil.getMD5UpperCase(params.get(req)+channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[盛付通支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
       payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper) && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("resp")  && null!=jsonResultStr.getString("resp")){
                        JSONObject resp = JSONObject.parseObject(Base64.decodeToString(jsonResultStr.getString("resp").getBytes())  );
                        //二维码内容 , formaction  string  当respcode=00且action=WxSao/AliSao返回此域
                        if(null!=resp && resp.containsKey("respcode") && "00".equalsIgnoreCase(resp.getString("respcode")) && resp.containsKey("formaction")  &&  StringUtils.isNotBlank(resp.getString("formaction"))){
                                result.put(QRCONTEXT,  resp.getString("formaction"));
                                payResultList.add(result);
                            }else { throw new PayException(resp.toJSONString()); }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[盛付通支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[盛付通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[盛付通支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}