package dc.pay.business.gongniu;

import java.util.ArrayList;
import java.util.Base64;
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
 * Nov 8, 2018
 */
@RequestPayHandler("GONGNIU")
public final class GongNiuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GongNiuPayRequestHandler.class);

    //参数                      格式                  必填          说明
    //merchant_code             String(10)             √            参数名称：商家号 商户签约时，公牛支付分配给商家的唯一身份标识 例如：100000001或者100000002
    //merchant_order_no         String(25)             √            参数名称：商户订单号 由商户网站或APP生成的订单号，支付结果返回时会回传该参数
    //merchant_goods            String(200)            √            参数名称：商品名称 发起支付相关的商品名称或商品代码等
    //merchant_amount           String(10)             √            参数名称：支付金额 发起支付累计金额，如10.01，货币单位：人民币
    //gateway                   String(20)             √            参数名称：支付网关/通道 取值： alipay（支付宝）/ alipay_wap（支付宝WAP）
    //urlcall                   String(100)            √            参数名称：服务器异步通知地址 支付成功后，公牛支付会主动发送通知给商户，商户必须指定此通知地址
    //urlback                   String(100)            √            参数名称：返回网址  支付成功后，通过页面跳转的方式跳转到商家网站
    //merchant_sign             String(200)            ×            参数名称：数据加密印鉴 生成规则：base64_encode(md5('merchant_code='.$merchant_code.'&merchant_order_no='.$merchant_order_no.'&merchant_goods='.$merchant_goods.'&merchant_amount='.$merchant_amount.'&merchant_md5='.$merchant_md5)) 生成规则中的“+”表示字符连接，两个参数中间实际不加任何额外字符。merchant_md5是公牛支付分配给商户的商户MD5，请不要泄露
    private static final String merchant_code                                          ="merchant_code";
    private static final String merchant_order_no                                      ="merchant_order_no";
    private static final String merchant_goods                                         ="merchant_goods";
    private static final String merchant_amount                                        ="merchant_amount";
    private static final String gateway                                                ="gateway";
    private static final String urlcall                                                ="urlcall";
    private static final String urlback                                                ="urlback";
//    private static final String merchant_sign                                          ="merchant_sign";

    private static final String key        ="merchant_md5";
    //signature    数据签名    32    是    　
//    private static final String signature  ="merchant_sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_code, channelWrapper.getAPI_MEMBERID());
                put(merchant_order_no,channelWrapper.getAPI_ORDER_ID());
                put(merchant_goods,"name");
                put(merchant_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(gateway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(urlcall,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(urlback,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[公牛]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merchant_code+"=").append(api_response_params.get(merchant_code)).append("&");
        signSrc.append(merchant_order_no+"=").append(api_response_params.get(merchant_order_no)).append("&");
        signSrc.append(merchant_goods+"=").append(api_response_params.get(merchant_goods)).append("&");
        signSrc.append(merchant_amount+"=").append(api_response_params.get(merchant_amount)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = new String(Base64.getEncoder().encode(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().getBytes()));
        log.debug("[公牛]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (1 == 1 || HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
////            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[公牛]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[公牛]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            System.out.println("请求返回=========>"+resultStr);
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//                log.error("[公牛]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //JSONObject resJson = JSONObject.parseObject(resultStr);
//            JSONObject resJson;
//            try {
//                resJson = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[公牛]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                String code_url = resJson.getString("codeimg");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[公牛]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[公牛]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[公牛]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}