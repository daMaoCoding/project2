package dc.pay.business.xinzhifu;

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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XINZHIFU")
public final class XinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinZhiFuPayRequestHandler.class);

    private static final String     mchno	 = "mchno";                //商户号，由我方提供
    private static final String     pay_type = "pay_type";             //
    private static final String     price	 = "price";               //商品价格（单位/分）
    private static final String     bill_title	 = "bill_title";       //商品标题
    private static final String     bill_body	 = "bill_body";       //商品描述
    private static final String     nonce_str	 = "nonce_str";       //32位以内随机字符串，只包含字母和数字
    private static final String     ip	  = "ip";                   //支付设备ip，（非必填，若使用微信WAP则为必填）
    private static final String     notify_url	 = "notify_url";    //通知回调地址（非必填，多服务器使用，单服务器提供链接后台绑定即可）
    private static final String     sign	 = "sign";               //验签，查看以下加密方式
    private static final String     linkId	 = "linkId";               //我的订单号




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mchno,channelWrapper.getAPI_MEMBERID() );
            payParam.put(pay_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(price,channelWrapper.getAPI_AMOUNT() );
            payParam.put(bill_title,bill_title );
            payParam.put(bill_body,bill_body );
            payParam.put(nonce_str, HandlerUtil.getRandomStr(10));
            payParam.put(ip, channelWrapper.getAPI_Client_IP());
            payParam.put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(linkId, channelWrapper.getAPI_ORDER_ID());
        }

        log.debug("[鑫支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[鑫支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        try {
//            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//                payResultList.add(result);
//            }else{

                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);

            if(1==1 ) {  //扫码（银联扫码&微信&支付宝&QQ）
                    if(null!=jsonResultStr && jsonResultStr.containsKey("resultCode") && "200".equalsIgnoreCase(jsonResultStr.getString("resultCode")) && jsonResultStr.containsKey("order")){
                        JSONObject jsonRrder = jsonResultStr.getJSONObject("order");
                        if(null!=jsonRrder && jsonRrder.containsKey("pay_link") && StringUtils.isNotBlank( jsonRrder.getString("pay_link")    )){
                            String pay_Link = jsonRrder.getString("pay_link");

                            if(pay_Link.contains("qrcode?uuid") ){
                                result.put(QRCONTEXT, URLDecoder.decode(pay_Link.split("uuid=")[1]));
                                payResultList.add(result);
                            }else{
                                if(!HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) && channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM")){
                                     result.put(QRCONTEXT, pay_Link);
                                }else{
                                     result.put(JUMPURL, pay_Link);
                                }
                                payResultList.add(result);
                            }
                        }
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[鑫支付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
//            }
        } catch (Exception e) {
            log.error("[鑫支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[鑫支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[鑫支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}