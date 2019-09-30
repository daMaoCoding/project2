package dc.pay.business.zhongshangtong;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

@RequestPayHandler("ZHONGSHANGTONG")
public final class ZhongShangTongPayRequestHandler extends PayRequestHandler {
    private final Logger log =  LoggerFactory.getLogger(ZhongShangTongPayRequestHandler.class);

    private static final  String userid = "userid";
    private static final  String orderid = "orderid";
    private static final  String money = "money"; //元
    private static final  String url = "url";  //回调
    private static final  String bankid = "bankid";
    private static final  String sign = "sign";
    private static final  String sign2 = "sign2"; //否

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> paramsMap = new HashMap<String,String>(){{
            put(userid,channelWrapper.getAPI_MEMBERID());
            put(orderid,channelWrapper.getAPI_ORDER_ID());
            put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            put(url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            put(bankid,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //  put(outputmode,"json");

        }};
        log.debug("[中商通支付]-[请求支付]-1.组装请求参数完成："+JSON.toJSONString(paramsMap));
        return paramsMap;

    }
    protected String buildPaySign(Map<String,String> payParam) throws PayException {
        // sign:userid={}&orderid={}&bankid={}&keyvalue={}
        // sign2:money={}&userid={}&orderid={}&bankid={}&keyvalue={}
        String paramsStr1 = String.format("userid=%s&orderid=%s&bankid=%s&keyvalue=%s",
                payParam.get(userid),  payParam.get(orderid), payParam.get(bankid),channelWrapper.getAPI_KEY()
        );
        String sign1 = HandlerUtil.getMD5UpperCase(paramsStr1).toLowerCase();

        String paramsStr2 = String.format("money=%s&userid=%s&orderid=%s&bankid=%s&keyvalue=%s",
                payParam.get(money), payParam.get(userid),  payParam.get(orderid), payParam.get(bankid),channelWrapper.getAPI_KEY()
        );
        String sign2 = HandlerUtil.getMD5UpperCase(paramsStr2).toLowerCase();
        log.debug("[中商通支付]-[请求支付]-2.生成加密URL签名完成：" + sign1.concat(",").concat(sign2));
        return sign1.concat(",").concat(sign2);

    }
    protected List<Map<String,String>> sendRequestGetResult(Map<String, String> payParam,String pay_md5sign) throws PayException {
        LinkedList<Map<String,String>> payResultList = Lists.newLinkedList();
        Map result = Maps.newHashMap();
        payParam.put(sign, pay_md5sign.split(",")[0]);
        payParam.put(sign2, pay_md5sign.split(",")[1]);
        if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)){
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());
        } else{
            String httpResult=null;
        	if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_YL_")){
        		httpResult= RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();  //第一次提交
        	}else {
        		httpResult= RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();  //第一次提交
			}
            if (StringUtils.isBlank(httpResult)) {
            	log.error("[中商通支付]3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
            	throw new PayException("第三方返回异常:返回空"+",参数："+JSON.toJSONString(payParam));
            }
            Document document = Jsoup.parse(httpResult);
            Element bodyEl = document.getElementsByTag("body").first();
            Element formEl = bodyEl.getElementsByTag("form").first();
            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
            String bankUrl = channelWrapper.getAPI_CHANNEL_BANK_URL();
            if(secondPayParam!=null && secondPayParam.get(ACTION).startsWith("http")) bankUrl = secondPayParam.get(ACTION);
            try {
            	httpResult =  HandlerUtil.sendToThreadPayServ(secondPayParam,bankUrl).getBody(); //二次提交
            } catch (Exception e) {
                log.error("[中商通支付]3.2..发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL());
                throw new PayException(httpResult,e);
            }
            if (StringUtils.isBlank(httpResult)) {
            	log.error("[中商通支付]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL());
            	throw new PayException("第三方返回异常:返回空"+",参数："+JSON.toJSONString(payParam));
            }
            document = Jsoup.parse(httpResult);
            bodyEl = document.getElementsByTag("body").first();
            formEl = bodyEl.getElementsByTag("form").first();
            Elements select = Jsoup.parse(httpResult).select("div.divCode img ");
            select = ((null == select || select.size() < 1) ? Jsoup.parse(httpResult).select("div.divCode_zfb img ") : select);
            String src = select.first().attr("src");
            String qrSrc = (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WX_") || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_ZFB_")) ? src : HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_URL()).concat(src);
            result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(qrSrc));
        }
        payResultList.add(result);
        log.debug("[中商通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功："+ JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String,String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(null!=resultListMap && !resultListMap.isEmpty() && resultListMap.size()==1){
            Map<String, String> result = resultListMap.get(0);
            requestPayResult =  buildResult(result,channelWrapper,requestPayResult);
            requestPayResult.setDetail(resultListMap);
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        if(ValidateUtil.requestesultValdata(requestPayResult)){
            requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        log.debug("[中商通支付]-[请求支付]-4.处理请求响应成功："+ JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}