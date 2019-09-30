package dc.pay.business.anbeifu;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Oct 19, 2018
 */
@RequestPayHandler("ANBEIFU")
public final class AnBeiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AnBeiFuPayRequestHandler.class);

    //输入项         输入项名称        属性        注释      数据类型
    //mid             商户号             M          商户号      as..32
    //orderNo         商户订单号         M          建议： 日期(YYYYMMDDHHMMSS)+商户首字母（4字节）+商户交易流水号（12字节））      as..4
    //subject         订单标题           M          可放置商品名称      N14
    //body            订单描述           M          商品的简要描述      
    //amount          订单金额           M          ##.## (圆.角分)      
    //type            支付种类           M          union_sm:银联扫码;  QQwap:QQ钱包Wap;   QQwallet:QQ钱包扫码   alipay_srb:支付宝h5      
    //notifyUrl       通知URL                           
    //buyerName       买家姓名           M                
    //buyerId         唯一编号           M          买家在商城的唯一编号      
    //payRemark       付款摘要           M          付款摘要      
    //extNetIp        外网IP             M          用户设备外网      
    //sign            签名               M           数字签名      
    private static final String mid                                ="mid";
    private static final String orderNo                            ="orderNo";
    private static final String subject                            ="subject";
    private static final String body                               ="body";
    private static final String amount                             ="amount";
    private static final String type                               ="type";
    private static final String notifyUrl                          ="notifyUrl";
    private static final String buyerName                          ="buyerName";
    private static final String buyerId                            ="buyerId";
    private static final String payRemark                          ="payRemark";
    private static final String extNetIp                           ="extNetIp";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mid, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(subject,"name");
                put(body,"name");
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(buyerName,handlerUtil.getRandomStr(5));
                put(buyerId,handlerUtil.getRandomStr(6));
                put(payRemark,"name");
                put(extNetIp,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[安呗付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = null;
        try {
            signMd5 = dc.pay.business.anbeifu.DesUtil.encrypt(channelWrapper.getAPI_MEMBERID(),paramsStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[安呗付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[安呗付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
    	Map<String,String> headersMap = new HashMap<>();
//      // // 设置文件字符集:
        headersMap.put("Charset", "UTF-8");
        headersMap.put("Content-Type", "application/soap+xml;charset=UTF-8");
        headersMap.put("SOAPAction", "");
    	String metName="pay";//对应方法名
    	String soapXml="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://server.webservices.packcommon.xmss.com/\">\n" +
    	        "   <soapenv:Header/>" +
    	        "   <soapenv:Body>" +
    	        "      <ser:"+metName+">" +
    	        "         <arg0>"+JSON.toJSONString(payParam)+"</arg0>" +
    	        "      </ser:"+metName+">" +
    	        "   </soapenv:Body>" +
    	        "</soapenv:Envelope>";
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), soapXml, headersMap);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[安呗付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[安呗付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        String returnCont = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            StringReader sr = new StringReader(resultStr);
            InputSource is = new InputSource(sr);
            Document document = db.parse(is);
            Element root = document.getDocumentElement();
            NodeList nodelist_return = root.getElementsByTagName("return");
            if (null == nodelist_return) {
                log.error("[安呗付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            returnCont = nodelist_return.item(0).getTextContent();
        } catch (Exception e1) {
            e1.printStackTrace();
            log.error("[安呗付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (StringUtils.isBlank(returnCont)) {
            log.error("[安呗付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[安呗付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        if (!returnCont.contains("{") || !returnCont.contains("}")) {
           log.error("[安呗付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(returnCont);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[安呗付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("resultCode") && "SUCCESS".equalsIgnoreCase(resJson.getString("resultCode"))  && resJson.containsKey("qrCode") && StringUtils.isNotBlank(resJson.getString("qrCode"))) {
            String code_url = resJson.getString("qrCode");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else {
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[安呗付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[安呗付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[安呗付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}