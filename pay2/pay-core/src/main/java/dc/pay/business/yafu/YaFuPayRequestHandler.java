package dc.pay.business.yafu;

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

/**
 * ************************
 *
 * @author tony 3556239829
 */
@RequestPayHandler("YAFU")
public class YaFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(YaFuPayRequestHandler.class);
    private  static  final  String backUrl	    =  "backUrl";
    private  static  final  String bankCode	    =  "bankCode";
    private  static  final  String goodsName	=  "goodsName";
    private static final String  version	      =  "version";
    private static final String  code	          =  "code";
    private static final String  msg	          =  "msg";
    private static final String  consumerNo	      =  "consumerNo";
    private static final String  merOrderNo	      =  "merOrderNo";
    private static final String  orderNo	      =  "orderNo";
    private static final String  payType	      =  "payType";
    private static final String  contentType	  =  "contentType";
    private static final String  busContent	      =  "busContent";
    private static final String  orderStatus	  =  "orderStatus";
    private static final String  transAmt	      =  "transAmt";
    private static final String  sign	          =  "sign";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newTreeMap();
            payParam.put(version,"3.0");
            payParam.put(consumerNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(merOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(transAmt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(backUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());

            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_")){
                payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            }else{
                payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
            payParam.put(goodsName,"goods");
        log.debug("[雅付支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append("key="+channelWrapper.getAPI_KEY());//"key="+
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        log.debug("[雅付支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return  pay_md5sign;
    }


    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try{
            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_") ){
                HashMap<String, String> result = Maps.newHashMap();
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
                payResultList.add(result);
            } else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_")||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAPAPP_")){
                String getURLForwapAPP = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                HashMap<String, String> result = Maps.newHashMap();
                result.put(JUMPURL, getURLForwapAPP);
                payResultList.add(result);
            }else{
                    //String urlWithParam = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam);
                    //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, null);
                    String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                    JSONObject resJson = JSONObject.parseObject(resultStr);
                     if(null!=resJson && resJson.containsKey("code") &&  resJson.getString("code").equalsIgnoreCase("000000") && resJson.containsKey(busContent) && StringUtils.isNotBlank(resJson.getString(busContent))   && !ValidateUtil.isHaveChinese(resJson.getString(busContent)) ){
                         HashMap<String, String> resultMap = Maps.newHashMap();
                         resultMap.put(QRCONTEXT,resJson.getString(busContent));
                         resultMap.put(PARSEHTML,resultStr);
                         payResultList.add(resultMap);

                     }else{
                         log.error("[雅付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：" + resultStr);
                         throw new PayException(resultStr);
                     }

            }
        } catch (Exception e) {
                throw new PayException(e.getMessage(),e);
        }
        log.debug("[雅付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> result) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();

        if(!result.isEmpty() && result.size()==1){
            Map<String, String> lastResult = result.get(0);
            requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
            requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
            requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            if(lastResult.containsKey(JUMPURL)){
                requestPayResult.setRequestPayJumpToUrl(lastResult.get(JUMPURL));
            }else if (lastResult.containsKey(QRCONTEXT)) {
                requestPayResult.setRequestPayQRcodeContent(lastResult.get(QRCONTEXT));
            }else  if(lastResult.containsKey(HTMLCONTEXT)){
                requestPayResult.setRequestPayHtmlContent(lastResult.get(HTMLCONTEXT));
            }

        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }

        if(ValidateUtil.requestesultValdata(requestPayResult)){
            requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }

        log.debug("[雅付支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}