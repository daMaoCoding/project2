package dc.pay.business.youfuquanqiu;

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

@RequestPayHandler("YOUFUQUANQIU")
public final class YouFuQuanQiuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YouFuQuanQiuPayRequestHandler.class);


    private static final String  customerid = "customerid";     //  商户ID  否  商户在系统上的商户号
    private static final String  sdcustomno = "sdcustomno";     //  商户流水号  否  订单在商户系统中的流水号
    private static final String  orderAmount = "orderAmount";     //  支付金额  否  订单支付金额；单位:分(人民币)
    private static final String  cardno = "cardno";     //  支付方式  否  见附表
    private static final String  noticeurl = "noticeurl";     //  通知商户Url  否  通知商户的地址，该地址不能带任何参数，否则异步通知会不成功
    private static final String  backurl = "backurl";     //  回调Url  否  浏览器跳转商户的地址
    private static final String  sign = "sign";     //  md5签名  否  签名,以上参数拼接商户在密钥（key）一起按照顺序MD5加密并转为大写 的字符串
    private static final String  mark = "mark";     //  商户自定义 信息  否  例如 数字+英文字母 不能存在中文 例如：test123
    private static final String  remarks = "remarks";     //  支付商品名 称  是  可以为中文说明，该参数如果未传人参数或空值时，支付商品名称会默认使 用mark参数，该字段不参加提交和回调的MD5加密。
    private static final String  zftype = "zftype";     //  返回类型  是  Zftype=2直接返回调起链接
    private static final String  loIp = "loIp";
    private static final String  authcode = "authcode";



    //http://api.unpay.com/PayMegerHandler.ashx?customerid=301738&sdcustomno=20180824171315dd0f13&orderAmount=30000&cardno=41&noticeurl=http://www.baidu.com&backurl=http://www.baidu.com&mark=20180824171159ee59ff&remarks=&loIp=&authcode=&zftype=1&sign=4D774116563DD49D88DEEE712F008732




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(customerid,channelWrapper.getAPI_MEMBERID());
            payParam.put(sdcustomno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderAmount,channelWrapper.getAPI_AMOUNT());
            payParam.put(cardno,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(noticeurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(backurl,channelWrapper.getAPI_WEB_URL());
            payParam.put(mark,channelWrapper.getAPI_ORDER_ID());
            payParam.put(zftype,"1");  //2：xml 3：json
            payParam.put(loIp,"");  //允许为空,H5支付必传(loIp首字母为小写L,第三个为大写的i)
            payParam.put(authcode,"");  //微信刷卡必传（微信刷卡返回类型3）
        }
        log.debug("[优付全球]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // customerid=%s&sdcustomno=%s&orderAmount=%s&cardno=%s&noticeurl=%s&backurl=%s%s
        // customerid=%s&sdcustomno=%s&orderAmount=%s&cardno=%s&noticeurl=%s&backurl=http://www.baidu.com42C8938E4CF5777700700E642DC2A8CD
        String paramsStr = String.format("customerid=%s&sdcustomno=%s&orderAmount=%s&cardno=%s&noticeurl=%s&backurl=%s%s",
                params.get(customerid),
                params.get(sdcustomno),
                params.get(orderAmount),
                params.get(cardno),
                params.get(noticeurl),
                params.get(backurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[优付全球]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[优付全球]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[优付全球]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[优付全球]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}