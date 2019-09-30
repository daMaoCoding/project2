package dc.pay.business.zhongyifu;

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
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("ZHONGYIFU")
public final class ZhongYiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhongYiFuPayRequestHandler.class);

    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";


    private static final  String  src_code	     = "src_code";        //是	商户唯一标识
     private static final String  sign	         = "sign";            //是	签名
     private static final String  out_trade_no	 = "out_trade_no";    //是	接入方交易订单号
     private static final String  total_fee	     = "total_fee";       //是	订单总金额
     private static final String  time_start	 = "time_start";      //是	发起交易的时间
     private static final String  goods_name	 = "goods_name";      //是	商品名称
     private static final String  trade_type	 = "trade_type";      //是	交易类型，网关支付：80103
     private static final String  finish_url	 = "finish_url";      //是	支付完成页面的url，有效性根据实际通道而定
     private static final String  mchid	         = "mchid";           //是	商户号

     private static final String  extend	     = "extend";          //是	扩展域，此字段是一个json格式，具体参数如下表
     private static final String  bankName	     = "bankName";        //是	银行名称总行名称，值范围：北京农村商业银行, 农业银行, 华夏银行, 交通银行, 广发银行, 邮政储蓄银行, 中国银行, 兴业银行, 中信银行, 招商银行, 银联通道, 光大银行, 建设银行, 平安银行, 浦发银行, 北京银行, 民生银行, 上海银行, 工商银行
     private static final String  cardType	     = "cardType";        //是	卡类型，目前只支持借记卡，取值“借记卡”

     private static final String  pay_params	 = "pay_params";      //跳转URL





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误：mchid & src_code");
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(src_code,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(out_trade_no, channelWrapper.getAPI_ORDER_ID());
                put(total_fee, channelWrapper.getAPI_AMOUNT());
                put(time_start, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(goods_name, "PAY");
                put(trade_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(finish_url, HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()));

                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_")){
                    HashMap<String, String> extendMap = Maps.newHashMap();
                    extendMap.put(bankName,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                    extendMap.put(cardType,"借记卡");
                    put(extend, JSON.toJSONString(extendMap));
                }
            }
        };
        log.debug("[众易付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());
        log.debug("[众易付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            JSONObject resJson = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
            String respcd = resJson.getString("respcd");
            String respmsg = resJson.getString("respmsg");
            String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();

            if (!respcd.equalsIgnoreCase("0000")) {
                log.error("[众易付]3.发送支付请求，及获取支付请求结果：" + resJson.toJSONString() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resJson.toJSONString());
            }

                if (api_channel_bank_name.endsWith("WX_SM") || api_channel_bank_name.endsWith("ZFB_SM")  ||api_channel_bank_name.endsWith("QQ_SM") ) {
                        String qrContent = resJson.getJSONObject("data").getString("pay_params");
                        HashMap<String, String> result = Maps.newHashMap();
                        result.put(QRCONTEXT, qrContent);
                        result.put(PARSEHTML, resJson.toJSONString());
                        payResultList.add(result);
                } else if (api_channel_bank_name.contains("_WY_")) {

                    String jumpURL=resJson.getJSONObject("data").getString("pay_params");
                    HashMap<String, String> result = Maps.newHashMap();
                   // result.put(QRCONTEXT, wxQRContext);
                    result.put(PARSEHTML, resJson.toJSONString());
                    result.put(JUMPURL, jumpURL);
                    payResultList.add(result);
                    if (StringUtils.isBlank(jumpURL)) {
                        throw new PayException(SERVER_MSG.REQUEST_PAY_PARSE_RESULT_ERROR);
                    }
                } else {
                    String body = HandlerUtil.replaceBlank("");
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(HTMLCONTEXT, body);
                    payResultList.add(result);
                }




        } catch (Exception e) {
            log.error("[众易付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[众易付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(JUMPURL)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayHtmlContent(null);
                    requestPayResult.setRequestPayJumpToUrl(resultMap.get(JUMPURL));
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                }
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[众易付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}