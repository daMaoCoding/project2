package dc.pay.business.maikezhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import dc.pay.utils.*;
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
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;

/**
 * @author Cobby
 * Mar 22, 2019
 */
@RequestPayHandler("MAIKEZHIFU")
public final class MaiKeZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MaiKeZhiFuPayRequestHandler.class);

    private static final String merCode                ="merCode";   //  商户编号  我方平台注册或申请获得
    private static final String tranNo                 ="tranNo";    //  订单号   使用uuid生成订单号以免重复
    private static final String tranAmt                ="tranAmt";   //  订单金额  单位：分
    private static final String settleType             ="settleType";//  清算类型  0：D0实时到账
    private static final String collectWay             ="collectWay";//  结算方式  微信扫码：WXZF微信公众号：WXGZH 支付宝：ZFBZF QQ钱包：QQZF 银联扫码：UPZF 京东扫码：JDZF QQ WAP：QQWEP
    private static final String tranTime               ="tranTime";  //  交易时间  格式：yyyyMMddHHmmss
    private static final String expireTime             ="expireTime";//  有效期    最大120分钟
    private static final String noticeUrl              ="noticeUrl"; //  回调地址  支付结果的通知地址
    private static final String subject                ="subject";   //  商品名称  例：苹果
    private static final String orderDesc              ="orderDesc"; //  订单描述   20个字符以内

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merCode, channelWrapper.getAPI_MEMBERID());
                   put(tranNo,channelWrapper.getAPI_ORDER_ID());
                   put(tranAmt,  channelWrapper.getAPI_AMOUNT());
                   put(settleType,"0");
                   put(collectWay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                   put(tranTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                   put(expireTime,"5");
                   put(noticeUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                   put(subject,"name");
                   put(orderDesc,"order Desc");
            }
        };
        log.debug("[麦克支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[麦克支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[麦克支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("respCode") && "2".equalsIgnoreCase(jsonObject.getString("respCode"))
                              && jsonObject.containsKey("codeUrl") && StringUtils.isNotBlank(jsonObject.getString("codeUrl"))) {
                    String code_url = jsonObject.getString("codeUrl");
                    result.put( JUMPURL , code_url);
                }else {
                    log.error("[麦克支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[麦克支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[麦克支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[麦克支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}