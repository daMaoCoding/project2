package dc.pay.business.kaixinpay;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("KAIXINZHIFU")
public final class KaiXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KaiXinZhiFuPayRequestHandler.class);

    private static final String   notifyUrl = "notifyUrl";//	 下行异步通知地址   String 	N	下行异步通知的地址，需要以http://开头且没有任何参数
    private static final String   outOrderId = "outOrderId";//	 外部订单号   String	N	商户系统的订单编号
    private static final String   tradeName = "tradeName";//	 商品名称   String	N	商户系统的商品名称
    private static final String   amount = "amount";//	 交易金额   String	N	商户商品价格（元）两位小数
    private static final String   merId = "merId";//	 商户号   String	N
    private static final String   payWay = "payWay";//	 支付类型   String	N	支付宝扫码：alipay  支付宝h5：zfbh5 微信扫码：wxScan
    private static final String   sign = "sign";//	 MD5签名   String	N	32位MD5签名值



  private static final String    code = "code";  // 0,
  private static final String    payState = "payState";  // "success",
  private static final String    message = "message";  // "支付接口调用成功",
  private static final String    payUrl = "payUrl";  // "weixin:/pay/bizpayurl?appid=wxc8bda2993cd5ef62&mch_id=1273028201&nonce_str=1512181445&product_id=42963553717876329120&time_stamp=1536909138&sign=1576C40CDBC80DE9493B70A7F201ED1C",
  //private static final String    tradeName = "tradeName";  // "20180914151231",
  //private static final String    payWay = "payWay";  // "wxScan",
  //private static final String    amount = "amount";  // "10.00",
  private static final String    orderNo = "orderNo";  // "KX201809141512322644148"



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(outOrderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(tradeName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(merId,channelWrapper.getAPI_MEMBERID());
            payParam.put(payWay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        }
        log.debug("[开心pay支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //amount=10.00&merId=39ddedf5b&notifyUrl=http://www.baidu.com&outOrderId=1517194489924&payWay=zfbh5&tradeName=book_test&key=0d5d5187316e5c4f464f11c40dbf67f3
        String paramsStr = String.format("amount=%s&merId=%s&notifyUrl=%s&outOrderId=%s&payWay=%s&tradeName=%s&key=%s",
                params.get(amount),
                params.get(merId),
                params.get(notifyUrl),
                params.get(outOrderId),
                params.get(payWay),
                params.get(tradeName),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[开心pay支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper)  &&  HandlerUtil.isYLKJ(channelWrapper)   &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
/*				
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getValueAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
				
*/				
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{

                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr  && jsonResultStr.containsKey(payState) && "success".equalsIgnoreCase(jsonResultStr.getString(payState))   && jsonResultStr.containsKey(code) && "0".equalsIgnoreCase(jsonResultStr.getString(code)) && jsonResultStr.containsKey(payUrl)){
                            if(StringUtils.isNotBlank(jsonResultStr.getString(payUrl))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString(payUrl));
                                }else{
                                    result.put(QRCONTEXT, jsonResultStr.getString(payUrl));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[开心pay支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[开心pay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[开心pay支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}