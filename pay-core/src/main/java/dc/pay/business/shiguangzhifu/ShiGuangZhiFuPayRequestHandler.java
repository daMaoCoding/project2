package dc.pay.business.shiguangzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * Apr 13, 2019
 */
@RequestPayHandler("SHIGUANGZHIFU")
public final class ShiGuangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShiGuangZhiFuPayRequestHandler.class);

    private static final String uid             ="uid";       //    商户uid    int(10)    必填。您的商户唯一标识，注册后在设置里获得。
    private static final String price           ="price";     //    价格       float    必填。单位：元。精确小数点后2位
    private static final String istype          ="istype";    //    支付渠道    int    必填。1：支付宝；2：微信支付
    private static final String notify_url      ="notify_url";//    通知回调网址    string(255)    必填。用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www .aaa.com/qpay_notify
    private static final String return_url      ="return_url";//    跳转网址       string(255)    必填。用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.aaa .com/qpay_return
    private static final String orderid         ="orderid";   //    商户自定义订单号    string(50)    必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201710192541
    private static final String orderuid        ="orderuid";  //    商户自定义客户号    string(100)    选填。我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。强烈建议填写。可以填用户名，也可以填您数据库中的用户uid。例：xxx, xxx@aaa.com
    private static final String goodsname       ="goodsname"; //    商品名称    string(100)    选填。您的商品名称，用来显示在后台的订单名称。如未设置，我们会使用后台商品管理中对应的商品名称
    private static final String attach          ="attach";    //    附加内容    string(2048)    选填。回调时将会根据传入内容原样返回


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(price,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(istype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(orderuid,channelWrapper.getAPI_ORDER_ID());
                put(goodsname,channelWrapper.getAPI_ORDER_ID());
                put(attach,"attach");
            }
        };
        log.debug("[时光支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //goodsname +“+”+ istype +“+”+ notify_url +“+”+ orderid+“+”+ orderuid +“+”+ price +“+”+ return_url +“+”+ token +“+”+ uid
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(goodsname)).append("+");
        signSrc.append(api_response_params.get(istype)).append("+");
        signSrc.append(api_response_params.get(notify_url)).append("+");
        signSrc.append(api_response_params.get(orderid)).append("+");
        signSrc.append(api_response_params.get(orderuid)).append("+");
        signSrc.append(api_response_params.get(price)).append("+");
        signSrc.append(api_response_params.get(return_url)).append("+");
        signSrc.append(channelWrapper.getAPI_KEY()).append("+");
        signSrc.append(api_response_params.get(uid));
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[时光支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[时光支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[时光支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[时光支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}