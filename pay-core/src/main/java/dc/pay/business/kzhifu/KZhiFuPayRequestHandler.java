package dc.pay.business.kzhifu;

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

import java.util.*;

@RequestPayHandler("KZHIFU")
public final class KZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KZhiFuPayRequestHandler.class);

    private static final String   merAccount = "merAccount" ;        //	是	String(32)	商户标识，由系统随机生成
    private static final String   merNo = "merNo" ;        //	是	String(30)	用户编号，由系统生成
    private static final String   orderId = "orderId" ;        //	是	String(32)	商户订单号，由商户自行生成，必须唯一
    private static final String   time = "time" ;        //	是	long	时间戳，例如：1510109185，精确到秒，前后误差不超过5分钟
    private static final String   amount = "amount" ;        //	是	int	支付金额，单位分，必须大于0
    private static final String   productType = "productType" ;        //	是	String(10)	商品类别码，固定值01
    private static final String   product = "product" ;        //	是	String(50)	商品名称
    private static final String   productDesc = "productDesc" ;        //	否	String(200)	商品描述
    private static final String   userType = "userType" ;        //	是	int	用户类型，固定值0
    private static final String   payWay = "payWay" ;        //	是	String(20)	支付方式，银联：UNIONPAY，微信：WEIXIN， QQ钱包：QQPAY，  支付宝：ALIPAY
    private static final String   payType = "payType" ;        //	是	String(30)	支付类型，参考支付类型表
    private static final String   userIp = "userIp" ;        //	是	String(20)	用户IP地址
    private static final String   notifyUrl = "notifyUrl" ;        //	否	String(255)	商户异步通知地址
    private static final String   sign	   = "sign" ;                //是	string	签名
    private static final String   data	   = "data" ;

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接 商戶编号和商户标识,如：商戶编号&商户标识");
        }
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merAccount,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(merNo,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(orderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(time,System.currentTimeMillis()/1000+"");
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(productType,"01");
            payParam.put(product,channelWrapper.getAPI_ORDER_ID());
            payParam.put(productDesc,channelWrapper.getAPI_ORDER_ID());
            payParam.put(userType,"0");
            payParam.put(payWay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(userIp,channelWrapper.getAPI_Client_IP());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[K支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String sign=null;
        StringBuffer buffer		= new StringBuffer();
        TreeMap<String, Object> treeMap = new TreeMap<String, Object>(params);
        for (Map.Entry<String, Object> entry : treeMap.entrySet()) {
            if (entry.getValue() != null) {
                buffer.append(entry.getValue());
            }
        }
        buffer.append(channelWrapper.getAPI_KEY());
        sign=  HashUtil.sha1(buffer.toString());

        //JSONObject json =  JSONObject.parseObject(JSON.toJSONString(params));
       // String sign = KZhiFuUtils.buildSign(json,channelWrapper.getAPI_KEY());
        log.debug("[K支付]-[请求支付]-2.生成加密URL签名完成：" + sign);
        return sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            //String dataStr =  BouncyCastleAES.encrypt(JSON.toJSONString(payParam),channelWrapper.getAPI_KEY());
            String dataStr =  BouncyCastleAES.encode3(JSON.toJSONString(payParam),channelWrapper.getAPI_KEY());
            Map<String, String> paramMap	= new HashMap<String, String>();
            paramMap.put(data, dataStr);
            paramMap.put(merAccount, channelWrapper.getAPI_MEMBERID().split("&")[1]);

            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),paramMap).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), paramMap, String.class, HttpMethod.POST).trim();
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "000000".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey(data)
                             && null!=jsonResultStr.getJSONObject(data) && jsonResultStr.getJSONObject(data).containsKey("qrCode") && StringUtils.isNotBlank(jsonResultStr.getJSONObject(data).getString("qrCode"))){
                                result.put(JUMPURL, jsonResultStr.getJSONObject(data).getString("qrCode"));
                                payResultList.add(result);
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[K支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[K支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[K支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}