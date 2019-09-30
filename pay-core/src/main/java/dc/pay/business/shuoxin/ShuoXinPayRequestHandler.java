package dc.pay.business.shuoxin;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("SHUOXINZHIFU")
public final class ShuoXinPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShuoXinPayRequestHandler.class);

     private static final String       merchantNo = "merchantNo";   //	商户号	Y	Varchar	由平台下发的商户号
     private static final String       orderAmount = "orderAmount";   //	订单金额	Y	Varchar	订单交易金额，保留2位小数。如100.21，单位：元
     private static final String       service = "service";   //	数据字典（Service）    Y	Varchar	不同业务不同，详见数据字典（Service）
     private static final String       merchantOrderNo = "merchantOrderNo";   //	商户订单号	Y	Varchar	商户平台订单号，由商户平台产生的，必须保证唯一
     private static final String       requestIp = "requestIp";   //	请求终端IP	Y	Varchar	请求下单的用户终端的IP地址
     private static final String       orderTitle = "orderTitle";   //	订单标题	Y	Varchar	订单标题
     private static final String       notifyUrl = "notifyUrl";   //	订单通知URL	Y	Varchar	订单结果通知地址，必须是完整的URL
     private static final String       attach = "attach";   //	附加字段	N	Varchar	Json串，用于扩展保留字段，非强制字段(微信H5交易需强制上传，上传内容为交易场景信息，格式为：{sceneInfo:’场景’})
     private static final String       sign = "sign";   //	签名	Y	Varchar	详见签名规则





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(merchantOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(requestIp,channelWrapper.getAPI_Client_IP());
            payParam.put(orderTitle,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
           // payParam.put(attach,"1");
        }

        log.debug("[烁昕支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append(channelWrapper.getAPI_KEY());
        String signStr = sb.toString();//.replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[烁昕支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "1".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("result")){
                        JSONObject jsonRes = jsonResultStr.getJSONObject("result");
                        if(null!=jsonRes && StringUtils.isNotBlank(jsonRes.getString("payInfo"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, jsonRes.getString("payInfo"));
                                }else{
                                    result.put(QRCONTEXT, jsonRes.getString("payInfo"));
                                }
                                payResultList.add(result);
                            }else{
                                throw new PayException(resultStr);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[烁昕支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[烁昕支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[烁昕支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}