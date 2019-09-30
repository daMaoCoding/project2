package dc.pay.business.jiubak;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Sep 14, 2018
 */
@RequestPayHandler("JIUBAK")
public final class JiuBaKPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiuBaKPayRequestHandler.class);

    //参数名称                是否必填                   参数含义                              参数类型
    //partner_no                是                      商户号                                String
    //mch_order_no              是                      商户订单号( 32个字符以内 )            String( 32 )
    //body                      是                      商品名称( 50个字以内 )                String( 50 )
    //detail                    否                      商品详情( 255个字以内 )               String( 255 )
    //money                     是                      订单金额, 单位`分`                    Int
    //attach                    否                      透传字段( 附加参数 )                  String( 255 )
    //callback_url              否                      商户支付回调                          String( 255 )
    //time_stamp                是                      13位的时间戳，单位`毫秒`              Int( 13 )
    //code_type                 是                      支付方式，1=微信2=支付宝              Int
    //token                     是                      32位的验签参数，生成规则见1. 1        String( 32 )
    private static final String partner_no               ="partner_no";
    private static final String mch_order_no             ="mch_order_no";
    private static final String body                     ="body";
//    private static final String detail                   ="detail";
    private static final String money                    ="money";
    private static final String attach                   ="attach";
    private static final String callback_url             ="callback_url";
    private static final String time_stamp               ="time_stamp";
    private static final String code_type                ="code_type";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="token";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[98K]-[请求支付]-“商家密钥（私钥）”输入数据格式为【中间使用-分隔】：下单密钥-支付回调密钥" );
            throw new PayException("[98K]-[请求支付]-“商家密钥（私钥）”输入数据格式为【中间使用-分隔】：下单密钥-支付回调密钥" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner_no, channelWrapper.getAPI_MEMBERID());
                put(mch_order_no,channelWrapper.getAPI_ORDER_ID());
                put(body,"name");
//                put(detail,"name");
                put(money,  channelWrapper.getAPI_AMOUNT());
                put(attach,channelWrapper.getAPI_MEMBERID());
                put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(time_stamp,System.currentTimeMillis()+"");
                put(code_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[98K]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(mch_order_no+"=").append(api_response_params.get(mch_order_no)).append("&");
        signSrc.append(money+"=").append(api_response_params.get(money)).append("&");
        signSrc.append(partner_no+"=").append(api_response_params.get(partner_no)).append("&");
        signSrc.append(time_stamp+"=").append(api_response_params.get(time_stamp)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY().split("-")[0]);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[98K]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[98K]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[98K]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[98K]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        JSONObject resJson = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("code") && "0".equalsIgnoreCase(resJson.getString("code"))  && 
                (resJson.containsKey("code_img_url") && StringUtils.isNotBlank(resJson.getString("code_img_url")) || 
                 resJson.containsKey("code_link") && StringUtils.isNotBlank(resJson.getString("code_link")))
                ) {
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, resJson.getString("code_link"));
            }else {
                result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(resJson.getString("code_img_url")));
            }
        }else {
            log.error("[98K]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[98K]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[98K]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}