package dc.pay.business.weifutong;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RequestPayHandler("WEIFUTONG")
public final class WeiFuTongPayRequestHandler extends PayRequestHandler {
     private static final Logger log = LoggerFactory.getLogger(WeiFuTongPayRequestHandler.class);


    private static final String   service  ="service";               //:pay.weixin.native
    private static final String   version  ="version";               //:1.0
    private static final String   charset  ="charset";               //:UTF-8
    private static final String   sign_type  ="sign_type";            //:MD5
    private static final String   out_trade_no  ="out_trade_no";      //:1506932712105
    private static final String   body  ="body";                      //:测试购买商品
    private static final String   attach  ="attach";                   //:附加信息
    private static final String   total_fee  ="total_fee";             //:1
    private static final String   mch_create_ip  ="mch_create_ip";     //:127.0.0.1


    private static final String   mch_id  ="mch_id";
    private static final String   notify_url  ="notify_url";
    private static final String   nonce_str  ="nonce_str";


    private static final String QRCONTEXT = "QRCONTEXT";
    private static final String PARSEHTML = "PARSEHTML";
    private static final String HTMLCONTEXT = "HTMLCONTEXT";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(service, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(version, "1.0");
                put(charset, "UTF-8");
                put(sign_type, "MD5");
                put(out_trade_no, channelWrapper.getAPI_ORDER_ID());
                put(body, "PAY");
                put(attach, "PAY");
                put(total_fee, channelWrapper.getAPI_AMOUNT());
                put(mch_create_ip,HandlerUtil.getRandomIp(channelWrapper) );
                put(mch_id,channelWrapper.getAPI_MEMBERID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(nonce_str, String.valueOf(new Date().getTime()));

            }
        };
        log.debug("[威富通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map payParam) throws PayException {

        Map<String,String> params = SignUtils.paraFilter(payParam);
        StringBuilder buf = new StringBuilder((params.size() +1) * 10);
        SignUtils.buildPayParams(buf,params,false);
        String preStr = buf.toString();
        String sign = MD5.sign(preStr, "&key=" + channelWrapper.getAPI_KEY(), "utf-8");
        log.debug("[威富通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(sign));
        return sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            //System.out.println("reqParams:" + XmlUtils.parseXML(XmlUtils.getParameterMap(payParam)));
            String firstPayresult = HttpUtil.sendPostForString(channelWrapper.getAPI_CHANNEL_BANK_URL(), XmlUtils.parseXML(XmlUtils.getParameterMap(payParam)));
            if (firstPayresult.length() < 10) {
                log.error("[威富通]3.发送支付请求，及获取支付请求结果：" + firstPayresult + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.toJSONString(firstPayresult));
            }

            Map<String, String> firstPayresultMap = XmlUtils.toMap(firstPayresult.getBytes("UTF-8"), "UTF-8");
            String code_url = firstPayresultMap.get("code_url");
            String status = firstPayresultMap.get("status");

                if (StringUtils.isNotBlank(code_url) && status.equalsIgnoreCase("0")) {
                        HashMap<String, String> result = Maps.newHashMap();
                        result.put(QRCONTEXT, code_url);
                        result.put(PARSEHTML, body);
                        payResultList.add(result);
                }else{
                    log.error("[威富通]3.发送支付请求，及获取支付请求结果出错：" + firstPayresult + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(firstPayresult);
                }

        } catch (Exception e) {
            log.error("[威富通]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[威富通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }

            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[威富通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}