package dc.pay.business.xinjuyuan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 17, 2019
 */
@RequestPayHandler("XINJUYUANZHIFU")
public final class XinJuYuanZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinJuYuanZhiFuPayRequestHandler.class);

    //参数名 参数  可空  加入签名    说明
    //商户ID    partner N   Y   商户id,由支付API分配
    //银行类型    banktype    N   Y   银行类型，具体参考附录1 
    //金额  paymoney    N   Y   单位元（人民币）,精确到小数点后两位
    //商户订单号   ordernumber N   Y   商户系统订单号，该订单号将作为支付API接口的返回数据。该值需在商户系统内唯一，支付API系统暂时不检查该值是否唯一
    //下行异步通知地址    callbackurl N   Y   下行异步通知的地址，需要以http://开头且没有任何参数
    //MD5签名   sign    N   N   32位小写MD5签名值，UTF-8编码
    private static final String partner                 ="partner";
    private static final String banktype                ="banktype";
    private static final String paymoney                ="paymoney";
    private static final String ordernumber             ="ordernumber";
    private static final String callbackurl             ="callbackurl";
//    private static final String sign                    ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ordernumber,channelWrapper.getAPI_ORDER_ID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[新聚源支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
        signSrc.append(banktype+"=").append(api_response_params.get(banktype)).append("&");
        signSrc.append(paymoney+"=").append(api_response_params.get(paymoney)).append("&");
        signSrc.append(ordernumber+"=").append(api_response_params.get(ordernumber)).append("&");
        signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl));
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新聚源支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        
        if (true) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
////      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
////      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[新聚源支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (!resultStr.contains("http")) {
//                log.error("[新聚源支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if(HandlerUtil.isWapOrApp(channelWrapper)){
//                result.put(HTMLCONTEXT,resultStr);
//            }else {
//                
//                //支付宝扫码，不允许电脑上直接扫码    这方法靠谱
//                if ((handlerUtil.isWEBWAPAPP_SM(channelWrapper) && HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())) || 
//                        channelWrapper.getAPI_ORDER_ID().startsWith("T") || 
//                        handlerUtil.isWapOrApp(channelWrapper)) {
//                    //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
//                    //保存第三方返回值
//                    result.put(HTMLCONTEXT,resultStr);
//                }else {
//                    throw new PayException("请在APP或者WAP应用上使用通道......");
//                }
//                
//                //第三方只使用他们的收银台页面
////              String htmlUrl = HandlerUtil.getHtmlUrl(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
////              result.put( JUMPURL,htmlUrl);
//                
//                
////              if (!resultStr.contains("form")) {
////                  log.error("[新聚源支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                  throw new PayException(resultStr);
////              }
////              Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
////              Element bodyEl = document.getElementsByTag("body").first();
////              Element formEl = bodyEl.getElementsByTag("form").first();
////              Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
//////              String resultStr2 = RestTemplateUtil.postForm(secondPayParam.get("action").startsWith("http") ? secondPayParam.get("action") : "http://api.budepay.com/"+secondPayParam.get("action"), secondPayParam,"UTF-8");
////              String resultStr2 = RestTemplateUtil.sendByRestTemplate(secondPayParam.get("action").startsWith("http") ? secondPayParam.get("action") : "http://api.budepay.com/"+secondPayParam.get("action"), secondPayParam,String.class, HttpMethod.GET);
//////              String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
////              if (StringUtils.isBlank(resultStr2)) {
////                  log.error("[新聚源支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                  throw new PayException(resultStr2);
////              }
////              Elements elements = Jsoup.parse(resultStr2).select("[id=Image2]");
////              if (null == elements || elements.size() != 1) {
////                  log.error("[新聚源支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                  throw new PayException(resultStr2);
////              }
////              String src = elements.first().attr("src");
////              if (StringUtils.isBlank(src)) {
////                  log.error("[新聚源支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                  throw new PayException(resultStr2);
////              }
////              String qr = QRCodeUtil.decodeByUrl(src.startsWith("http") ? src : "http://125.88.183.191:1888/"+src);
////              if (StringUtils.isBlank(qr)) {
////                  log.error("[新聚源支付]-[请求支付]-3.7.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                  throw new PayException(resultStr2);
////              }
////              result.put(QRCONTEXT, qr);
//            }
//            
//        }
        
        
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新聚源支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新聚源支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}