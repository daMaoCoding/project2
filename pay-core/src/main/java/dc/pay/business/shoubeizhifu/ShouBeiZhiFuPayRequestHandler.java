package dc.pay.business.shoubeizhifu;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("SHOUBEIZHIFU")
public final class ShouBeiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShouBeiZhiFuPayRequestHandler.class);

      private static final String     version = "version";  //	版本号	ANS1..6	M	R	用于版本控制，向下兼容，当前版本号为1.0
      private static final String     charset = "charset";  //	参数编码字符集	ANS1..10	M	M	UTF-8
      private static final String     merId = "merId";  //	商户号	AN16	M	R	平台分配的唯一商户编号
      private static final String     orderTime = "orderTime";  //	商户订单提交时间	yyyyMMddHHmmss	M	R	数字串，一共14 位 格式为：yyyyMMddHHmmss 例如：20160820113900
      private static final String     transCode = "transCode";  //	交易类型	AN16	M	R	交易类型，详见交易类型表
      private static final String     signType = "signType";  //	签名方式	ANS1..6	M	M	签名方式：MD5
      private static final String     transactionId = "transactionId";  //     商户订单号	AN10..32	M	字符串，只允许使用字母、数字、- 、_,并以字母或数字开头，每商户提交的订单号，必须在自身账户交易中唯一
      private static final String     orderAmount = "orderAmount";  //     商户订单金额	ANS1..10	M	浮点数DECIMAL(10,2)；以元为单位，例如10元，金额格式为10.00
      private static final String     orderDesc = "orderDesc";  //     订单描述	ANS1..500	C	订单描述
      private static final String     payType = "payType";  //     支付方式	N4	M	详见附录 通道编号
      private static final String     productName = "productName";  //     商品名称	ANS1..40	C	英文或中文字符串
      private static final String     productDesc = "productDesc";  //     商品描述	ANS1..256	C	英文或中文字符串
      private static final String     bgUrl = "bgUrl";  //     支付结果后台通知地址	ANS1..256	C	在线支付平台支持后台通知时必填
      private static final String     pageUrl = "pageUrl";  //     	页面回跳地址	ANS1..256	C	成功支付后页面回调地址
      private static final String     mch_create_ip = "mch_create_ip";  //     	用户终端IP	IPV4	M	必填，不参与签名
    //  private static final String     bank_code = "bank_code";  //     	银行代码		C	银行代码，不参与签名，详见附录，网银或快捷必填，0405时直接传入银行卡号，
      private static final String     signData = "signData";  //	签名	AN32	M	M	签名信息


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"1.0");
            payParam.put(charset,"UTF-8");
            payParam.put(merId,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(transCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(signType,"MD5");
            payParam.put(transactionId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(orderDesc,channelWrapper.getAPI_ORDER_ID());
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(productName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(productDesc,channelWrapper.getAPI_ORDER_ID());
            payParam.put(bgUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pageUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(mch_create_ip,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[收呗支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || signData.equalsIgnoreCase(paramKeys.get(i).toString()) || mch_create_ip.equalsIgnoreCase(paramKeys.get(i).toString())  )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[收呗支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("retCode") && "RC0000".equalsIgnoreCase(jsonResultStr.getString("retCode")) && jsonResultStr.containsKey("qrCodeVal")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("qrCodeVal"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString("qrCodeVal"));
                                }else{
                                    result.put(QRCONTEXT, jsonResultStr.getString("qrCodeVal"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[收呗支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[收呗支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[收呗支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}