package dc.pay.business.eightvipzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.tongsao.TongSaoRequestHandler;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * Feb 25, 2019
 */
@RequestPayHandler("EIGHTEIGHTVIPZHIFU")
public final class BaBaVipZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaBaVipZhiFuPayRequestHandler.class);

// version	request	C	String(8)	版本号	默认：V1
// mer_no	request	M	String(15)	商户号	平台分配的唯一商户号
// mer_order_no	request	M	String(30)	商户订单号	商户必须保证唯一
// ccy_no	request	C	String(10)	币种	CNY:人民币 USD:美元 默认CNY
// order_amount	request	M	Number	交易金额	分为单位，整数
// busi_code	request	M	String(10)	业务编码	100101 微信扫码支付 100201 支付宝扫码支付
// goods	request	M	String(100)	商品名称
// reserver	request	C	String(32)	保留信息	原样返回
// bg_url	request	M	String	异步通知地址	支付成功后，平台主动通知商家系统，商家系统必须指定接收通知的地址。
// sign	request	M	String(64)	签名	使用MD5计算摘要作为签名数据； 所有参数按照字段名的 ascii 码从小到大排序后使用 QueryString 的格式（即key1=value1&key2=value2…） 拼接而成，再加上秘钥 &key=key作为签名源串，空值不参与签名；
    private static final String version                ="version";
    private static final String mer_no                 ="mer_no";
    private static final String mer_order_no           ="mer_order_no";
    private static final String ccy_no                 ="ccy_no";
    private static final String order_amount           ="order_amount";
    private static final String busi_code              ="busi_code";
    private static final String goods                  ="goods";
    private static final String reserver               ="reserver";
    private static final String bg_url                 ="bg_url";
    private static final String key        ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "V1");
                put(mer_no, channelWrapper.getAPI_MEMBERID());
                put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
                put(ccy_no,"CNY");
                put(order_amount,  channelWrapper.getAPI_AMOUNT());
                put(busi_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(goods,"name");
                put(bg_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(reserver,"reserver");
            }
        };
        log.debug("[88VIP支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key +"=" +channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[88VIP支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
                //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[88VIP支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("status") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("order_data") && StringUtils.isNotBlank(jsonObject.getString("order_data"))) {
                    String code_url = jsonObject.getString("order_data");
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                }else {
                    log.error("[88VIP支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }

        } catch (Exception e) {
            log.error("[88VIP支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[88VIP支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[88VIP支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}