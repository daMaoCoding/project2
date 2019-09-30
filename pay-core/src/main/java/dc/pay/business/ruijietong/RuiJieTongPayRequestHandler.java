package dc.pay.business.ruijietong;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("RUIJIETONG")
public final class RuiJieTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RuiJieTongPayRequestHandler.class);
    private static final String      version  = "version";   // 版本号，固定值：V3.1.0.0
    private static final String      merNo  = "merNo";   // 商户号
    private static final String      netway  = "netway";   // 支付网关代码，参考附录3.1
    private static final String      random  = "random";   // 随机数
    private static final String      orderNum  = "orderNum";   // 订单号，必须按当前时间的yyyyMMdd开头，并 以北京时间为准（例如：2017080800001）
    private static final String      amount  = "amount";   // 金额（单位：分）
    private static final String      goodsName  = "goodsName";   // 商品名称
    private static final String      callBackUrl  = "callBackUrl";   // 支付结果通知地址
    private static final String      callBackViewUrl  = "callBackViewUrl";   // 回显地址
    private static final String      charset  = "charset";   // 客户端系统编码格式，UTF-8，GBK
    private static final String      subMerNo  = "subMerNo";   // 客户端系统编码格式，UTF-8，GBK
    private static final String      sign  = "sign";   // 签名（字母大写）



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误：商户号&MD5秘钥");
        }
        Map<String, String> payParam = Maps.newTreeMap();
            payParam.put(version,"V3.1.0.0");
            payParam.put(merNo,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(subMerNo,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(netway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(random,HandlerUtil.getRandomStr(5));
            payParam.put(orderNum,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(callBackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(callBackViewUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(charset,"UTF-8");
        log.debug("[睿捷通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String metaSignJsonStr = HandlerUtil.mapToJson(params);
        String sign = RuiJieTongUtil.MD5(metaSignJsonStr + channelWrapper.getAPI_MEMBERID().split("&")[1], RuiJieTongUtil.CHARSET);// 32位
        log.debug("[睿捷通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(sign));
        return sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {


            byte[] dataStr = RuiJieTongUtil.encryptByPublicKey(HandlerUtil.mapToJson(payParam).getBytes(RuiJieTongUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
           // String param = com.sun.org.apache.xml.internal.security.utils.Base64.encode(dataStr);
            String param =java.util.Base64.getEncoder().encodeToString(dataStr);
            String reqParam = "data=" + java.net.URLEncoder.encode(param, RuiJieTongUtil.CHARSET) + "&merchNo=" + channelWrapper.getAPI_MEMBERID().split("&")[0] + "&version=" + payParam.get("version");
            String contents = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqParam,MediaType.APPLICATION_FORM_URLENCODED_VALUE).trim();
            contents = new String(contents.getBytes("ISO-8859-1"), "UTF-8");


            // 检查状态
            net.sf.json.JSONObject resultJsonObj = net.sf.json.JSONObject.fromObject(contents);
            String stateCode = resultJsonObj.getString("stateCode");
            if (stateCode.equals("00")) {
            String resultSign = resultJsonObj.getString("sign");
            resultJsonObj.remove("sign");
            String targetString = RuiJieTongUtil.MD5(resultJsonObj.toString() + channelWrapper.getAPI_MEMBERID().split("&")[1], RuiJieTongUtil.CHARSET);
                if (targetString.equals(resultSign)) {
                    if(HandlerUtil.isWapOrApp(channelWrapper)){
                        result.put(JUMPURL, HandlerUtil.UrlDecode(resultJsonObj.getString("qrcodeUrl")));

                    }else{
                        result.put(QRCONTEXT, HandlerUtil.UrlDecode(resultJsonObj.getString("qrcodeUrl")));
                    }
                    payResultList.add(result);
                }
            }else {
                throw new PayException(contents);
            }
        } catch (Exception e) {
             log.error("[睿捷通]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[睿捷通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[睿捷通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}