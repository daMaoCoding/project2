package dc.pay.business.hongniuzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.*;
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("HONGNIUZHIFU")
public final class HongNiuZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);




     private static final String  notify_url = "notify_url";    //	支付成功后通知的地址。
     private static final String  return_url = "return_url";    //	支付成功后跳转的地址。注：此地址应只作为支付成功的提示界面，实际支付验证应在notify_url里处理。
     private static final String  user_account = "user_account";    //	商户在红牛支付平台的账号，即登录支付平台的账号。
     private static final String  out_trade_no = "out_trade_no";    //	商户系统内部订单号，只能是数字、大小写字母，且在同一个商户号下唯一。
     private static final String  payment_type = "payment_type";    //	wxpay(微信)，wxwap(微信wap)， alipay(支付宝)，aliwap(支付宝wap)，qqpay(QQ)，qqwap(QQwap)
     private static final String  total_fee = "total_fee";    //	订单总金额，单位为元，最多保留两位小数。
     private static final String  trade_time = "trade_time";    //	订单生成时间，格式为yyyy-MM-dd HH:mm:ss。例 2018-05-07 11：21：33。
     private static final String  body = "body";    //	订单简单描述。
     private static final String  sign = "sign";    //	通过签名算法计算得出的签名值，详见3.5 签名机制。



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(user_account,channelWrapper.getAPI_MEMBERID());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(payment_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(trade_time,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss") );
            payParam.put(body,channelWrapper.getAPI_ORDER_ID() );
        }
        log.debug("[红牛支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[红牛支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        String QrContentStr = null;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                if(StringUtils.isNotBlank(resultStr)){
                    Document document = Jsoup.parse(resultStr);
                    Element aEl = document.select("img#qrImg").first();
                    if(null!=aEl && StringUtils.isNotBlank(aEl.attr("src"))) {
                        QrContentStr = QRCodeUtil.decodeByBase64(aEl.attr("src"));
                    }else{throw new PayException("无法解析获取二维码："+resultStr);}
                }else{throw new PayException(EMPTYRESPONSE);}
                if(StringUtils.isBlank(QrContentStr)) throw new PayException(resultStr);
                if(HandlerUtil.isWapOrApp(channelWrapper)){
                    result.put(JUMPURL, QrContentStr);
                }else{
                    result.put(QRCONTEXT, QrContentStr);
                }
                payResultList.add(result);
            }
        } catch (Exception e) { 
             log.error("[红牛支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[红牛支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[红牛支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}