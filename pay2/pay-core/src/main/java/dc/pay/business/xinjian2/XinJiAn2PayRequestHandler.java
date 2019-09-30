package dc.pay.business.xinjian2;

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
 * Jun 29, 2019
 */
@RequestPayHandler("XINJIAN2")
public final class XinJiAn2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinJiAn2PayRequestHandler.class);

    //Name    Description Type                             说明
    //pid 商户id <必填>   integer
    private static final String pid                   ="pid";
    //orderid 订单id <必填>   string
    private static final String orderid               ="orderid";
    //pt_type 7：支付宝,，1：微信，6:银联<必填>    Integer                请使用上面表单地址提交
    private static final String pt_type               ="pt_type";
    //name    商品名称 <必填>   String                  不要输入中文
    private static final String name                  ="name";
    //money   金额 <必填> decimal number          保留2位小数（1.00）
    private static final String money                 ="money";
    //notify_url  支付结果通知地址 <必填>   string
    private static final String notify_url            ="notify_url";
    //request_url 支付成功跳转地址（默认回支付页）<选填>    string
//    private static final String request_url            ="request_url";
    //sign    大写MD5加密<必填> string
//    private static final String sign            ="sign";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pid, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(pt_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(name,"name");
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[新吉安2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY()).append("&");
        signSrc.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc.append(pt_type+"=").append(api_response_params.get(pt_type)).append("&");
        signSrc.append(name+"=").append(api_response_params.get(name)).append("&");
        signSrc.append(money+"=").append(api_response_params.get(money)).append("&");
        signSrc.append(notify_url+"=").append(api_response_params.get(notify_url));
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新吉安2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (handlerUtil.isWapOrApp(channelWrapper)) {
        if (handlerUtil.isYLSM(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else {
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[新吉安2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[新吉安2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新吉安2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("Data") && StringUtils.isNotBlank(resJson.getString("Data"))) {
                JSONObject resJson2;
                try {
                    resJson2 = JSONObject.parseObject(resJson.getString("Data"));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[新吉安2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != resJson2 && resJson2.containsKey("error") && "true".equalsIgnoreCase(resJson2.getString("error"))  && resJson2.containsKey("qr_code") && StringUtils.isNotBlank(resJson2.getString("qr_code"))) {
                    String code_url = resJson2.getString("url");
                    if (handlerUtil.isWapOrApp(channelWrapper)) {
                        result.put(JUMPURL, code_url);
                    }else {
                        result.put(QRCONTEXT, code_url);
                    }
                }else {
                    log.error("[新吉安2]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }else {
                log.error("[新吉安2]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新吉安2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新吉安2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}