package dc.pay.business.ezhifu;

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
 * @author Cobby
 * Mar 13, 2019
 */
@RequestPayHandler("EZHIFU")
public final class EZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(EZhiFuPayRequestHandler.class);
    
    private static final String service          ="service";    //    支付类型
    private static final String merchantNo       ="merchantNo"; //    商户编号            是
    private static final String orderNo          ="orderNo";    //    商户订单号          是
    private static final String amount           ="amount";     //    订单金额   单位:元   是
    private static final String bankCode         ="bankCode";   //    银行代码            否
    private static final String productName      ="productName";//    商品名称            是
    private static final String notifyUrl        ="notifyUrl";  //    服务器异步通知地址    是
    private static final String returnUrl        ="returnUrl";  //    支付完成后返回地址    否
    private static final String remark           ="remark";     //    备注    商家备注,回调是原样返回    否    X(50)    9

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(merchantNo, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                if (HandlerUtil.isWY(channelWrapper)){
                    put(service,"01");
                    put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(productName,"name");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(remark,"pay");
            }
        };
        log.debug("[E支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
//      service=01&merchantNo=35125052&orderNo=20180101235959&amount=10.12&productName=apple&
//      notifyUrl=https://www.baidu.com&returnUrl=https://www.baidu.com&remark=test&FE931EB45AAA009319E87A7B30DCE062
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(service+"=").append(api_response_params.get(service)).append("&");
        signSrc.append(merchantNo+"=").append(api_response_params.get(merchantNo)).append("&");
        signSrc.append(orderNo+"=").append(api_response_params.get(orderNo)).append("&");
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        //如果是网银
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WEB_.name())) {
            signSrc.append(bankCode+"=").append(api_response_params.get(bankCode)).append("&");
        }
        signSrc.append(productName+"=").append(api_response_params.get(productName)).append("&");
        signSrc.append(notifyUrl+"=").append(api_response_params.get(notifyUrl)).append("&");
        signSrc.append(returnUrl+"=").append(api_response_params.get(returnUrl)).append("&");
        signSrc.append(remark+"=").append(api_response_params.get(remark)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[E支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
            
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            
        } catch (Exception e) {
            log.error("[E支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[E支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[E支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}