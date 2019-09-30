package dc.pay.business.aoyinzhifu;

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

import java.util.*;

/**
 * @author sunny
 * May 18, 2019
 */
@RequestPayHandler("AOYINZHIFU")
public final class AoYinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AoYinZhiFuPayRequestHandler.class);
    
    private static final String token                 ="token";               //  1,W6R6BVFABZKZYUE2    商户ID + ‘,’ + 密钥
    private static final String businessnoencryption  ="businessnoencryption";//  1    传1代表不加密,不传代表加密,建议加密的方式
    private static final String amount                ="amount";              //  是    decimal(16,3)    订单金额
    
    private static final String paytypeid             ="paytypeid";           //  是    integer    支付方式ID,通过支付方式接口获取
    private static final String down_ordercode        ="down_ordercode";      //  是    String    订单号
    private static final String createtime            ="createtime";          //  是    bigint    订单创建时间 ,时间戳
    private static final String client_ip             ="client_ip";           //  是    String    客户端IP
    private static final String notifyurl             ="notifyurl";           //  是    String    回调地址,回调方法同意为POST
    private static final String ismobile              ="ismobile";            //  是    String    是否手机标志,0-手机,1-PC


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(paytypeid,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(down_ordercode,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(createtime,System.currentTimeMillis()/1000+"");
                put(client_ip, channelWrapper.getAPI_Client_IP());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ismobile,handlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())?"0":"1");
            }
        };
        log.debug("[傲银支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        Map<String,Object> response_params = new HashMap<>();
        response_params.put(paytypeid,Long.valueOf(api_response_params.get(paytypeid)));
        response_params.put(down_ordercode,api_response_params.get(down_ordercode));
        response_params.put(amount,Double.valueOf(api_response_params.get(amount)));
        response_params.put(createtime,Long.valueOf(api_response_params.get(createtime)));
        response_params.put(client_ip,api_response_params.get(client_ip));
        response_params.put(notifyurl,api_response_params.get(notifyurl));
        response_params.put(ismobile,api_response_params.get(ismobile));
        String params = JSON.toJSONString(response_params);
        AoYinZhiFuAesCBC aoYinZhiFuAesCBC = new AoYinZhiFuAesCBC();
        String encrypt = null;
        try {
            encrypt = aoYinZhiFuAesCBC.encrypt(params,"utf-8", channelWrapper.getAPI_KEY(), channelWrapper.getAPI_KEY());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String signMd5 =encrypt;
        log.debug("[傲银支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.clear();
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        HashMap<String, String> HeaderMap = Maps.newHashMap();
        HeaderMap.put(token,channelWrapper.getAPI_MEMBERID()+","+channelWrapper.getAPI_KEY());
        HeaderMap.put(businessnoencryption,"");
        HeaderMap.put("Content-Type","application/json");
        String url = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/create_order";
        try {
            String resultStr = RestTemplateUtil.postStr(url, JSON.toJSONString(payParam), HeaderMap);
            resultStr = new String(resultStr.getBytes("ISO8859-1"),"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[傲银支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("rescode") && "10000".equalsIgnoreCase(jsonObject.getString("rescode"))) {
                	AoYinZhiFuAesCBC aoYinZhiFuAesCBC = new AoYinZhiFuAesCBC();
                	String encryptStr = aoYinZhiFuAesCBC.decrypt(jsonObject.get("data").toString(),"utf-8", channelWrapper.getAPI_KEY(), channelWrapper.getAPI_KEY());
                	JSONObject rutJson=JSONObject.parseObject(encryptStr);
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, rutJson.getString("path"));
                    //if (handlerUtil.isWapOrApp(channelWrapper)) {
                    //    result.put(JUMPURL, code_url);
                    //}else{
                    //    result.put(QRCONTEXT, code_url);
                    //}
                }else {
                    log.error("[傲银支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[傲银支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[傲银支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[傲银支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}