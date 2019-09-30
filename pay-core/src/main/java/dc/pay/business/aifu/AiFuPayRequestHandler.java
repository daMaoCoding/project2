package dc.pay.business.aifu;

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

@RequestPayHandler("AIFU")
public final class AiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AiFuPayRequestHandler.class);

     private static final String      version = "version";// 	接口版本	String(2)	必选	固定值v1
     private static final String      merchant_no = "merchant_no";// 	商户号	String(12)	必选	商户签约时AI分配的唯一商家编号
     private static final String      order_no = "order_no";// 	商户订单号	String(30)	必选	由商户平台生成的交易订单号，请确保其在商户平台的唯一性，其组成仅限于字母和数字
     private static final String      goods_name = "goods_name";// 	商品名称	String(128)	必选	使用base64进行编码（UTF-8编码）
     private static final String      order_amount = "order_amount";// 	订单金额	number	必选	以元为单位，精确到小数点后两位
     private static final String      backend_url = "backend_url";// 	接受AI后台异步订单状态通知的地址	String（128）	必选	该订单在AI支付平台的状态更新后，AI会主动将订单状态推送到该地址，请确保该地址可访问
     private static final String      pay_mode = "pay_mode";// 	支付模式	String(2)	必选	12：H5支付模式
     private static final String      bank_code = "bank_code";// 	银行编号	String(25)	必选	WECHATWAP：微信WAP
     private static final String      card_type = "card_type";// 	允许支付的卡类型	String（1）	必选	0:仅允许使用借记卡支付1:仅允许使用信用卡支付 2:借记卡和信用卡都能对订单进行支付
     private static final String      sign = "sign";// 	签名数据	String(32)	必选	以上请求参数再加上接口秘钥通过md5加密生成的签名串，参数顺序按照表格中从上到下的顺序，最后加上接口秘钥，本参数不参与签名
     private static final String      frontend_url = "frontend_url";
     private static final String      reserve = "reserve";




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"v1");
            payParam.put(merchant_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(goods_name,channelWrapper.getAPI_ORDER_ID());
            payParam.put(order_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(backend_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            if(HandlerUtil.isWY(channelWrapper)){
             payParam.put(pay_mode,"01");
            }else if(HandlerUtil.isWapOrApp(channelWrapper)){
                payParam.put(pay_mode, "12");
            }else{
             payParam.put(pay_mode,"09");
            }
            payParam.put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(card_type,"2");
            payParam.put(frontend_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(reserve,channelWrapper.getAPI_ORDER_ID());
        }

        log.debug("[爱付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //version=xxx&merchant_no=xxx&order_no=xxx&goods_name=xxx&order_amount=xxx&backend_url=xxx&frontend_url=xxx&reserve=xxx&pay_mode=xxx&bank_code=xxx&card_type=xxx&key=xxx
        // =xxx&=xxx&=xxx&=xxx&=xxx&=xxx&=xxx&=xxx&=xxx&=xxx&=xxx

        String paramsStr = String.format("version=%s&merchant_no=%s&order_no=%s&goods_name=%s&order_amount=%s&backend_url=%s&frontend_url=%s&reserve=%s&pay_mode=%s&bank_code=%s&card_type=%s&key=%s",
                params.get(version),
                params.get(merchant_no),
                params.get(order_no),
                params.get(goods_name),
                params.get(order_amount),
                params.get(backend_url),
                params.get(frontend_url),
                params.get(reserve),
                params.get(pay_mode),
                params.get(bank_code),
                params.get(card_type),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[爱付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'", "method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("result_code") && "00".equalsIgnoreCase(jsonResultStr.getString("result_code")) && jsonResultStr.containsKey("code_url")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("code_url"))){
                                result.put(QRCONTEXT, jsonResultStr.getString("code_url"));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[爱付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[爱付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[爱付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}