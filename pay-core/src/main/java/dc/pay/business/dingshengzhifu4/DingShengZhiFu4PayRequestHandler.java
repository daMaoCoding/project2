package dc.pay.business.dingshengzhifu4;

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
import org.springframework.http.HttpMethod;

import java.util.*;

/**
 *
 * @author Cobby
 * May 13, 2019
 */
@RequestPayHandler("DINGSHENGZHIFU4")
public final class DingShengZhiFu4PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DingShengZhiFu4PayRequestHandler.class);


    private static final String user_id                  ="user_id";      // 商户ID    是   是   平台分配商户号
    private static final String out_trade_no             ="out_trade_no"; // 外部订单号  是   是  上送订单号唯一
    private static final String product_id               ="product_id";   // 支付产品    是   是  支付产品ID
    private static final String return_url               ="return_url";   // 页面通知    是   是  同步返回地址，HTTP/HTTPS开头字符串
    private static final String notify_url               ="notify_url";   // 服务器通知  是   是   服务器主动通知商户服务器哩指定的页面HTTP/HTTPS路径
    private static final String subject                  ="subject";      // 订单标题    是   是
    private static final String body                     ="body";         // 订单描述    是   是
    private static final String remark                   ="remark";       // 备注       是   是
    private static final String pay_amount               ="pay_amount";   // 订单金额    是   是   单位：元
    private static final String applydate                ="applydate";    // 提交时间    是   是

    private static final String key        ="apikey";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[鼎盛支付4]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[鼎盛支付4]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//                put(user_id, channelWrapper.getAPI_MEMBERID());
                put(user_id, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
//                put(product_id,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(product_id,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(subject,"name");
                put(body,"name");
                put(remark,"name");
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(applydate,  DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            }
        };
        log.debug("[鼎盛支付4]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[鼎盛支付4]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
            try {
                    String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//                    String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                    if (StringUtils.isBlank(resultStr)) {
                        log.error("[鼎盛支付4]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                        throw new PayException(resultStr);
                    }
                    JSONObject jsonObject;
                    try {
                        jsonObject = JSONObject.parseObject(resultStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("[鼎盛支付4]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                        throw new PayException(resultStr);
                    }
                    //只取正确的值，其他情况抛出异常
                    if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))
                            && (jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data")) )){
                        String code_url = jsonObject.getJSONObject("data").getJSONObject("pay_extends").getString("pay_url");
//                        result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                        if (handlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                            result.put(JUMPURL, code_url);
                        }else{
                            result.put(QRCONTEXT, code_url);
                        }
                    }else {
                        log.error("[鼎盛支付4]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                        throw new PayException(resultStr);
                    }
            } catch (Exception e) {
                log.error("[鼎盛支付4]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
                throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
            }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[鼎盛支付4]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[鼎盛支付4]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}