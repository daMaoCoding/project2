package dc.pay.business.wanglianfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 16, 2019
 */
@RequestPayHandler("WANGLIANFU")
public final class WangLianFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WangLianFuPayRequestHandler.class);

    //字段  名称  是否必填    长度  值   加入签名
    //pay_returntype  返回方式    否       0   否
    //pay_memberid    商户号 是           是
    //pay_orderid 订单号 是   50      是
    //pay_amount  金额  是       单位（元）   是
    //pay_applydate   订单时间    是       YY-mm-dd HH:ii:ss   是
    //pay_bankcode    通道编码    是       见说明 是
    //pay_notifyurl   服务器通知地址 是           是
    //pay_callbackurl 页面返回地址  是           是
    //pay_productname 商品名称    否           否
    //pay_md5sign 签名  是           否
    private static final String pay_returntype            ="pay_returntype";
    private static final String pay_memberid            ="pay_memberid";
    private static final String pay_orderid             ="pay_orderid";
    private static final String pay_amount              ="pay_amount";
    private static final String pay_applydate           ="pay_applydate";
    private static final String pay_bankcode            ="pay_bankcode";
    private static final String  pay_notifyurl          ="pay_notifyurl";
    private static final String pay_callbackurl         ="pay_callbackurl";
//  private static final String pay_productname         ="pay_productname";
//  private static final String pay_md5sign             ="pay_md5sign";

    //signature 数据签名    32  是   　
//  private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_returntype,"0");
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_applydate, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(pay_bankcode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[网联付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_returntype.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //去除最后一个&符
        //paramsStr = paramsStr.substring(0,paramsStr.length()-1);
        //删除最后一个字符
        signSrc.append("key="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[网联付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (handlerUtil.isZfbSM(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) {
          //支付宝扫码，不允许电脑上直接扫码  这方法靠谱
            if ((handlerUtil.isWEBWAPAPP_SM(channelWrapper) && HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())) || 
                channelWrapper.getAPI_ORDER_ID().startsWith("T") || 
                handlerUtil.isWapOrApp(channelWrapper)) {
                StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                //保存第三方返回值
                result.put(HTMLCONTEXT,htmlContent.toString());
            }else {
                throw new PayException("请在APP或者WAP应用上使用通道......");
            }
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[网联付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            Elements elements = Jsoup.parse(resultStr).select("[id=ewm]");
            if (null == elements || elements.size() != 1) {
                log.error("[网联付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            String src = elements.first().attr("src");
            if (StringUtils.isBlank(src)) {
                log.error("[网联付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            String qr = QRCodeUtil.decodeByUrl(src.startsWith("http") ? src : "http://www.wanglianfu.com/"+src);
            if (StringUtils.isBlank(qr)) {
                log.error("[网联付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            result.put(QRCONTEXT, qr);
            
//            JSONObject jsonObject = JSONObject.parseObject(resultStr);
//            //只取正确的值，其他情况抛出异常
//            if (null != jsonObject && jsonObject.containsKey("returncode") && "00".equalsIgnoreCase(jsonObject.getString("returncode"))  && jsonObject.containsKey("txcode") && StringUtils.isNotBlank(jsonObject.getString("txcode"))) {
//                result.put(QRCONTEXT, jsonObject.getString("txcode"));
//                result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
//            }else {
//                log.error("[网联付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[网联付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[网联付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}