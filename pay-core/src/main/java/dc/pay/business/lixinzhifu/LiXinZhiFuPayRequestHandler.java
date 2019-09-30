package dc.pay.business.lixinzhifu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("LIXINZHIFU")
public final class LiXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LiXinZhiFuPayRequestHandler.class);


    private static final String     amount = "amount";           //	交易金额，单位：分	是	int	11	以“分”为单位，不能带小数点。如1.55元，值为：155；
    private static final String     subject = "subject";           //	商品名称	是	String	12
    private static final String     body = "body";           //	描述	是	String	100
    private static final String     paymentType = "paymentType";           //	支付类型	是	String	20	微信、支付宝统一: WEIXIN_QRCODE
    private static final String     notifyUrl = "notifyUrl";           //	后台回调地址	是	String	500
    private static final String     frontUrl = "frontUrl";           //
    private static final String     spbillCreateIp = "spbillCreateIp";           //	客户端发起的IP	是
    private static final String     tradeNo = "tradeNo";           //	商户订单号, 商户平台全局唯一的编号	是	String	32
    private static final String     merchantNo = "merchantNo";           //	商户号	是	String	32
    private static final String     operationCode = "operationCode";           //	业务类型编号	是	String	50	下单: order.createOrder, 支付查询:order.query，
    private static final String     version = "version";           //	接口版本号	是	String	10	默认为：1.0
    private static final String     date = "date";           //	时间戳。  是	String		例如：1523875802853
    private static final String     sign = "sign";           //	签名，所有非空字段	是	String
    private static final String     createOrder = "order.createOrder";



      private static final String   code = "code";  // "100",
      private static final String   msg = "msg";  // "请求成功",
      private static final String   platformOrderId = "platformOrderId";  // "1037569809597857792",
      private static final String   status = "status";  // "2",
      private static final String   parameter1 = "parameter1";  // null,
      private static final String   message = "message";  // "请求成功",
      private static final String   payCode = "payCode" ;  // http://qr.liantu.com/api.php?&w=280&text=HTTPS://QR.ALIPAY.COM/FKX00375Y8VMN7UVWHWB2D?t=1536158385210
      private static final String   extents = "extents";  // null,
      private static final String   channelNo = "channelNo";  // null,
      private static final String   channelType = "channelType";  // "SHANHONG_PAY"



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(paymentType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(frontUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(spbillCreateIp,channelWrapper.getAPI_Client_IP());
            payParam.put(tradeNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(merchantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(operationCode,createOrder);
            payParam.put(version,"1.0");
            payParam.put(date,System.currentTimeMillis()+"");
        }
        log.debug("[利信支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("appkey=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[利信支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey(code) && "100".equalsIgnoreCase(jsonResultStr.getString(code)) && jsonResultStr.containsKey(payCode) && StringUtils.isNotBlank(jsonResultStr.getString(payCode))){
                            if(HandlerUtil.isWapOrApp(channelWrapper) ){
                                result.put(JUMPURL,  jsonResultStr.getString(payCode));
                                payResultList.add(result);
                            }else{throw new PayException(resultStr);}
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[利信支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[利信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[利信支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}