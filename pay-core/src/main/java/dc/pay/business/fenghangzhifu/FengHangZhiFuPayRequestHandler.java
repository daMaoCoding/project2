package dc.pay.business.fenghangzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.*;
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

/**
 * @author Cobby
 * Apr 26, 2019
 */
@RequestPayHandler("FENGHANGZHIFU")
public final class FengHangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FengHangZhiFuPayRequestHandler.class);

    private static final String pay_version            ="pay_version";  // 系统接口版本 是 否 固定值:vb1.0
    private static final String pay_memberid           ="pay_memberid"; // 商户号 是 是 平台分配商户号
    private static final String pay_orderid            ="pay_orderid";  // 订单号 是 是 上送订单号唯一, 字符长度 20
    private static final String pay_applydate          ="pay_applydate";// 提交时间 是 否    时间格式(yyyyMMddHHmmss)： 20161226181818
    private static final String pay_bankcode           ="pay_bankcode"; // 银行编码 是 是 参考后续说明
    private static final String pay_notifyurl          ="pay_notifyurl";// 服务端通知 是 是 服务端返回地址.GET 返回数据）
    private static final String pay_amount             ="pay_amount";   // 订单金额 是 是 商品金额（单位元）


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_version,"vb1.0");
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_applydate,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            }
        };
        log.debug("[丰航支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //stringSignTemp="pay_memberid=pay_memberid&pay_bankcode=pay_bankcode&pay_amount=pay_amoun
        //t&pay_orderid=pay_orderid&pay_notifyurl=pay_notifyurl"+key
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(pay_memberid+"=").append(api_response_params.get(pay_memberid)).append("&");
        signSrc.append(pay_bankcode+"=").append(api_response_params.get(pay_bankcode)).append("&");
        signSrc.append(pay_amount+"=").append(api_response_params.get(pay_amount)).append("&");
        signSrc.append(pay_orderid+"=").append(api_response_params.get(pay_orderid)).append("&");
        signSrc.append(pay_notifyurl+"=").append(api_response_params.get(pay_notifyurl));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[丰航支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));

        } catch (Exception e) {
            log.error("[丰航支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[丰航支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[丰航支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}