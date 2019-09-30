package dc.pay.business.xinhaitunzhifu;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.tongyinfu.MD5Util;
import dc.pay.business.tongyinfu.SignUtil;
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;

/**
 * @author cobby
 * Jan 17, 2019
 */
@RequestPayHandler("YONGSHUNZHIFU")
public final class YongShunZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YongShunZhiFuPayRequestHandler.class);

    //参数名              描述                  属性              请求        说明
    //requestId           请求订单号            Str-max32          M          保证唯一
    //orgId               机构号                Str-max16          M          平台分配机构号
    //timestamp           请求时间戳            Str-max14          M          格式:yyyyMMddHHmmss
    //productId           产品ID                Str-max10          M          产品ID 对应关系详见产品ID对应表
    //businessData        业务交互数据          JSON               M          对应产品交互业务数据
    //dataSignType        业务数据加密方式      Int-max1           C          上送的业务参数是否加密，为空默认为明文传输  0-不加密，明文传输  1-DES加密，密文传输   若为密文传输，需要将密文进行URLEncode处理
    //signData            数据签名              Str-max32          M          签名规则   详见签名算法
    //4.4.1.获取二维码
    //请求方式：POST
    //参数：BUSINESS_DATA
    //参数名              描述                  属性               请求       响应           说明
    //merno               商户号                Str-max32           M           M           平台进件返回商户号
    //bus_no              业务编号              Str-max10           M           M           详见bus_no列表
    //amount              交易金额              Str-max32           M           M           交易金额，单位分
    //goods_info          商品名称              Str-max32           M           M           商品名称
    //order_id            订单号                Str-max32           M           M           商户自定义订单号
    //return_url          前端通知地址          Str-max255          M           C           前端跳转回调地址
    //notify_url          后台通知地址          Str-max255          M           C           后台通知回调地址
    //url                 二维码URL地址         Str-max255                      M           二维码的url地址
    private static final String requestId                 ="requestId";
    private static final String orgId                     ="orgId";
    private static final String timestamp                 ="timestamp";
    private static final String productId                 ="productId";
    private static final String businessData              ="businessData";
    private static final String dataSignType              ="dataSignType";
    //    private static final String signData                  ="signData ";
    private static final String merno                     ="merno";
    private static final String bus_no                    ="bus_no";
    private static final String amount                    ="amount";
    private static final String goods_info                ="goods_info";
    private static final String order_id                  ="order_id";
    private static final String return_url                ="return_url";
    private static final String notify_url                ="notify_url";
//    private static final String url                       ="url";

    //网银
    //参数名          描述                属性           请求        响应        说明
    //notify_url      后台通知地址        Str-max255       M          C           后端跳转回调地址
    private static final String cardname                  ="cardname"; // 银行名称，参考银行卡支持列表
    private static final String bank_code                 ="bank_code";// 银行编码
    private static final String card_type                 ="card_type";// 卡类型 1-储蓄卡 2-信用卡
    private static final String channelid                 ="channelid";// 账户类型 1-对私 2-对公
    private static final String ishtml                    ="ishtml";   // 是否返回html 1-html内容  2-url地址

    //signature    数据签名    32    是    　
//    private static final String signature  ="signData";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[永顺支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
            throw new PayException("[永顺支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
        }
        Map<String, String> busMap = new TreeMap<String, String>() {
            {
                put(amount, channelWrapper.getAPI_AMOUNT());
                put(goods_info,"name");
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(merno, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                if (handlerUtil.isWY(channelWrapper)) {
                    put(bus_no,"0499");
                    put(card_type,"1");
                    put(channelid,"1");
                    put(ishtml,"2");
                    put(cardname,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                    put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                }else{
                    put(bus_no,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(requestId,System.currentTimeMillis()+""+System.currentTimeMillis());
                put(orgId, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(timestamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(productId,handlerUtil.isWY(channelWrapper) ? "0500" : "0100");
                put(businessData,JSON.toJSONString(busMap));
//                put(dataSignType,"0");
                put(dataSignType,"");
            }
        };
        log.debug("[永顺支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String paramsStr = SignUtil.getSignData(api_response_params)+channelWrapper.getAPI_KEY();
        String signMd5 = null;
        try {
            signMd5 = MD5Util.getMD5(paramsStr.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        log.debug("[永顺支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        Map<String, String> data = new TreeMap<String, String>() {
            {
                put(requestId,payParam.get(requestId));
                put(orgId, payParam.get(orgId));
                put(timestamp, payParam.get(timestamp));
                put(productId,payParam.get(productId));
//                put(businessData,"name");
                put(dataSignType,payParam.get(dataSignType));
                put(businessData,payParam.get(businessData));
                put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
            }
        };
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), data,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[永顺支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[永顺支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[永顺支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("respCode") && "00".equalsIgnoreCase(resJson.getString("respCode"))
                && StringUtils.isNotBlank(resJson.getString("result"))) {
            JSONObject resJson2;
            try {
                resJson2 = JSONObject.parseObject(resJson.getString("result"));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[永顺支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (null != resJson2 && resJson2.containsKey("url") && StringUtils.isNotBlank(resJson2.getString("url"))) {
                String code_url = resJson2.getString("url");
                if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWY(channelWrapper)) {
                    result.put(JUMPURL, code_url);
                }else {
                    result.put(QRCONTEXT, code_url);
                }
            }else {
                log.error("[永顺支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }else {
            log.error("[永顺支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[永顺支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[永顺支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}