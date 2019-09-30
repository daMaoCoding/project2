package dc.pay.business.shunfu;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.HttpUtil;
import dc.pay.utils.ValidateUtil;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * ************************
 * @author tony 3556239829
 */
@RequestPayHandler("SHUNFU")
public class ShunfuPayRequestHandler extends PayRequestHandler {

    private static final Logger log =  LoggerFactory.getLogger(ShunfuPayRequestHandler.class);
    private static  final String  GOODSINFO= "充值";
    private static final  String RESULT_MERNO = "merNo";
    private static final  String RESULT_ORDERNO = "orderNo";
    private static final  String RESULT_QRCODEINFO = "qrcodeInfo";
    private static final String JUMPURL = "JUMPURL";
    private static final String scanType = "scanType";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put("merNo",channelWrapper.getAPI_MEMBERID());
                put("payNetway", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put("random", "c72h");
                put("orderNo",channelWrapper.getAPI_ORDER_ID());
                put("amount", channelWrapper.getAPI_AMOUNT());
                put("goodsInfo", GOODSINFO);
                put("callBackUrl",channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                // put("callBackViewUrl", null);// 回显地址 todo
                put("clientIP",channelWrapper.getAPI_Client_IP());
                if(HandlerUtil.isFS(channelWrapper)){
                    put(scanType,"Page");
                }

            }
        };
        log.debug("[瞬付]-[请求支付]-1.组装请求参数完成："+JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam) throws PayException {
        String metaSignJsonStr = JSON.toJSONString(payParam);
               metaSignJsonStr+= channelWrapper.getAPI_KEY();
        String md5UpperCase = HandlerUtil.getMD5UpperCase(metaSignJsonStr, "UTF-8");
        log.debug("[瞬付]-[请求支付]-2.生成加密URL签名完成："+ JSON.toJSONString(md5UpperCase));
        return md5UpperCase;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try{
            String payParamStr = "data="+JSON.toJSONString(payParam);
           // String resultJsonStr = HttpUtil.sendPostForString(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParamStr);
            //代理访问
            String resultJsonStr = HttpUtil.sendPostForStringProxy(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParamStr);
            JSONObject resultJsonObj = JSONObject.fromObject(resultJsonStr);
            String stateCode = resultJsonObj.getString("resultCode");
            if (!stateCode.equals("00")) {
                String resultMsg = resultJsonObj.getString("resultMsg");
                log.error("[瞬付请求支付订单创建失败]-["+channelWrapper.getAPI_CHANNEL_BANK_NAME()+"]-["+channelWrapper.getAPI_ORDER_ID()+"]-"+resultMsg);
                throw new PayException(resultMsg);
            }


            String resultSign = resultJsonObj.getString("sign");
            resultJsonObj.remove("sign");
            String targetString = HandlerUtil.getMD5UpperCase(resultJsonObj.toString() + channelWrapper.getAPI_KEY(), "UTF-8");
            if (targetString.equals(resultSign)) {
                HashMap<String, String> resultMap = Maps.newHashMap();
                resultMap.put(RESULT_MERNO, resultJsonObj.getString(RESULT_MERNO));
                resultMap.put(RESULT_ORDERNO, resultJsonObj.getString(RESULT_ORDERNO));
                if(HandlerUtil.isFS(channelWrapper)|| HandlerUtil.isWapOrApp(channelWrapper)) {
                    resultMap.put(JUMPURL, resultJsonObj.getString(RESULT_QRCODEINFO));
                }else{
                    resultMap.put(RESULT_QRCODEINFO, resultJsonObj.getString(RESULT_QRCODEINFO));
                }
                payResultList.add(resultMap);
            }else{
                log.error("[注意：瞬付请求支付订单创建失败]-["+channelWrapper.getAPI_CHANNEL_BANK_NAME()+"]-["+channelWrapper.getAPI_ORDER_ID()+"]-"+"签名验证失败。");
                throw new PayException(SERVER_MSG.RESPONSE_PAY_VALDATA_SIGN_ERROR);
            }
        } catch (Exception e) {
            log.error("[瞬付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败，"+ e.getMessage());
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[瞬付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功："+ JSON.toJSONString(payResultList));
        return payResultList;
    }
    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> result) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(!result.isEmpty() && result.size()==1){
            requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
            requestPayResult.setRequestPayOrderId(result.get(0).get(RESULT_ORDERNO));
            requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            requestPayResult.setRequestPayQRcodeURL(null);
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());

            if(result.get(0).containsKey(JUMPURL)){
                 requestPayResult.setRequestPayJumpToUrl(result.get(0).get(JUMPURL));
            }else if(result.get(0).containsKey(RESULT_QRCODEINFO)){
                 requestPayResult.setRequestPayQRcodeContent(result.get(0).get(RESULT_QRCODEINFO));
            }
            if(ValidateUtil.requestesultValdata(requestPayResult)){
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            }else{
                throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
         log.debug("[瞬付]-[请求支付]-4.处理请求响应成功："+ JSON.toJSONString(requestPayResult));
         return requestPayResult;
    }
}