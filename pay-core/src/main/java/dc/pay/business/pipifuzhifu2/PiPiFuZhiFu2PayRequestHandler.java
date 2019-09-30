package dc.pay.business.pipifuzhifu2;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 11, 2019
 */
@RequestPayHandler("PIPIFU2ZHIFU")
public final class PiPiFuZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PiPiFuZhiFu2PayRequestHandler.class);
    
    private static final String zp_mer_id           ="zp_mer_id";       //    商户号     是    是    唯一号，由真皮支付(个码)提供。
    private static final String zp_order_amount     ="zp_order_amount"; //    交易金额   是    是    请求的价格(单位：元) 可以0.01元
    private static final String zp_pay_type         ="zp_pay_type";     //    支付方式   是    否    请求类型 【支付宝：alipay】 【云闪付：ysf】 PC和H5参数相同
    private static final String zp_order_id         ="zp_order_id";     //    订单号     是    是    仅允许字母或数字类型,不超过35个字符，不要有中文
    private static final String zp_notify_url       ="zp_notify_url";   //    异步地址   是    是    异步接收支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
    private static final String zp_back_url         ="zp_back_url";     //    同步地址   是    否    支付成功后跳转到的地址，不参与签名。
//  private static final String zp_desc             ="zp_desc";         //    商品名称   否    否    utf-8编码
//  private static final String zp_attch            ="zp_attch";        //    附加信息   否    否    原样返回，utf-8编码
//  private static final String zp_ip               ="zp_ip";           //    支付用户ip 否    否    用户支付时设备的IP地址
//  private static final String zp_sign             ="zp_sign";         //    数据签名   是    否    通过签名算法计算得出的签名值。小写
//  private static final String zp_format           ="zp_format";       //    格式化返回  否   否     json: 返回json 默认: 空


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(zp_mer_id, channelWrapper.getAPI_MEMBERID());
                put(zp_order_amount, HandlerUtil.getYuan( channelWrapper.getAPI_AMOUNT()));
                put(zp_pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(zp_order_id,channelWrapper.getAPI_ORDER_ID());
                put(zp_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(zp_back_url,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[皮皮付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //【md5(商户号|异步通知地址|支付金额|商户订单号|商户秘钥)】
        String paramsStr = String.format("%s|%s|%s|%s|%s",
                api_response_params.get(zp_mer_id),
                api_response_params.get(zp_notify_url),
                api_response_params.get(zp_order_amount),
                api_response_params.get(zp_order_id),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[皮皮付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
            
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            JSONObject jsonObject;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[皮皮付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
////            "HtmlContext": "{\"code\":200,\"message\":\"支付宝扫码支付\",\"data\":\"http://www.awq331.cn/alipay/?id=201683\"}"
//            if (null != jsonObject && jsonObject.containsKey("code") && "200".equalsIgnoreCase(jsonObject.getString("code"))
//                    && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
//                String code_url = jsonObject.getString("data");
//                result.put(  JUMPURL , code_url);
//            }else {
//                log.error("[皮皮付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }

        } catch (Exception e) {
            log.error("[皮皮付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[皮皮付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[皮皮付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}