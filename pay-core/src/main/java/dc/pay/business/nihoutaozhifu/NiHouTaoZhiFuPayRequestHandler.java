package dc.pay.business.nihoutaozhifu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("NIHOUTAOZHIFU")
public final class NiHouTaoZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

     private static final String   merNum = "merNum";   //商户号		是	String	me0001	分配商户号
     private static final String   payType = "payType";   //支付类型		是	Int	2	参考附录1
     private static final String   amount = "amount";   //支付金额		是	BigDecimal	1.00	单位元(人民币),2位小数
     private static final String   orderNum = "orderNum";   //订单号		是	String	20180901001	商户系统订单号
     private static final String   notifyUrl = "notifyUrl";   //回调地址支付结果异步通知地址		是	String 	http://xxx
     private static final String   ip = "ip";   //支付用户		是	String	127.0.0.1	用户在下单时IP
     private static final String   sign = "sign";   //MD5签名		是	String		详见签名说明
     private static final String   SHH = "商户号";



      private static final String  code = "code";   // 1000,
      private static final String  msg = "msg";   // null,
      private static final String  qrCode = "qrCode" ;   //"https://qr.alipay.com/bax02027naglfftjdjhm006c",
      private static final String  payOrderNum = "payOrderNum";   // "z15426084966102436"



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merNum,channelWrapper.getAPI_MEMBERID());
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(orderNum,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(ip,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[猕猴桃支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // String sign = "merNum="+merNo+"&orderNum="+orderNum+"&notifyUrl="+notifyUrl+""+"&payType="+Integer.parseInt(payType)+"&amount="+amount+"&secreyKey="+secreyKey;
        String paramsStr = String.format("merNum=%s&orderNum=%s&notifyUrl=%s&payType=%s&amount=%s&secreyKey=%s",
                params.get(merNum),
                params.get(orderNum),
                params.get(notifyUrl),
                params.get(payType),
                params.get(amount),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[猕猴桃支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String  bankUrl = channelWrapper.getAPI_CHANNEL_BANK_URL().replace(SHH,payParam.get(merNum));
        String resultStr;
        try {
            if (1==2  && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(bankUrl,payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

                resultStr = RestTemplateUtil.postJson(bankUrl, payParam);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "1000".equalsIgnoreCase(jsonResultStr.getString("code"))
                            && jsonResultStr.containsKey("qrCode") && StringUtils.isNotBlank(jsonResultStr.getString("qrCode"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("qrCode"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("qrCode"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[猕猴桃支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[猕猴桃支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[猕猴桃支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}