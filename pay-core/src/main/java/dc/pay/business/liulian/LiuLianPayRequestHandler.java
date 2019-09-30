package dc.pay.business.liulian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 23, 2018
 */
@RequestPayHandler("LIULIAN")
public final class LiuLianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LiuLianPayRequestHandler.class);

    //参数名称          参数含义                     参数说明                                       是否必填          长度          签名顺序
    //p1_MerchantNo      商户编号                    商户在支付平台系统的唯一身份标识。                是          X(15)          1
    //p2_OrderNo         商户订单号                  商户系统提交的唯一订单号。                        是          X(30)          2
    //p3_Amount          订单金额                    单位:元，精确到分，保留两位小数。例如：10.23。    是          D(16,2)          3
    //p4_Cur             交易币种                    默认设置为1（代表人民币）。                       是          X(10)          4
    //p5_ProductName     商品名称                    用于支付时显示在支付平台网关上的订单产品信息。    是          X(60)          5
    //p6_NotifyUrl       服务器异步通知地址          支付系统主动通知商户网站里指定的http地址          是          X(300)          6
    //tranType           交易类型                    1.扫码支付 2.商家扫码                             是          X(1)          7
    //authCode           线下授权码                  只有商家扫码模式才会上传                          否          X(16)          8
    //sign               签名数据                    参见5签名机制                                     是          X(4000)          
    private static final String p1_MerchantNo              ="p1_MerchantNo";
    private static final String p2_OrderNo                 ="p2_OrderNo";
    private static final String p3_Amount                  ="p3_Amount";
    private static final String p4_Cur                     ="p4_Cur";
    private static final String p5_ProductName             ="p5_ProductName";
    private static final String p6_NotifyUrl               ="p6_NotifyUrl";
    private static final String tranType                   ="tranType";
//    private static final String authCode                   ="authCode";
//    private static final String sign                       ="sign";

    //h5
    //bizType   业务类型
    private static final String bizType                     ="bizType";
    //p7_pageUrl    页面跳转地址  支付成功跳转地址    是   X(300)  7
    private static final String p7_pageUrl                  ="p7_pageUrl";
    
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(p1_MerchantNo, channelWrapper.getAPI_MEMBERID());
                put(p2_OrderNo,channelWrapper.getAPI_ORDER_ID());
                put(p3_Amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(p4_Cur,"1");
                put(p5_ProductName,"name");
                put(p6_NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                if (handlerUtil.isWapOrApp(channelWrapper)) {
                    put(bizType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(p7_pageUrl,channelWrapper.getAPI_WEB_URL());
                }else if (handlerUtil.isZFB(channelWrapper)) {
                    put(tranType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                //           线下授权码                  只有商家扫码模式才会上传                          否          X(16)          8
//                put(authCode,"1");
            }
        };
        log.debug("[榴莲]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(p1_MerchantNo));
        signSrc.append(api_response_params.get(p2_OrderNo));
        signSrc.append(api_response_params.get(p3_Amount));
        signSrc.append(api_response_params.get(p4_Cur));
        signSrc.append(api_response_params.get(p5_ProductName));
        signSrc.append(api_response_params.get(p6_NotifyUrl));
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("LIULIAN_BANK_WEBWAPAPP_ZFB_SM")) {
            signSrc.append(api_response_params.get(tranType));
//        } else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("LIULIAN_BANK_WEBWAPAPP_ZFB_SM")) {
        } else if (handlerUtil.isWapOrApp(channelWrapper)) {
            signSrc.append(api_response_params.get(p7_pageUrl));
            signSrc.append(api_response_params.get(bizType));
        }
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[榴莲]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[榴莲]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[榴莲]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
               log.error("[榴莲]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
               throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[榴莲]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("ra_Status") && "100".equalsIgnoreCase(resJson.getString("ra_Status"))  && resJson.containsKey("ra_Code") && StringUtils.isNotBlank(resJson.getString("ra_Code"))) {
                String code_url = resJson.getString("ra_Code");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[榴莲]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[榴莲]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[榴莲]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}