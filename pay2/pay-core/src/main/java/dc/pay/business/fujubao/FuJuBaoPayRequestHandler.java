package dc.pay.business.fujubao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 20, 2018
 */
@RequestPayHandler("FUJUBAO")
public final class FuJuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FuJuBaoPayRequestHandler.class);

    //字段名            变量名                  类型            说明                              可空
    //基本参数      
    //商户号            merchantId            String            合作商户的商户号，由付聚宝公司分配      N
    //通知URL           notifyUrl             String            针对该交易的交易状态同步通知接收URL      N
    //返回URL           returnUrl             String            结果返回URL，仅适用于立即返回处理结果的接口。付聚宝处理完请求后，立即将处理结果返回给这个URL      N
    //签名              sign                  String            数据的加密校验字符串，目前支持使用MD5、RSA签名算法对待签名数据进行签名      N
    //业务参数      
    //交易号            outOrderId            String            合作伙伴交易号（确保在合作伙伴系统中唯一）      N
    //订单名称          subject               String            订单的标题      N
    //订单描述          body                  String            订单的具体描述      N
    //交易金额          transAmt              Double            交易的总金额，单位为元      N
    //默认网银          defaultBank           String            银行简码，请参见18.1银行简码      N
    //默认渠道          channel               String            银行渠道，默认为B2C（个人），B2B（企业）      N
    //卡类型            cardAttr              String            卡类型，默认1为借记卡，2为贷记卡      N
    private static final String merchantId              ="merchantId";
    private static final String notifyUrl               ="notifyUrl";
    private static final String returnUrl               ="returnUrl";
    private static final String outOrderId              ="outOrderId";
    private static final String subject                 ="subject";
    private static final String body                    ="body";
    private static final String transAmt                ="transAmt";
    private static final String defaultBank             ="defaultBank";
    private static final String channel                 ="channel";
    private static final String cardAttr                ="cardAttr";
    //扫码
    private static final String scanType                ="scanType";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantId, channelWrapper.getAPI_MEMBERID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(outOrderId,channelWrapper.getAPI_ORDER_ID());
                put(subject,"name");
                put(body,"name");
                put(transAmt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                if (handlerUtil.isWY(channelWrapper)) {
                    put(defaultBank,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(channel,"B2C");
                    put(cardAttr,"1");
                }else {
                    put(scanType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        log.debug("[付聚宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        String paramsStr = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(paramsStr.substring(0,paramsStr.length()-1),channelWrapper.getAPI_KEY(),"SHA1WithRSA");    // 签名
        } catch (Exception e) {
            log.error("[付聚宝]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        //String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[付聚宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
    	if (HandlerUtil.isWY(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }
//    	else{
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[付聚宝]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
//                throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
//            }
//            System.out.println("请求返回=========>"+resultStr);
//            JSONObject resJson = JSONObject.parseObject(resultStr);
//            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                String code_url = resJson.getString("codeimg");
//                result.put(QRCONTEXT, code_url);
//            }else {
//                log.error("[付聚宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[付聚宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[付聚宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}