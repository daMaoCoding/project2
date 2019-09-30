package dc.pay.business.yiersanzhifu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * ************************
 * @author tony 3556239829
 */

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestPayHandler("YIERSANZHIFU")
public final class YiErSanZhiFuPayRequestHandler extends PayRequestHandler {
    
    //属性  含义  必填  最大长度    值   备注
    //accountId   商户编号    是   32  9165598850  
    private static final String accountId                    ="accountId";
    //method  请求方法    是   128 sfbpay.pay.create   
    private static final String method                    ="method";
    //timestamp   请求时间    是   19  2014-07-24 03:07:50 格式：yyyy-MM-dd HH:mm:ss
    private static final String timestamp                    ="timestamp";
    //reqIp   请求方IP   是   15  255.255.255.255 IPV4地址，该地址请传客户请求的IP 地址
    private static final String reqIp                    ="reqIp";
    //dataType    数据格式    是   32  json    目前只支持json
    private static final String dataType                    ="dataType";
    //signType    签名方式    是   10  MD5 目前只支持md5
    private static final String signType                    ="signType";
    //charset 编码格式    是   10  utf-8   目前只支持utf-8
    private static final String charset                    ="charset";
    //nonceStr    随机字符串   是   32  6465465464546456
    private static final String nonceStr                    ="nonceStr";
    //bizContent  业务请求参数的集合   是   -   业务请求参数的集合，最大长度不限，除公共参数外所有请求参数都必须放在这个参数中传递
    private static final String bizContent                    ="bizContent";
    
    //3.1.请求方法（method）
    //方法  sfbpay.pay.create
    //3.2.接口请求参数
    //属性  含义  必填  最大长度    值   备注
    //orderNo 订单号 是   32  655555555555555 商户生成唯一订单号
    private static final String orderNo                    ="orderNo";
    //content 订单描述    否   256 袜子  
//    private static final String content                    ="content";
    //clientIp    客户端IP   是   15  117.30.42.152   客户端IP
    private static final String clientIp                    ="clientIp";
    //money   订单金额    是   10  3000~200000（单位分）    订单金额不一定是支付金额，系统会根据风控规则检测金额是否可以支付，不排除因为金额导致订单创建失败，支付金额必须以回调的支付金额payMoney参数为准
    private static final String money                    ="money";
    //title   标题  是   32  袜子  
    private static final String title                    ="title";
    //notifyUrl   异步通知地址  是   256 http://demain/notify    
    private static final String notifyUrl                    ="notifyUrl";
    //returnUrl   支付成功后返回地址   否   256 http://demain/return    
//    private static final String returnUrl                    ="returnUrl";

    //sign    签名  是   256 B2EF9C7B10EB0985365F913420CCB84A    大写
    private static final String sign                    ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        payParam.put(accountId, channelWrapper.getAPI_MEMBERID());
        payParam.put(method, "sfbpay.pay.create");
        payParam.put(timestamp, DateUtil.curDateTimeStr());
        payParam.put(reqIp, channelWrapper.getAPI_Client_IP());
        payParam.put(dataType, "json");
        payParam.put(signType, "MD5");
        payParam.put(charset, "utf-8");
        payParam.put(nonceStr, HandlerUtil.getRandomStr(10));
        payParam.put(bizContent, this.getBizContent());
        log.debug("[123支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    private String getBizContent() throws PayException{
        Map<String, String> bizParamas = Maps.newHashMap();
        bizParamas.put(orderNo, channelWrapper.getAPI_ORDER_ID());
        bizParamas.put(clientIp, channelWrapper.getAPI_Client_IP());
        bizParamas.put(money, channelWrapper.getAPI_AMOUNT());
        bizParamas.put(title, "goods");
        bizParamas.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        String bizContent = JSON.toJSONString(bizParamas);
        log.debug("业务参数：{}",bizContent);
        return bizContent; 
    }
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            String keyName = paramKeys.get(i);
            String value = api_response_params.get(keyName);
            if(keyName == "sign") continue;
            
            if (StringUtils.isNotBlank(value)) {
                signSrc.append(paramKeys.get(i)).append("=").append(value).append("&");
            }
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[123支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();

        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
        }else{              
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[123支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[123支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[123支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[123支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("code") && "200".equalsIgnoreCase(jsonObject.getString("code"))  && 
            (jsonObject.containsKey("result") && StringUtils.isNotBlank(jsonObject.getString("result")) || 
             jsonObject.getJSONObject("result").containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getJSONObject("result").getString("payUrl")))
            ){
//            if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                String code_url = jsonObject.getJSONObject("result").getString("payUrl");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                result.put( JUMPURL , code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[123支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[123支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[123支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}