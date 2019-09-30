package dc.pay.business.kuaifutong;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Oct 24, 2018
 */
@RequestPayHandler("KUAIFUTONG")
public final class KuaiFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiFuTongPayRequestHandler.class);

    //4.2扫码支付 1007
    //数据元名称         数据元标识         数据元格式         请求         响应         数据元取值说明
    //交易类型           transType            N4                M                           1007
    //机构号             instCode             任意(1,10)        M                           平台机构号
    //证件类型           certType             数字(1,2)         M                           目前只支持0-平台商户号 1-身份证 2-营业执照,9-其他   固定送0
    //证件号码           certId               任意(8,20)        M                           平台商户号
    //金额               transAmt             数字(1,20)        M                           整数，以分为单位
    //支付类型           payType              数字(2)           M                           03-支付宝 04-微信 05-QQ钱包扫码 06-京东扫码 07-京东钱包 08-银联钱包
    //购物明细           goodsDesc            任意(1,1024)      M                           
    //交易日期           transDate            日期(8,8)         M                           yyyyMMdd
    //订单号             orderNo              任意(1,20)        M                           商户号+订单号+商户日期唯一标示一笔交易
    //后台通知地址URL    backUrl              任意(1,256)       M                           平台通知支付结果地址,支持交易中上送或预配置
    //签名               sign                 任意32)           M                           MD5签名
    //交易流水           tranSerno            任意(1,30)                     C              交易流水号
    //通讯应答码         ret_code             任意(1,20)                     M              0000-成功，非0000-失败，失败原因看通讯描述。
    //通讯描述           ret_msg              任意(1,1024)                   M              成功时返回二维码扫码地址，失败时为失败原因
    private static final String transType            ="transType";
    private static final String instCode             ="instCode";
    private static final String certType             ="certType";
    private static final String certId               ="certId";
    private static final String transAmt             ="transAmt";
    private static final String payType              ="payType";
    private static final String goodsDesc            ="goodsDesc";
    private static final String transDate            ="transDate";
    private static final String orderNo              ="orderNo";
    private static final String backUrl              ="backUrl";
//    private static final String tranSerno            ="tranSerno";
//    private static final String ret_code             ="ret_code";
//    private static final String ret_msg              ="ret_msg";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[快付通]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
            throw new PayException("[快付通]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(transType,"1007");
                put(instCode, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(certType,"0");
                put(certId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(transAmt,  channelWrapper.getAPI_AMOUNT());
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(goodsDesc,"name");
                put(transDate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(backUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[快付通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[快付通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[快付通]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[快付通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[快付通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("ret_code") && "0000".equalsIgnoreCase(resJson.getString("ret_code"))  && resJson.containsKey("ret_msg") && StringUtils.isNotBlank(resJson.getString("ret_msg"))) {
            String code_url = resJson.getString("ret_msg");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else {
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[快付通]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[快付通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[快付通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}