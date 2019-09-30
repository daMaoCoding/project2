package dc.pay.business.qbzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.*;
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

/**
 * @author Cobby
 * Apr 26, 2019
 */
@RequestPayHandler("QBPAYZHIFU")
public final class QBZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QBZhiFuPayRequestHandler.class);

    private static final String merchantno         ="merchantno"; //商户编号    merchantno    是    String    商户编号
    private static final String customno           ="customno";   //商户订单号    customno    是    String    商户订单号
    private static final String productname        ="productname";//产品名称    productname    是    String    产品名称
    private static final String money              ="money";      //支付金额    money    是    String    支付金额,单位（元）注意：请输入个位不为零的整数或两位小数
    private static final String stype              ="stype";      //收款方式    stype    是    String    参考附录4.2收款编码
    private static final String timestamp          ="timestamp";  //时间戳    timestamp    是    String    例如：1512475188571
    private static final String notifyurl          ="notifyurl";  //通知地址    notifyurl    是    String    通知回调地址
    private static final String buyerip            ="buyerip";    //用户IP    buyerip    是    String


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantno, channelWrapper.getAPI_MEMBERID());
                put(customno,channelWrapper.getAPI_ORDER_ID());
                put(productname,channelWrapper.getAPI_ORDER_ID());
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(stype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(timestamp,System.currentTimeMillis()/1000+"");
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(buyerip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[QBPay支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //origin＝merchantno+"|"+customno+"|"+ stype+"|"+notifyurl+"|"+money+"|"+timestamp+"|"+buyerip+"|"+md5key
//        //使用对方返回的数据进行签名
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                api_response_params.get(merchantno),
                api_response_params.get(customno),
                api_response_params.get(stype),
                api_response_params.get(notifyurl),
                api_response_params.get(money),
                api_response_params.get(timestamp),
                api_response_params.get(buyerip),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[QBPay支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[QBPay支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))
                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    jsonObject = JSONObject.parseObject(jsonObject.getString("data"));
                    String code_url = jsonObject.getString("scanurl");
                    result.put( JUMPURL , code_url);
                    //if (handlerUtil.isWapOrApp(channelWrapper)) {
                    //    result.put(JUMPURL, code_url);
                    //}else{
                    //    result.put(QRCONTEXT, code_url);
                    //}
                }else {
                    log.error("[QBPay支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[QBPay支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[QBPay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[QBPay支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}