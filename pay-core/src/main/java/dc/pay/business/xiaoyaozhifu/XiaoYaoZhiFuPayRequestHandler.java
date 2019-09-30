package dc.pay.business.xiaoyaozhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 * 
 * @author andrew
 * May 29, 2019
 */
@RequestPayHandler("XIAOYAOZHIFU")
public final class XiaoYaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XiaoYaoZhiFuPayRequestHandler.class);

    //请求参数
    //参数  必须  说明
    //p1_merchantno       商户号: 请访问商户后台来获取您的商户号。
    private static final String p1_merchantno                ="p1_merchantno";
    //p2_amount       支付金额: 以元为单位，精确到小数点后 2 位。如 15.00 及 15 都是合法的参数值。
    private static final String p2_amount                ="p2_amount";
    //p3_orderno      订单号: 唯一标识您的支付平台的一笔订单，必须保证此订单号的唯一性。
    private static final String p3_orderno                ="p3_orderno";
    //p4_paytype      支付产品类型编码: 如 AlipayScan 表示支付宝扫码, WechatH5 表示微信 H5 等。请参阅支付产品类型编码表。
    private static final String p4_paytype                ="p4_paytype";
    //p5_reqtime      支付发起时间: 14 字节长的数字串，格式布局如 yyyyMMddHHmmss. (如 20060102150405 表示 2006年1月2日下午3点04分05秒)
    private static final String p5_reqtime                ="p5_reqtime";
    //p6_goodsname        商品名称: 请勿使用空白字符串值。
    private static final String p6_goodsname                ="p6_goodsname";
    //p7_bankcode     银行编码: 付款银行的编码，仅在网关支付产品中有意义, 其他支付产品请传递空白字符串或忽略该参数。 具体支持银行范围和相关银行编码请参考银行编码表。
//    private static final String p7_bankcode                ="p7_bankcode";
    //p8_returnurl        同步跳转 URL: 支付过程完成后，用户设备上的浏览器将被跳转到此 URL.
    private static final String p8_returnurl                ="p8_returnurl";
    //p9_callbackurl      后台异步通知 (回调) 地址: 支付成功后，我方服务器将向该地址发起 GET 异步通知请求。    请注意，此 URL 不能携带查询参数。
    private static final String p9_callbackurl                ="p9_callbackurl";
    //sign        MD5 签名: HEX 大写, 32 字节。
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
//            log.error("[逍遥支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
//            throw new PayException("[逍遥支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(p1_merchantno, channelWrapper.getAPI_MEMBERID());
                put(p2_amount,  handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(p3_orderno,channelWrapper.getAPI_ORDER_ID());
                put(p4_paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(p5_reqtime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(p6_goodsname,"name");
                put(p8_returnurl,channelWrapper.getAPI_WEB_URL());
                put(p9_callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[逍遥支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = handlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[逍遥支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (handlerUtil.isWY(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) {
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[逍遥支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[逍遥支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[逍遥支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[逍遥支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("rspcode") && "A0".equalsIgnoreCase(jsonObject.getString("rspcode"))
                    && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))
//                    && jsonObject.getJSONObject("data").containsKey("url") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("url"))
            
            ){
//            if (null != jsonObject && jsonObject.containsKey("code") && "0000".equalsIgnoreCase(jsonObject.getString("code"))
//                    && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))
//                    && jsonObject.containsKey("qrCode") && StringUtils.isNotBlank(jsonObject.getString("qrCode"))) {
                String code_url = jsonObject.getString("data");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[逍遥支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[逍遥支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[逍遥支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}