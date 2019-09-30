package dc.pay.business.xinfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 13, 2018
 */
@RequestPayHandler("XINFU")
public final class XinFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinFuPayRequestHandler.class);

    //名称 字段名 必须输入 长度 备注
    //1 交易名称 tranName Y 固定填 payment
    private static final String tranName                ="tranName";
    //2 版本号 version Y 系统支持的最低版本号 1.00
    private static final String version                 ="version";
    //3 商户号 merCode Y
    private static final String merCode                ="merCode";
    //4 订单号 orderNo Y 商户端交易订单号（最长 30，不可重复）
    private static final String orderNo                ="orderNo";
    //5 订单时间 orderTime Y yyyyMMddhhmmss
    private static final String orderTime              ="orderTime";
    //6 支付方式 payType Y 2 选择支付方式：11 快捷支付 12 微信支付 14 支付宝支付 15 QQ 钱包支付 17 网银支付, 19 QQ WAP 20 支付宝 WAP 21 微信 WAP 支付
    private static final String payType              ="payType";
    //7 订单金额 amount Y 金额无小数点。默认最后两位为小数位
    private static final String amount             ="amount";
    //8 币种 currency Y 3 目前只支持 CNY
    private static final String currency             ="currency";
    //9 商品名称 productName Y 256 比如：product、网上支付
    private static final String productName             ="productName";
    //10 订单描述 orderDesc N
//    private static final String orderDesc             ="orderDesc";
    //11 商户 URL returnURL N
//    private static final String returnURL             ="returnURL";
    //12 商户后台通知 URL notifyURL Y 商户端接收订单异步回调通知的地址
    private static final String notifyURL             ="notifyURL";
    //13 预留字段用户ID 1 reservedField1 Y/N 支付方式为“支付宝、QQ 钱包、QQWAP、支付宝 WAP”时必须填写：用户 ID（即付款人在商户网站上的注册用户 ID）
//    private static final String reservedField1             ="reservedField1";
    //14 预留字段用户名 2 reservedField2 Y/N 支付方式为“支付宝、QQ 钱包、QQWAP、支付宝 WAP”时必须填写：用户名（即付款人在商户网站上的注册用户名）
//    private static final String reservedField2             ="reservedField2";
    //15 预留字段 3 reservedField3 N
//    private static final String reservedField3             ="reservedField3";
    //16 预留字段 4 reservedField4 N
//    private static final String reservedField4             ="reservedField4";
    //17 签名 sign Y 见 3.2 数据签名和验签
//    private static final String sign             ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(tranName,"payment");
                put(version,"1.00");
                put(merCode, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(orderTime,  DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(currency,"CNY");
                put(productName,"name");
                put(notifyURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//                put(reservedField1, handlerUtil.getRandomStr(5));
//                put(reservedField2, handlerUtil.getRandomStr(8));
            }
        };
        log.debug("[信付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[信付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String map2Xml = XmlUtil.map2Xml(payParam, true, "payment", false);
        StringBuffer sbHtml = new StringBuffer();
        sbHtml.append("<html><head><title>pay</title></head><body>");
        sbHtml.append("<form id=\"form\" name=\"form1\" action=\"" + channelWrapper.getAPI_CHANNEL_BANK_URL() + "\" method=\"post\">");
        sbHtml.append("<input type=\"hidden\" name=\"tranType\" value=\"payment\"/>");
        sbHtml.append("<input type=\"hidden\" name=\"param\" value='" +map2Xml+ "'/>");
        sbHtml.append("<input type=\"submit\" value=\"submit\" style=\"display:none;\"></form>");
        sbHtml.append("<script>document.forms['form'].submit();</script>");
        sbHtml.append("</body></html>");
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, sbHtml.toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.postXml(channelWrapper.getAPI_CHANNEL_BANK_URL(), sbHtml.toString());
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[信付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[信付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
               log.error("[信付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
               throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[信付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
                String code_url = resJson.getString("codeimg");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[信付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[信付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[信付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}