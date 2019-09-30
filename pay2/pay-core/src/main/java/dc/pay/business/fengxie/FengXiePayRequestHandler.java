package dc.pay.business.fengxie;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Oct 17, 2018
 */
@RequestPayHandler("FENGXIE")
public final class FengXiePayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FengXiePayRequestHandler.class);

    //字段名              字段说明          最大长度      是否必填      是否参与签名      备注
    //fx_merchant_id      商户号              30             是            是              商户签约时，本系统分配给商家的唯一标识。
    //fx_order_amount     交易金额            5              是            是              请求的价格(单位：元) 可以0.01元
    //fx_pay              支付方式            10             是            否              zfbsm支付宝   wxsm微信   qqsm QQ支付
    //fx_order_id         订单号              32             是            是              仅允许字母或数字类型,不超过35个字符，不要有中文
    //fx_notify_url       异步地址            200            是            是              异步接收支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
    //fx_back_url         同步地址            200            是            否              支付成功后跳转到的地址，不参与签名。
    //fx_desc             商品名称            50             否            否              utf-8编码
    //fx_attch            附加信息            50             否            否              原样返回，utf-8编码
    //fx_ip               支付用户ip地址      50             否            否              用户支付时设备的IP地址
    //fx_sign             数据签名            32             是                            通过签名算法计算得出的签名值。小写
    private static final String fx_merchant_id              ="fx_merchant_id";
    private static final String fx_order_amount             ="fx_order_amount";
    private static final String fx_pay                      ="fx_pay";
    private static final String fx_order_id                 ="fx_order_id";
    private static final String fx_notify_url               ="fx_notify_url";
    private static final String fx_back_url                 ="fx_back_url";
//    private static final String fx_desc                     ="fx_desc";
//    private static final String fx_attch                    ="fx_attch";
//    private static final String fx_ip                       ="fx_ip";
    private static final String fx_return_url               ="fx_return_url";
//    private static final String fx_sign                     ="fx_sign";

    //signature    数据签名    32    是    　
//    private static final String signature  ="fx_sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(fx_merchant_id, channelWrapper.getAPI_MEMBERID());
                put(fx_order_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(fx_pay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(fx_order_id,channelWrapper.getAPI_ORDER_ID());
                put(fx_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(fx_back_url,channelWrapper.getAPI_WEB_URL());
                //老板，我是技术支持
                //你直接post提交过来，多一个 fx_return_url = 100  参数就行， 响应给你json数据
                if (!handlerUtil.isWapOrApp(channelWrapper)) {
                    put(fx_return_url,"100");
                }
            }
        };
        log.debug("[风携]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(fx_merchant_id)).append("|");
        signSrc.append(api_response_params.get(fx_order_id)).append("|");
        signSrc.append(api_response_params.get(fx_order_amount)).append("|");
        signSrc.append(api_response_params.get(fx_notify_url)).append("|");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase()).toLowerCase();
        log.debug("[风携]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWapOrApp(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[风携]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
               log.error("[风携]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
               throw new PayException(resultStr);
            }
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[风携]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("fx_status") && "200".equalsIgnoreCase(resJson.getString("fx_status"))  && resJson.containsKey("fx_cashier_url") && StringUtils.isNotBlank(resJson.getString("fx_cashier_url"))) {
                Map<String, String> urlMap = handlerUtil.getUrlParams(resJson.getString("fx_cashier_url"));
                String qr_code_url = urlMap.get("qr_code_url");
                if (StringUtils.isBlank(qr_code_url)) {
                    log.error("[风携]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                try {
                    result.put(QRCONTEXT, URLDecoder.decode(qr_code_url, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    log.error("[风携]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }else {
                log.error("[风携]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[风携]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[风携]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}