package dc.pay.business.youchuangzhifu;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.*;


/**
 *
 * @author Cobby
 * May 01, 2019
 */
@RequestPayHandler("YOUCHUANGZHIFU")
public final class YouChuangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YouChuangZhiFuPayRequestHandler.class);


    private static final String mer_id               ="mer_id";         //商户号
    private static final String timestamp            ="timestamp";      //请求时间    时间戳,格式yyyy-MM-dd HH:mm:ss
    private static final String terminal             ="terminal";       //终端类型    请看4.1.2 支付类型表
    private static final String version              ="version";        //版本号      01
    private static final String amount               ="amount";         //金额       代付金额(单位 分,不低于10元)
    private static final String backurl              ="backurl";        //返回的url   支付成功返回的url
    private static final String failUrl              ="failUrl";        //返回的url   支付失败返回的url
    private static final String ServerUrl            ="ServerUrl";      //异步返回的url   返回的数据将post提交到该url  商户处理数据
    private static final String businessnumber       ="businessnumber"; //商品订单号（['A~Z,a~z,0~9']组成的10到64位字符串）
    private static final String goodsName            ="goodsName";      //商品名称（描述）,建议使用英文
    private static final String sign_type            ="sign_type";      //默 认 md5

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mer_id, channelWrapper.getAPI_MEMBERID());
                put(timestamp, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(businessnumber,channelWrapper.getAPI_ORDER_ID());
                put(amount,channelWrapper.getAPI_AMOUNT());
                put(backurl,channelWrapper.getAPI_WEB_URL());
                put(failUrl,channelWrapper.getAPI_WEB_URL());
                put(ServerUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(terminal,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(goodsName,channelWrapper.getAPI_ORDER_ID());
                put(sign_type,"md5");
                put(version,"01");
            }
        };
        log.debug("[优创支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        paramKeys.remove(sign_type);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[优创支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();

        try {
                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[优创支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("result") && "success".equalsIgnoreCase(jsonObject.getString("result")) 
                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    jsonObject = JSONObject.parseObject(jsonObject.getString("data"));
                    String code_url = jsonObject.getString("trade_qrcode");
                    result.put( JUMPURL , code_url);
                }else {
                    log.error("[优创支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[优创支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        payResultList.add(result);
        log.debug("[优创支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[优创支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}