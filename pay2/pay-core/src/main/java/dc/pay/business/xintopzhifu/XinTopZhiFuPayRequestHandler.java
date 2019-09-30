package dc.pay.business.xintopzhifu;

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

import java.util.*;

/**
 * @author Cobby
 * May 31, 2019
 */
@RequestPayHandler("XINTOPZHIFU")
public final class XinTopZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinTopZhiFuPayRequestHandler.class);

    private static final String Version                ="Version";    //版本号  string (5) 否 默认 1.3
    private static final String Memid                  ="Memid";      //商户编号  string (8) 否 商户后台获取
    private static final String outTradeNo             ="outTradeNo"; //商户订单号  string (14,30) 否    商户生成的订单号14-30 位
    private static final String Amount                 ="Amount";     //订单金额  string (10,2) 否    单位元，最多两位小数，例如 128.42
    private static final String tradeType              ="tradeType";  //支付编号  string (10) 否 详见附录 1
    private static final String tradeTime              ="tradeTime";  //时间戳  string (14) 否 单位秒
    private static final String bankCode               ="bankCode";   //银行编号  string (10)    网银不可为空，其他支付方式填写tradeType 相同值  详见附录 2
    private static final String NotifyUrl              ="NotifyUrl";  //异步通知 URL  string (50) 否 不能带有任何参数
    private static final String ReturnUrl              ="ReturnUrl";  //同步跳转 URL  string (50) 否 不能带有任何参数
    private static final String Body                   ="Body";       //订单备注说明  string (50) 否


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(Version,"1.3");
                put(Memid, channelWrapper.getAPI_MEMBERID());
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                put(Amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(tradeType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(tradeTime,System.currentTimeMillis()/1000+"");
                put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ReturnUrl,channelWrapper.getAPI_WEB_URL());
                put(Body,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[新TOP支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> params) throws PayException {
//        'Amount='.$Amount.'&bankCode='.$bankCode.'&Memid='.$Memid.'&NotifyUrl='.$NotifyUrl.'&outTradeNo='.$outTradeNo.'&Retur
//        nUrl='.$ReturnUrl.'&tradeTime='.$tradeTime.'&tradeType='.$tradeType.'&Version='.$Version.'&'.$key
        StringBuffer signSrc= new StringBuffer();
        signSrc.append("Amount=").append(params.get(Amount)).append("&");
        signSrc.append("bankCode=").append(params.get(bankCode)).append("&");
        signSrc.append("Memid=").append(params.get(Memid)).append("&");
        signSrc.append("NotifyUrl=").append(params.get(NotifyUrl)).append("&");
        signSrc.append("outTradeNo=").append(params.get(outTradeNo)).append("&");
        signSrc.append("ReturnUrl=").append(params.get(ReturnUrl)).append("&");
        signSrc.append("tradeTime=").append(params.get(tradeTime)).append("&");
        signSrc.append("tradeType=").append(params.get(tradeType)).append("&");
        signSrc.append("Version=").append(params.get(Version)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新TOP支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                resultStr = UnicodeUtil.unicodeToString(resultStr);
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[新TOP支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))
                        && jsonObject.containsKey("url") && StringUtils.isNotBlank(jsonObject.getString("url"))) {
                    String code_url = jsonObject.getString("url");
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                    if (handlerUtil.isWapOrApp(channelWrapper)) {
                        result.put(JUMPURL, code_url);
                    }else{
                        result.put(QRCONTEXT, code_url);
                    }
                }else {
                    log.error("[新TOP支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        } catch (Exception e) {
            log.error("[新TOP支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新TOP支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新TOP支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}