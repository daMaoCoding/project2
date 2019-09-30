package dc.pay.business.juxinzhifu;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("JUXINZHIFU")
public final class JuXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuXinZhiFuPayRequestHandler.class);

    private static final String  version           = "version";     //    版本号    String    否    默认：V1
    private static final String  mer_no            = "mer_no";      //    商户号    String    是    平台分配的唯一商户编号
    private static final String  mer_order_no      = "mer_order_no";//   商户订单号    String    是    商户必须保证唯一
    private static final String  ccy_no            = "ccy_no";      //    币种    String    否    CNY:人民币 USD:美元 默认CNY
    private static final String  order_amount      = "order_amount";//    下单金额    Number    是    分为单位，整数
    private static final String  busi_code         = "busi_code";   //    业务编码    String    是    详情见：附件业务编码
    private static final String  goods             = "goods";       //    商品名称    String    是    商品名称
    private static final String  reserver          = "reserver";    //    保留信息    String    否    原样返回
    private static final String  bg_url            = "bg_url";      //    异步通知地址    String    是    支付成功后，平台主动通知商家系统，商家系统必须指定接收通知的地址。
    private static final String  bankCode          = "bankCode";    //    银行编码     String    否    网关业务必填
    private static final String  page_url          = "page_url";    //    页面跳转地址    String    否    WAP支付，H5支付，网关支付必填   支付成功后，页面跳转至此地址
    private static final String  key               = "key=";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"V1");
            payParam.put(mer_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(ccy_no,"CNY");
            payParam.put(order_amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(busi_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(goods,channelWrapper.getAPI_ORDER_ID());
            payParam.put(reserver,channelWrapper.getAPI_ORDER_ID());
            payParam.put(bg_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(page_url,channelWrapper.getAPI_WEB_URL());
            if (HandlerUtil.isWY(channelWrapper)){
                payParam.put(busi_code,"100301");
                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());

            }
        }

        log.debug("[聚信支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚信支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(),JSON.toJSONString(payParam));
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[聚信支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (null != jsonObject && jsonObject.containsKey("status") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("status")) 
                    && jsonObject.containsKey("order_data") && StringUtils.isNotBlank(jsonObject.getString("order_data"))) {
                String code_url = jsonObject.getString("order_data");
                result.put( JUMPURL, code_url);
            }else {
                log.error("[聚信支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        } catch (Exception e) {
             log.error("[聚信支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        payResultList.add(result);
        log.debug("[聚信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[聚信支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}