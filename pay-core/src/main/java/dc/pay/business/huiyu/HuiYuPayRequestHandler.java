package dc.pay.business.huiyu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 17, 2018
 */
@RequestPayHandler("HUIYU")
public final class HuiYuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiYuPayRequestHandler.class);

    //商户编号  mechno  是   定Int(12)    商户编号是高户在支付平台上开设的商户号码为12位数字，如：126804357570
    private static final String mechno                ="mechno";
    //支付方式  payway  是   变String(32) 微信：WEIXIN    支付宝：ALIPAY    QQ支付：QQPAY    京东支付：JDPAY     银联网关: UNIONPAY
    private static final String payway                 ="payway";
    //支付类型  paytype 是   变String(32) 微信- 公众号支付: OPENPAY， H5支付：H5PAY， 扫码支付：SCANPAY     支付宝- Wap支付：DIRECT_PAY， 扫码支付：SCAN_PAY      QQ支付- Wap支付：QQ_WAP       京东支付- 扫码支付：JD_SCAN Wap支付：JD_WAP        银联支付: 快捷支付：UN_QUICK 网关支付：UN_GATEWAY
    private static final String paytype                ="paytype";
    //交易金额  amount  是   变Int(10)    订单的资金总额，单位为 RMB-分。
    private static final String amount                ="amount";
    //订单时间  timestamp   是   定Int(13)    格式：Unix时间戳，精确到毫秒，时间误差超过1小时会抛弃此订单
    private static final String timestamp              ="timestamp";
    //订单编号  orderno 是   定String(32) 确保唯一,长度不超过32，尽量随机生成
    private static final String orderno                 ="orderno";
    //商品名称  body    是   定String(32) 商品的名称，原值参与签名后用UrlEncoder进行编码
    private static final String body              ="body";
    //下单IP地址    orderip 是   定String(32) 发起订单的真实IP地址
    private static final String orderip              ="orderip";
    //前端通知地址    returl  是   变String(255)    支付成功之后调起的前端界面URL，确保外网可以访问,并且视情况进行url encode (咨询运营),不允许带!@#+等，例如qq+v&c?!22
    private static final String returl                ="returl";
    //异步通知地址    notifyurl   是   变String(255)    支付成功之后将异步返回给到商户服务端！确保外网可以访问
    private static final String notifyurl                 ="notifyurl";
    //银行编码  bankcode    否   定String(32) 此参数暂时用于银行编码，银联支付必传： 中国工商【ICBC】   中国农业【ABC】    中国建设【CCB】     北京银行【BCCB】    中国邮政【PSBC】      中国光大【CEB】      上海银行【SHB】     民生银行【CMBC】      招商银行【CMBCHINA】     如上面没有的银行编码可以自行百度
//    private static final String bankcode                 ="bankcode";
    //来源    source  否   定String(32) 客户端来源 手机端：MOBILE      电脑端：PC    不填默认为手机端
//    private static final String source                 ="source";
    //签名参数  sign    是   定String(32) 详见"签名说明"支付请求报文范例
//    private static final String sign                 ="sign";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mechno, channelWrapper.getAPI_MEMBERID());
                put(payway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(timestamp,  System.currentTimeMillis()+"");
                put(orderno,channelWrapper.getAPI_ORDER_ID());
                put(body,"name");
                put(orderip,channelWrapper.getAPI_Client_IP());
                put(returl,channelWrapper.getAPI_WEB_URL());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[汇裕]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        log.debug("[汇裕]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
//        if (true) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[汇裕]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[汇裕]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//               log.error("[汇裕]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//               throw new PayException(resultStr);
//            }
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
//                resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
//                resultStr = UnicodeUtil.unicodeToString(resultStr);
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[汇裕]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && 
            (jsonObject.getJSONObject("data").containsKey("status") && "0".equalsIgnoreCase(jsonObject.getJSONObject("data").getString("status"))
                    && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("payUrl")))
            ){
//            if (null != jsonObject && jsonObject.containsKey("status") && "0".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getString("payUrl"))) {
                String code_url = jsonObject.getJSONObject("data").getString("payUrl");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                result.put( JUMPURL, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[汇裕]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[汇裕]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[汇裕]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}