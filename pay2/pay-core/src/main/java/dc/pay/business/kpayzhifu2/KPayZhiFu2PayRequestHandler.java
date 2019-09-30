package dc.pay.business.kpayzhifu2;

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
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 24, 2019
 */
@RequestPayHandler("KPAYZHIFU2")
public final class KPayZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KPayZhiFu2PayRequestHandler.class);

    private static final String version     = "version";    //    版本号    string(3)    Y    默认填写2.0
    private static final String charset     = "charset";    //    字符集    string(10)    Y    默认填写 UTF-8
    private static final String spid        = "spid";       //    收款方商户号    string(10)    Y    k-pay 分配的商户号
    private static final String spbillno    = "spbillno";   //    商户单号    string(32)    Y    商户系统内部的订单号
    private static final String notifyUrl   = "notifyUrl";  //    后台通知地址    string(255)    Y    接收k-pay 通知的URL， 能通过互联网访问该地址
    private static final String productName = "productName";//    商品名称    string(255)    Y    商品名称
    private static final String signType    = "signType";   //    签名类型    string(10)    Y    目前只支持MD5
    private static final String tranAmt     = "tranAmt";    //    交易金额    string(18)    Y    交易金额，单位为分
    //    private static final String userId         = "userId";    //    商户系统用户唯一标识，传则会记住用户支付银行卡
//    private static final String productDesc    ="productDesc";//    商品描述    string(255)    N    商品描述
//    private static final String attach         ="attach";     //    保留字段    string(255)    N    原样返回
//    private static final String backUrl        ="backUrl";    //    前台页面通知地址    string(255)    N    支付结束后显示的合作商户系统页面地址
    private static final String key         = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "2.0");
                put(charset, "UTF-8");
                put(spid, channelWrapper.getAPI_MEMBERID());
                put(spbillno, channelWrapper.getAPI_ORDER_ID());
                put(tranAmt, channelWrapper.getAPI_AMOUNT());
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(productName, channelWrapper.getAPI_ORDER_ID());
                put(signType, "MD5");
            }
        };
        log.debug("[Kpay支付2]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signType.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[Kpay支付2]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String                  xml         = XmlUtil.map2Xml(payParam, false, "xml", false);
            HashMap<String, String> payParamMap = Maps.newHashMap();
            payParamMap.put("req_data", xml);
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParamMap).toString());

        } catch (Exception e) {
            log.error("[Kpay支付22]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[Kpay支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[Kpay支付2]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}