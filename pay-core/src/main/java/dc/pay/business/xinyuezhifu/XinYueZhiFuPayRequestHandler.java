package dc.pay.business.xinyuezhifu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * Mar 18, 2019
 */
@RequestPayHandler("XINYUEZHIFU")
public final class XinYueZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinYueZhiFuPayRequestHandler.class);

    private static final String cpId              ="cpId";        //     是     商户ID
    private static final String serviceId         ="serviceId";   //     是     服务ID
    private static final String payType           ="payType";     //     是     支付类型（见附录2）
    private static final String fee               ="fee";         //     是     支付金额（单位分）
    private static final String subject           ="subject";     //     是     商品名
    private static final String description       ="description"; //     是     商品说明
    private static final String orderIdCp         ="orderIdCp";   //     是     商户订单号
    private static final String notifyUrl         ="notifyUrl";   //     是     异步通知地址
    private static final String callbackUrl       ="callbackUrl"; //     是     前端回调地址
//    private static final String cpParam           ="cpParam";     //     否     透传参数
//    private static final String bankCode          ="bankCode";    //     否     银行编码（注1）
    private static final String userIdentity      ="userIdentity";//     是     用户识别码（注2）
    private static final String timestamp         ="timestamp";   //     是     当前时间戳（13位）
    private static final String ip                ="ip";          //     是     用户ip
    private static final String version           ="version";     //     是     版本号（固定1）

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
            log.error("[心悦支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户ID&支付宝服务ID&银联服务ID" );
            throw new PayException("[心悦支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户ID&支付宝服务ID&银联服务ID" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(cpId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                if (HandlerUtil.isYLKJ(channelWrapper)){
                    put(serviceId, channelWrapper.getAPI_MEMBERID().split("&")[2]);
                }else {
                    put(serviceId, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                }
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(fee, channelWrapper.getAPI_AMOUNT());
                put(subject,"name");
                put(description,"description");
                put(orderIdCp,channelWrapper.getAPI_ORDER_ID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callbackUrl,channelWrapper.getAPI_WEB_URL());
                put(userIdentity,channelWrapper.getAPI_ORDER_ID());
                put(timestamp,System.currentTimeMillis()+"");
                put(ip,channelWrapper.getAPI_Client_IP());
                put(version,"1");
            }
        };
        log.debug("[心悦支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }


     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
//         请求支付接口：cpId、serviceId、payType、fee、subject、description、orderIdCp、notifyUrl、callbackUrl、timestamp、ip、version
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if ( !userIdentity.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[心悦支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
            if (1==2) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
                    String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[心悦支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("status") && "0".equalsIgnoreCase(jsonObject.getString("status"))
                        && jsonObject.containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getString("payUrl"))) {
                    String payUrl = jsonObject.getString("payUrl");
                        result.put(JUMPURL, payUrl);

                }else {
                    log.error("[心悦支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
            
        } catch (Exception e) {
            log.error("[心悦支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[心悦支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[心悦支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}