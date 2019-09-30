package dc.pay.business.yiyouzhifu;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * Mar 25, 2019
 */
@RequestPayHandler("YIYOUZHIFU")
public final class YiYouZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiYouZhiFuPayRequestHandler.class);

    private static final String payKey                ="payKey";         //  商户支付Key       String     否     32
    private static final String orderPrice            ="orderPrice";     //  订单金额，单位：元 保留小数点后两位     String     否     12
    private static final String outTradeNo            ="outTradeNo";     //  商户支付订单号     String     否     30
    private static final String productType           ="productType";    //  产品类型，请查阅本文档2.6      String     否     8
    private static final String orderTime             ="orderTime";      //  下单时间，格式：yyyyMMddHHmmss     String     否     14
    private static final String productName           ="productName";    //  支付产品名称       String     否     200
    private static final String orderIp               ="orderIp";        //  下单IP           String     否     15
    private static final String returnUrl             ="returnUrl";      //  页面通知地址       String     否     300
    private static final String notifyUrl             ="notifyUrl";      //  后台异步通知地址    String     否     300
    private static final String signature             ="sign";           //  签名              String     否     50
//  private static final String subPayKey             ="subPayKey";      //  子商户支付Key，大商户时必填     String     是     32
    private static final String bankCode              ="bankCode";       //  产品为B2C支付时，填银行编码（请查阅本文档2.7），//产品为WAP，扫码时，填 OTHER
    private static final String bankAccountType       ="bankAccountType";//  支付银行卡类型     PRIVATE_DEBIT_ACCOUNT               对私借记卡     PRIVATE_CREDIT_ACCOUNT               对私贷记卡     B2C支付时，二选一     WAP，扫码支付时，随便选其中一个     String     否     10
    private static final String isShow                ="isShow";         //  扫码     isShow=1时：直接跳转。     isShow=0时
    private static final String key                   ="paySecret";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                 put(payKey, channelWrapper.getAPI_MEMBERID());
                 put(orderPrice,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                 put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                 put(productType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                 put(productName,"name");
                 put(orderIp,channelWrapper.getAPI_Client_IP());
                 put(returnUrl,channelWrapper.getAPI_WEB_URL());
                 put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                 if (HandlerUtil.isYLKJ(channelWrapper)){
                      put(bankCode,"OTHER");
                 }else {
                      put(bankCode,"OTHER");
                 }
                 put(bankAccountType,"PRIVATE_DEBIT_ACCOUNT");
                 put(isShow,"1");
            }
        };
        log.debug("[亦友支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[亦友支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[亦友支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[亦友支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[亦友支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}