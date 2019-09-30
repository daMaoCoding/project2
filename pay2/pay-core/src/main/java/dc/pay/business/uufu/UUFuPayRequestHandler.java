package dc.pay.business.uufu;

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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.net.URLDecoder;
import java.util.*;

@RequestPayHandler("UUFU")
public final class UUFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(UUFuPayRequestHandler.class);
 
    private static final String  merchantNo	  ="merchantNo";    //String	是
    private static final String  outTradeNo	  ="outTradeNo";    //String	是
    private static final String  amount	  ="amount";    //Long	是  单位分
    private static final String  content	  ="content";    //String	是  交易主题
    private static final String  payType	  ="payType";    //String	是
    private static final String  returnURL	  ="returnURL";    //String	是 同步回调地址
    private static final String  callbackURL	  ="callbackURL";    //String	是 异步回调地
    private static final String  DEBIT_BANK_CARD_PAY	  ="DEBIT_BANK_CARD_PAY";    //储蓄卡(借记卡)支付
    private static final String  sign	  ="sign";    //String	是


     private static final String   defaultBank	   ="defaultBank"; //String	否 银行编码
     private static final String   currency	   ="currency"; //String	否  CNY
     private static final String   outContext	   ="outContext"; //Long	否  外部上下文，用户自己设置
     private static final String   returnType	   ="returnType"; //String	否  默认返回JSON。


 

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY")){
                    put(defaultBank ,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(payType, DEBIT_BANK_CARD_PAY);
                }else{
                    put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());

                }
                put(merchantNo,channelWrapper.getAPI_MEMBERID());
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                put(amount,channelWrapper.getAPI_AMOUNT());
                put(content,"PAY");
                put(currency,"CNY");
                put(outContext,"PAY");
                put(returnType,"JSON");
                put(returnURL,channelWrapper.getAPI_WEB_URL());
                put(callbackURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                //put(create_time,new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
                //put("out_trade_no", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
                //put(body,"PAYBody");
            }
        };
        log.debug("[UU付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map params) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        signSrc.append("amount=").append(params.get(amount)).append("&");
        signSrc.append("callbackURL=").append(params.get(callbackURL)).append("&");
        signSrc.append("content=").append(params.get(content)).append("&");
        signSrc.append("currency=").append(params.get(currency)).append("&");
        if(params.containsKey(defaultBank)){
            signSrc.append("defaultBank=").append(params.get(defaultBank)).append("&");
        }
        signSrc.append("merchantNo=").append(params.get(merchantNo)).append("&");
        signSrc.append("outContext=").append(params.get(outContext)).append("&");
        signSrc.append("outTradeNo=").append(params.get(outTradeNo)).append("&");
        signSrc.append("payType=").append(params.get(payType)).append("&");
        signSrc.append("returnType=").append("JSON").append("&");
        signSrc.append("returnURL=").append(params.get(returnURL));
        String signInfo = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY(),"SHA1withRSA");	// 签名
            //signMd5  = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY());
            //signMd5  = RsaUtil.signByPrivateKey2(signInfo,channelWrapper.getAPI_KEY());
           // signMd5  = RsaUtil.signByPublicKey(signInfo,channelWrapper.getAPI_PUBLIC_KEY());

        } catch (Exception e) {
            log.error("[UU付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        //String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[UU付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
                HashMap<String, String> result = Maps.newHashMap();
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
                payResultList.add(result);
            }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            JSONObject resJson = JSONObject.parseObject(resultStr);
            if (resJson!=null && resJson.containsKey("paymentInfo") && StringUtils.isNotBlank(resJson.getString("paymentInfo"))) {
                  String code_url = resJson.getString("paymentInfo");
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(QRCONTEXT, code_url);
                    payResultList.add(result);

            } else {
                log.error("[UU付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.toJSONString(URLDecoder.decode((String) resultStr, "UTF-8")));
            }

        }

        } catch (Exception e) {
            log.error("[UU付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[UU付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(HTMLCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
                }
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[UU付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}