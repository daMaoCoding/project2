package dc.pay.business.shouyinbaozhifu;

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
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * May 1, 2019
 */
@RequestPayHandler("SHOUYINBAOZHIFU")
public final class ShouYinBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShouYinBaoZhiFuPayRequestHandler.class);

    private static final String time                  ="time";        // 秒级时间戳 1543315597
    private static final String third_num             ="third_num";   // 商户号 9991
    private static final String order_price           ="order_price"; // 订单价格 2000
    private static final String order_num             ="order_num";   // 订单号唯⼀ PT_AAA01 唯⼀不可重复
    private static final String attach                ="attach";      // ⾃定义参数 RX123353D
    private static final String redirect_url          ="redirect_url";// 订单回调地址 http://www.xxxxx.cn/api/redirectConfirm⽤于订单成功后发送成回调
    private static final String code_type             ="code_type";   // ⽀付类型 1/2 （默认1）1:微信2:⽀付宝


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(time,String.valueOf(System.currentTimeMillis()));
                put(third_num, channelWrapper.getAPI_MEMBERID());
                put(order_price,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_num,channelWrapper.getAPI_ORDER_ID());
                put(attach,channelWrapper.getAPI_ORDER_ID());
                put(redirect_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(code_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[收银宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //密钥 + md5( third_num+order_price+ order_num+time+redirect_url+attach )
                String paramsStr = String.format("%s%s%s%s%s%s",
                api_response_params.get(third_num),
                api_response_params.get(order_price),
                api_response_params.get(order_num),
                api_response_params.get(time),
                api_response_params.get(redirect_url),
                api_response_params.get(attach));
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        signMd5 = HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()+signMd5).toLowerCase();
        log.debug("[收银宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
                //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[收银宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("code") && "200".equalsIgnoreCase(jsonObject.getString("code"))
                        && jsonObject.containsKey("get_img_url") && StringUtils.isNotBlank(jsonObject.getString("get_img_url"))) {
                    String code_url = jsonObject.getString("get_img_url");
                    if (!code_url.contains("http://")){
                        code_url="http://"+code_url;
                    }
                    result.put( JUMPURL , code_url);
                }else {
                    log.error("[收银宝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[收银宝]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[收银宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[收银宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}