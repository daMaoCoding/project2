package dc.pay.business.qiqi;

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
import org.springframework.http.*;

import java.util.*;

@RequestPayHandler("QIQI")
public final class QiQiPayRequestHandler extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(QiQiPayRequestHandler.class);
    static final  String QRCODECONTENT = "QRcodeContent";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new HashMap<String, String>() {
            {
                put("notifyUrl",channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put("outOrderNo", channelWrapper.getAPI_ORDER_ID());
                put("goodsClauses","商品名称");
                put("tradeAmount",HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put("code",channelWrapper.getAPI_MEMBERID());
                put("payCode",channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[奇奇支付]-[请求支付]-1.组装请求参数完成："+JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map payParam) throws PayException {
        SortedMap<Object,Object> sortMap = new TreeMap<>();
        sortMap.put("outOrderNo",payParam.get("outOrderNo"));
        sortMap.put("goodsClauses",payParam.get("goodsClauses"));
        sortMap.put("tradeAmount",payParam.get("tradeAmount"));
        String sign = Main.createSign(channelWrapper.getAPI_KEY(), "UTF-8", sortMap);
        log.debug("[奇奇支付]-[请求支付]-2.生成加密URL签名完成："+ JSON.toJSONString(sign));
        return sign;
    }
    protected List<Map<String,String>> sendRequestGetResult(Map<String, String> payParam,String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try {
           String result = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            if(StringUtils.isNotBlank(result)){
                JSONObject jsonObject = JSON.parseObject(result);
                String payState = jsonObject.getString("payState");
                if(StringUtils.isBlank(payState) || !"success".equalsIgnoreCase(payState)){
                    throw new PayException(jsonObject.getString("message"));
                }
                HashMap<String, String> resultJson = Maps.newHashMap();
                resultJson.put("resultJson",result);
                payResultList.add(resultJson);

                String qrContext = jsonObject.getString("url");
                if(StringUtils.isNotBlank(qrContext)){
                    HashMap<String, String> QrContent = Maps.newHashMap();
                    QrContent.put(QRCODECONTENT,qrContext);
                    payResultList.add(QrContent);
                }
            }else{
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
            }
        } catch (Exception e) {
            log.error("[奇奇支付]3.发送支付请求，及获取支付请求结果出错：",e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[奇奇支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功："+ JSON.toJSONString(payResultList));
        return payResultList;
    }
    protected RequestPayResult buildResult(List<Map<String,String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(!resultListMap.isEmpty() && resultListMap.size()==2){
            for(Map<String,String> resultMap:resultListMap){
                if(resultMap.containsKey(QRCODECONTENT)){
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(channelWrapper.getAPI_OrDER_TIME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCODECONTENT));
                }
            }
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            if(ValidateUtil.requestesultValdata(requestPayResult)){
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            }else{
                throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[奇奇支付]-[请求支付]-4.处理请求响应成功："+ JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}