package dc.pay.business.kuaiantongzhifu;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("KUAIANTONGZHIFU")
public final class KuaiAnTongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiAnTongZhiFuPayRequestHandler.class);

     private static final String    versionId = "versionId";   //	服务版本号	必输	 1.1当前
     private static final String    orderAmount = "orderAmount";   //	订单金额	必输	以分为单位
     private static final String    orderDate = "orderDate";   //	订单日期	必输	yyyyMMddHHmmss
     private static final String    currency = "currency";   //	货币类型	必输	RMB：人民币  其他币种代号另行提供
     private static final String    transType = "transType";   //	交易类别	必输	默认填写 0008
     private static final String    asynNotifyUrl = "asynNotifyUrl";   //	异步通知URL
     private static final String    synNotifyUrl = "synNotifyUrl";   //	同步返回URL	必输	针对该交易的交易状态同步通知接收URL
     private static final String    signType = "signType";   //	加密方式	必输	MD5
     private static final String    merId = "merId";   //	商户编号	必输
     private static final String    prdOrdNo = "prdOrdNo";   //	商户订单号	必输
     private static final String    payMode = "payMode";   //	支付方式	必输	00028-支付宝H5
     private static final String    receivableType = "receivableType";   //	到账类型	必输	D00
     private static final String    prdName = "prdName";   //	商品名称	必输
     private static final String    signData = "signData";   //	加密数据	必输
     private static final String    RMB = "RMB";
     private static final String    MD5 = "MD5";
     private static final String    D00 = "D00";




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(versionId,"1.1");
            payParam.put(orderAmount,channelWrapper.getAPI_AMOUNT());
            payParam.put(orderDate,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(currency,RMB);
            payParam.put(transType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(asynNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(synNotifyUrl, channelWrapper.getAPI_WEB_URL() );
            payParam.put(signType, MD5 );
            payParam.put(merId, channelWrapper.getAPI_MEMBERID() );
            payParam.put(prdOrdNo, channelWrapper.getAPI_ORDER_ID() );
            payParam.put(payMode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1] );
            payParam.put(receivableType, D00 );
            payParam.put(prdName, channelWrapper.getAPI_ORDER_ID() );
        }

        log.debug("[快安通支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || signData.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString();//.replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[快安通支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
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
                    if(null!=jsonResultStr && jsonResultStr.containsKey("retCode") && "1".equalsIgnoreCase(jsonResultStr.getString("retCode")) && jsonResultStr.containsKey("htmlText")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("htmlText"))){
                                result.put(HTMLCONTEXT, jsonResultStr.getString("htmlText"));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[快安通支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[快安通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[快安通支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}