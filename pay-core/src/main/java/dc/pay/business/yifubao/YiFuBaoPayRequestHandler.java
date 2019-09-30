package dc.pay.business.yifubao;

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
 * Nov 6, 2018
 */
@RequestPayHandler("YIFUBAO")
public final class YiFuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiFuBaoPayRequestHandler.class);

    //接口请求参数（Post）
    //接收参数               类型               必需           描述        示例
    //yfb_appid              String(15)          是           申请开户后新建自动生成的一个APPID号码        1001
    //yfb_total_fee          Numeric(10)         是           支付金额        100.00
    //yfb_type               int(1)              是           支付类型 1支付宝，2QQ钱包，3微信        1
    //yfb_out_trade_no       String(50)          是           商户订单号        jubaopen_2016567567454
    //yfb_webname            String(100)         是           网站名称        亿付宝支付
    //yfb_subject            String(100)         是           商品名        亿付宝支付产品
    //yfb_return_url         String(100)         是           支付成功返回网址        http://www.xxxx.com
    //yfb_notify_url         String(100)         是           支付成功异步通知        http://www.xxxx.com/notify.php
    //yfb_sign               String(32)          是           接口请求校验码        md5($appid.$appkey.$out_trade_no.$total_fee)
    private static final String yfb_appid                           ="yfb_appid";
    private static final String yfb_total_fee                       ="yfb_total_fee";
    private static final String yfb_type                            ="yfb_type";
    private static final String yfb_out_trade_no                    ="yfb_out_trade_no";
    private static final String yfb_webname                         ="yfb_webname";
    private static final String yfb_subject                         ="yfb_subject";
    private static final String yfb_return_url                      ="yfb_return_url";
    private static final String yfb_notify_url                      ="yfb_notify_url";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="yfb_sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[亿付宝]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：密钥-商户号" );
            throw new PayException("[亿付宝]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：密钥-商户号" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(yfb_appid, channelWrapper.getAPI_MEMBERID());
                put(yfb_total_fee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(yfb_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(yfb_out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(yfb_webname,"name");
                put(yfb_subject,"name");
                put(yfb_return_url,channelWrapper.getAPI_WEB_URL());
                put(yfb_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[亿付宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(yfb_appid));
        signSrc.append(channelWrapper.getAPI_KEY().split("-")[0]);
        signSrc.append(api_response_params.get(yfb_out_trade_no));
        signSrc.append(api_response_params.get(yfb_total_fee));
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[亿付宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[亿付宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[亿付宝]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//               log.error("[亿付宝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//               throw new PayException(resultStr);
//            }
//            //JSONObject resJson = JSONObject.parseObject(resultStr);
//            JSONObject resJson;
//            try {
//                resJson = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[亿付宝]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                String code_url = resJson.getString("codeimg");
//                if (handlerUtil.isWapOrApp(channelWrapper)) {
//                    result.put(JUMPURL, code_url);
//                }else{
//                    result.put(QRCONTEXT, code_url);
//                }
//            }else {
//                log.error("[亿付宝]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[亿付宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[亿付宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}