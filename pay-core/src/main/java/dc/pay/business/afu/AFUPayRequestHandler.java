package dc.pay.business.afu;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
 
@RequestPayHandler("AFU")
public final class AFUPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AFUPayRequestHandler.class);

    // payKey          | 商户支付Key                                  | String | 否    | 32   |
    private static final String payKey  ="payKey";
    // orderPrice      | 订单金额，单位：元<br>保留小数点后两位       | String | 否    | 12   |
    private static final String orderPrice  ="orderPrice";
    // outTradeNo      | 商户支付订单号                               | String | 否    | 30   |
    private static final String outTradeNo  ="outTradeNo";
    // productType     | 产品类型<br>==50000101== B2C T1支付<br>==50000103== B2C T0支付 | String | 否    | 8    |
    private static final String productType  ="productType";
    // orderTime       | 下单时间，格式<br>yyyyMMddHHmmss             | String | 否    | 14   |
    private static final String orderTime  ="orderTime";
    // productName     | 支付产品名称                                 | String | 否    | 200  |
    private static final String productName  ="productName";
    // orderIp         | 下单IP                                       | String | 否    | 15   |
    private static final String orderIp  ="orderIp";
    // returnUrl       | 页面通知地址                                 | String | 否    | 300  |
    private static final String returnUrl  ="returnUrl";
    // notifyUrl       | 后台异步通知地址                             | String | 否    | 300  |
    private static final String notifyUrl  ="notifyUrl";
    // remark          | 备注                                         | String | 是    | 200  |
    private static final String remark  ="remark";
    // mobile          | 移动端（当为手机端时此参数不为空 值为 1）    | String | 是    | 10   |
    private static final String mobile  ="mobile";
    private static final String paySecret  ="paySecret";

    /**
     * 封装第三方所需要的参数
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_CHANNEL_BANK_NAME_FlAG = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
        if (null == api_CHANNEL_BANK_NAME_FlAG || !api_CHANNEL_BANK_NAME_FlAG.contains(",") || api_CHANNEL_BANK_NAME_FlAG.split(",").length != 2) {
            log.error("[A付]-[请求支付]-1.1.组装请求参数格式：通信类型,支付渠道编号,简码。如：网关支付,银联,工商银行==>200002,30,ICBCD" );
            throw new PayException("[A付]-[请求支付]-1.1.组装请求参数格式：通信类型,支付渠道编号,简码。如：网关支付,银联,工商银行==>200002,30,ICBCD" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(payKey, channelWrapper.getAPI_MEMBERID());
                put(productType,api_CHANNEL_BANK_NAME_FlAG.split(",")[0]);
                put(productName,"productName");
                put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(orderPrice,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderIp,channelWrapper.getAPI_Client_IP());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(remark,"remark");
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[A付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     */
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        if (StringUtils.isNotBlank(api_response_params.get(mobile))) {
            signSrc.append(mobile+"=").append(api_response_params.get(mobile)).append("&");
        }
        signSrc.append(notifyUrl+"=").append(api_response_params.get(notifyUrl)).append("&");
        signSrc.append(orderIp+"=").append(api_response_params.get(orderIp)).append("&");
        signSrc.append(orderPrice+"=").append(api_response_params.get(orderPrice)).append("&");
        signSrc.append(orderTime+"=").append(api_response_params.get(orderTime)).append("&");
        signSrc.append(outTradeNo+"=").append(api_response_params.get(outTradeNo)).append("&");
        signSrc.append(payKey+"=").append(api_response_params.get(payKey)).append("&");
        signSrc.append(productName+"=").append(api_response_params.get(productName)).append("&");
        signSrc.append(productType+"=").append(api_response_params.get(productType)).append("&");
        signSrc.append(remark+"=").append(api_response_params.get(remark)).append("&");
        signSrc.append(returnUrl+"=").append(api_response_params.get(returnUrl)).append("&");
        signSrc.append(paySecret+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[A付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5)+"，参数："+JSON.toJSONString(paramsStr));
        return signMd5;
    }

    /**
     * 生成返回给RequestPayResult对象detail字段的值
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String,String> result = Maps.newHashMap();
        if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //保存第三方返回值
            result.put(HTMLCONTEXT, htmlContent.toString());
        }else{
            String tmpStr = null;
            try {
                tmpStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                if (null == tmpStr || StringUtils.isBlank(tmpStr)) {
                    log.error("[A付]3.1.发送支付请求，获取支付请求返回值异常:返回空");
                    throw new PayException("返回空");
                }
            } catch (Exception e) {
                log.error("[A付]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                throw new PayException(e.getMessage(),e);
            }
            JSONObject jsonObject = JSONObject.parseObject(tmpStr);
            //响应码为‘0000’时非空
            if (null == jsonObject || (jsonObject.containsKey("resultCode") && !"0000".equals(jsonObject.getString("resultCode")))) {
                 log.error("[A付]3.2.发送支付请求，获取支付请求返回值异常:"+tmpStr);
                 throw new PayException(tmpStr);
            }
            //按不同的请求接口，向不同的属性设置值
            if(handlerUtil.isWapOrApp(channelWrapper)) {
                if (jsonObject.getString("payMessage").contains("form")) {
                    result.put(HTMLCONTEXT, jsonObject.getString("payMessage"));
                }else {
                    result.put(JUMPURL, jsonObject.getString("payMessage"));
                }
            }else{
                result.put(QRCONTEXT, jsonObject.getString("payMessage"));
            }
            result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
        }
        payResultList.add(result);
        log.debug("[A付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     */
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
        log.debug("[A付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}