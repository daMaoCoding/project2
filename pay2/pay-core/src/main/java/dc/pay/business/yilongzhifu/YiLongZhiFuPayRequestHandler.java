package dc.pay.business.yilongzhifu;

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
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("YILONGZHIFU")
public final class YiLongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiLongZhiFuPayRequestHandler.class);

     private static final String    out_trade_no = "out_trade_no";   //  String  Y  商户单号
     private static final String    goods_name = "goods_name";   //  String  Y  商品名称
     private static final String    total_amount = "total_amount";   //  String  Y  交易金额（单位：元）
     private static final String    channel_code = "channel_code";   //  String  Y  支付渠道编码，
     private static final String    notify_url = "notify_url";   //  String  Y  交易回调地址.
     private static final String    device = "device";   //  int  Y  设备 0手机 1电脑

     private static final String  appid ="appid";   //     String  Y  商户appid，对应商户编码
     private static final String  version ="version";   //     String  Y  版本号，固定值1.0.0
     private static final String  request_time ="request_time";   //     String  Y  请求时间格式为  yyyy-MM-dd HH:mm:ss
     private static final String  sign_type ="sign_type";   //     String  N  签名类型,MD5，不参与签名
     private static final String  sign ="sign";   //     String  Y  经过签名算法生成大写的
     private static final String  MD5 ="MD5";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)

            payParam.put(appid,channelWrapper.getAPI_MEMBERID());
            payParam.put(version,"1.0.0");
            payParam.put(request_time,DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
            payParam.put(sign_type,MD5);


            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(goods_name,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(channel_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(device,"1");
            if(HandlerUtil.isWapOrApp(channelWrapper)){
                payParam.put(device,"0");
            }

        }

        log.debug("[亿龙支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i)))  ||sign_type.equalsIgnoreCase(paramKeys.get(i).toString())     )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[亿龙支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
/*				
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getValueAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
				
*/				
				
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                resultStr= UnicodeUtil.unicodeToString(resultStr);
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr
                            && jsonResultStr.containsKey("biz_code") && "10010".equalsIgnoreCase(jsonResultStr.getString("biz_code"))
                            && jsonResultStr.containsKey("out_trade_no") && channelWrapper.getAPI_ORDER_ID().equalsIgnoreCase(jsonResultStr.getString("out_trade_no")) && jsonResultStr.containsKey("content")&& jsonResultStr.containsKey("type")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("content")) && "1".equalsIgnoreCase(jsonResultStr.getString("type"))){
                                result.put(JUMPURL, jsonResultStr.getString("content"));
                                payResultList.add(result);
                            }else if(StringUtils.isNotBlank(jsonResultStr.getString("content")) && "0".equalsIgnoreCase(jsonResultStr.getString("type"))){
                                result.put(HTMLCONTEXT, jsonResultStr.getString("content"));
                                payResultList.add(result);
                            }else { throw new PayException(resultStr);}
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[亿龙支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[亿龙支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[亿龙支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}