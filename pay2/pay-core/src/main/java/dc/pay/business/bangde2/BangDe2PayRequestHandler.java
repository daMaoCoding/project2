package dc.pay.business.bangde2;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 17, 2019
 */
@RequestPayHandler("BANGDE2")
public final class BangDe2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BangDe2PayRequestHandler.class);

    //参数名称                  参数含义            必填          签名          参数说明
    //pay_memberid          商户ID            是           是           商户id,由邦德2支付分配
    //pay_orderid           订单号         是           是           格式：订单号+商户ID，（订单号当日不可重复，订单号不支持中文字符）
    //pay_amount            金额              是           是           单位：元，精确到分   如：0.01
    //pay_applydate         订单提交时间      是           是           格式：YYYY-MM-DD HH:MM:SS: 如： 2017-12-26 18:18:18
    //pay_bankcode          银行编号            是           是           支付宝：ALIPAY  支付宝H5：:ALIPAY_H5
    //pay_notifyurl         回调地址            是           是           服务端回调返回地址.（POST返回数据）
    //pay_md5sign           MD5签名字段     是           否           请看验证签名参数顺序，参数值大写
    //pay_reserved1         保留字段            否           否           原样返回参数
    //ip                    付款者IP           否           否           交易发起者的IP地址，未填写可能导致交易失败。
    private static final String pay_memberid                ="pay_memberid";
    private static final String pay_orderid                 ="pay_orderid";
    private static final String pay_amount                  ="pay_amount";
    private static final String pay_applydate               ="pay_applydate";
    private static final String pay_bankcode                ="pay_bankcode";
    private static final String pay_notifyurl               ="pay_notifyurl";
    //这里用来回传订单号
    private static final String pay_reserved1               ="pay_reserved1";
    private static final String ip                          ="ip";

    //signature 数据签名    32  是   　
//  private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID()+channelWrapper.getAPI_MEMBERID());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_applydate, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ip,channelWrapper.getAPI_Client_IP());
                put(pay_reserved1,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[邦德2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(pay_amount+"=").append(api_response_params.get(pay_amount)).append("&");
        signSrc.append(pay_applydate+"=").append(api_response_params.get(pay_applydate)).append("&");
        signSrc.append(pay_bankcode+"=").append(api_response_params.get(pay_bankcode)).append("&");
        signSrc.append(pay_memberid+"=").append(api_response_params.get(pay_memberid)).append("&");
        signSrc.append(pay_notifyurl+"=").append(api_response_params.get(pay_notifyurl)).append("&");
        signSrc.append(pay_orderid+"=").append(api_response_params.get(pay_orderid)).append("&");
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[邦德2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isYLKJ(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }
        
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[邦德2]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
//                throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
//            }
//            try {
//                if (!resultStr.contains("name=\"QRcodeURL\"")) {
//                    log.error("[邦德2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(new String(resultStr.getBytes("ISO-8859-1"), "UTF-8")) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(new String(resultStr.getBytes("ISO-8859-1"), "UTF-8"));
//                }else {
//                    Elements selects = Jsoup.parse(resultStr).select("[name=QRcodeURL]");
//                    if (null == selects) {
//                        log.error("[邦德2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                        throw new PayException(resultStr);
//                    }
//                    String val = selects.first().val();
//                    if (StringUtils.isBlank(val)) {
//                        log.error("[邦德2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                        throw new PayException(resultStr);
//                    }
//                    String decodeByUrl = QRCodeUtil.decodeByUrl(val);
//                    result.put(QRCONTEXT, decodeByUrl);
//                }
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//                throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[邦德2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[邦德2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}