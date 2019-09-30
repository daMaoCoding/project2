package dc.pay.business.xinkoubei;

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
 * Jul 10, 2018
 */
@RequestPayHandler("XINKOUBEI")
public final class XinKouBeiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinKouBeiPayRequestHandler.class);

    //输入项            输入项名称      属性      注释                                                                          数据类型
    //mid               商户号          M        商户号                                                                          as..32
    //orderNo           商户订单号      M        建议： 日期(YYYYMMDDHHMMSS)+商户首字母（4字节）+商户交易流水号（12字节））      as..4
    //product_name      订单标题        M        可放置商品名称                                                                  N14
    //body              订单描述        M        商品的简要描述      
    //amount            订单金额        M        ##.## (圆.角分)      
    //notifyUrl         通知URL                      
    //pz_userId         唯一编号        M        用户编号      
    //mch_create_ip     外网IP          M        用户设备外网      
    //returnUrl         同步回调地址             同步回调地址      
    //sign              签名            M        数字签名md5(md5(key)+mid+orderNo+amount)      
    private static final String mid                        ="mid";
    private static final String orderNo                    ="orderNo";
    private static final String product_name               ="product_name";
    private static final String body                       ="body";
    private static final String amount                     ="amount";
    private static final String notifyUrl                  ="notifyUrl";
    private static final String pz_userId                  ="pz_userId";
    private static final String mch_create_ip              ="mch_create_ip";
    private static final String returnUrl                  ="returnUrl";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mid, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(product_name,"name");
                put(body,"name");
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pz_userId, handlerUtil.getRandomStr(5));
                put(mch_create_ip,channelWrapper.getAPI_Client_IP());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[新呗]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()).toLowerCase());
        signSrc.append(api_response_params.get(mid));
        signSrc.append(api_response_params.get(orderNo));
        signSrc.append(api_response_params.get(amount));
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新呗]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
    	if (HandlerUtil.isWapOrApp(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }
//    	else{
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[新呗]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
//                throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
//            }
//            if (!resultStr.contains("<form")) {
//            	log.error("[新呗]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            	throw new PayException(resultStr);
//            }
//            Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
//            Element formEl = document.getElementsByTag("form").first();
//            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
//            String resultStr2 = RestTemplateUtil.postForm(secondPayParam.get("action"), secondPayParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr2)) {
//                log.error("[新呗]-[请求支付]-3.3.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(secondPayParam));
//                throw new PayException("返回空,参数："+JSON.toJSONString(secondPayParam));
//            }
//            if (!resultStr2.contains("<a") && !resultStr2.contains("href")) {
//            	log.error("[新呗]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            	throw new PayException(resultStr2);
//            }
//            result.put(HTMLCONTEXT, resultStr2);
//        }
    	ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新呗]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新呗]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}