package dc.pay.business.yingxionglianmeng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 15, 2019
 */
@RequestPayHandler("YINGXIONGLIANMENG")
public final class YingXiongLianMengPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YingXiongLianMengPayRequestHandler.class);

    //请求：
    //字段名 类型  最大长度    必填  说明
    //merchant    int32       是   商户ID
    private static final String merchant               ="merchant";
    //project int32       是   项目ID
    private static final String project               ="project";
    //product int32       是   商品ID
    private static final String product               ="product";
    //morder  string  32  是   商户订单ID
    private static final String morder               ="morder";
    //channel int32       是   物理通道ID
    private static final String channel               ="channel";
    //paytype int32       是   交易类型ID
    private static final String paytype               ="paytype";
    //price   int32       可选  交易金额，以分为单位。
    private static final String price               ="price";
    //callback    string  1024    是   商户的支付回调链接
    private static final String callback               ="callback";
    //sign    string  1024    是   签名
//    private static final String sign               ="sign";

//    private static final String key        ="paySecret";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
            log.error("[英雄联盟]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchant&项目IDproject&商品IDproduct" );
            throw new PayException("[英雄联盟]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchant&项目IDproject&商品IDproduct" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant,  channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(project,  channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(product,  channelWrapper.getAPI_MEMBERID().split("&")[2]);
                put(channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(morder,channelWrapper.getAPI_ORDER_ID());
                put(price, channelWrapper.getAPI_AMOUNT());
                put(callback,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[英雄联盟]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map api_response_params) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        for (int i = 0; i < paramKeys.size(); i++) {
            signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        signSrc.deleteCharAt(signSrc.length()-1);
        String signInfo = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY(),"SHA256withRSA");   // 签名
        } catch (Exception e) {
            log.error("[英雄联盟]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        log.debug("[英雄联盟]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if(HandlerUtil.isWapOrApp(channelWrapper)){
//        if(true){
        if(false){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }
        
        else{
            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[英雄联盟]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[英雄联盟]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[英雄联盟]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[英雄联盟]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && JSONObject.parseObject(jsonObject.getString("data")).containsKey("link") && StringUtils.isNotBlank(JSONObject.parseObject(jsonObject.getString("data")).getString("link"))) {
                String respType = JSONObject.parseObject(jsonObject.getString("data")).getString("link");
                result.put( JUMPURL, respType);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[英雄联盟]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[英雄联盟]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[英雄联盟]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}