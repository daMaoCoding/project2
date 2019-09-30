package dc.pay.business.yijufuzhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 1, 2019
 */
@RequestPayHandler("YIJUFUZHIFU")
public final class YiJuFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiJuFuZhiFuPayRequestHandler.class);

    private static final String mch_id                ="mch_id";          //    String(32)    是    1234567890    平台分配的商户号
    private static final String trade_type            ="trade_type";      //    string(10)    是    ALIH5    交易类型
    private static final String nonce                 ="nonce";           //    string(32)    是    7VS264I5K8502SI8ZNMTM6LTKCH16CQ2    随机字符串，不长于 32 位
//  private static final String user_id               ="user_id";         //    string(32)    否    用户ID
    private static final String timestamp             ="timestamp";       //    string        是    1524822584    时间戳
    private static final String subject               ="subject";         //    String(200)   是    商品1    订单名称
//  private static final String detail                ="detail";          //    String(500)   否    商品1详情    商品详情
    private static final String out_trade_no          ="out_trade_no";    //    string(32)    是    20150806125346    商户系统内部的订单号，32 个字符内
    private static final String total_fee             ="total_fee";       //    Int           是    100    总金额，单位为分
    private static final String spbill_create_ip      ="spbill_create_ip";//    string        是    123.12.12.123    终端IP
    private static final String timeout               ="timeout";         //    string        否    1    过期时长
    private static final String notify_url            ="notify_url";      //    string        是    异步地址
//  private static final String return_url            ="return_url";      //    string        否    返回地址
//  private static final String issuer_id             ="issuer_id";       //    string        否    银行机构代码。如果trade_type是GATEWAY，则必填
    private static final String sign_type             ="sign_type";       //    string        是    MD5    签名类型，目前支持RSA和 MD5，建议使用MD5

    private static final String key                   ="key=";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(nonce,HandlerUtil.randomStr(8));
                put(timestamp,  System.currentTimeMillis()/1000 + "");
                put(subject,"product");
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,  channelWrapper.getAPI_AMOUNT());
                put(spbill_create_ip,channelWrapper.getAPI_Client_IP());
                put(timeout,"10");
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(sign_type,"MD5");
            }
        };
        log.debug("[易聚付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if ( StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
//        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(key + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[易聚付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[易聚付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))
                        && jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url"))) {
                    String code_url = jsonObject.getString("pay_url");
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url); jump=1
                    if (StringUtils.isNotBlank(jsonObject.getString("jump")) && "1".equalsIgnoreCase(jsonObject.getString("jump"))) {
                        result.put(JUMPURL, code_url);
                    }else{
                        result.put(QRCONTEXT, code_url);
                    }
                }else {
                    log.error("[易聚付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[易聚付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[易聚付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[易聚付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}