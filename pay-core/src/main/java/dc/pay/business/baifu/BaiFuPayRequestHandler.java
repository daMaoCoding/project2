package dc.pay.business.baifu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("BAIFU")
public final class BaiFuPayRequestHandler extends PayRequestHandler {
     private static final Logger log = LoggerFactory.getLogger(BaiFuPayRequestHandler.class);
     private static final String       merchantNo	  ="merchantNo";                   //16	商户号（我方佰富提供）
     private static final String       netwayCode	  ="netwayCode";                   //16	扫码支付网关:（二维码地址生成二维码图片进行扫码支付）
     private static final String       randomNum	  ="randomNum";                    //4	随机数：唯一性
     private static final String       orderNum	  ="orderNum";                         //20	订单号：唯一性
     private static final String       payAmount	  ="payAmount";                    //14	金额（单位：分）   例：1元=100
     private static final String       goodsName	  ="goodsName";                    //20	商品信息(字符长度限定20字符，禁止出现标点符号，特殊符号，可能影响商户交易成功率)
     private static final String       callBackUrl	  ="callBackUrl";                  //128	交易异步通知地址：我方通知商户接收交易结果地址
     private static final String       frontBackUrl	  ="frontBackUrl";                 //128	回显地址：若无，则与异步通知地址相同即可
     private static final String       requestIP	  ="requestIP";                    //15	商户真实交易请求IP：(格式：112.224.69.132)：禁止使用127.0.0.1，通道校验非商户正式ip，则影响商户成功率
     private static final String       sign	  ="sign";                                 //32	签名（字母大写）
     private static final String       paramData	  ="paramData";

     private static final String       CodeUrl	  ="CodeUrl";
     private static final String       resultCode	  ="resultCode";
     private static final String       ISO88591	  ="ISO-8859-1";
     private static final String       UTF8	  ="UTF-8";
     private static final String       doubleZero	  ="00";
    private static final String scanType = "scanType";





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchantNo, channelWrapper.getAPI_MEMBERID() );
            payParam.put(netwayCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(randomNum,HandlerUtil.getRandomStrStartWithDate(5) );
            payParam.put(orderNum, channelWrapper.getAPI_ORDER_ID());
            payParam.put(payAmount,channelWrapper.getAPI_AMOUNT() );
            payParam.put(goodsName,goodsName );
            payParam.put(callBackUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(frontBackUrl, channelWrapper.getAPI_WEB_URL());
            payParam.put(requestIP, channelWrapper.getAPI_Client_IP());
            if(HandlerUtil.isFS(channelWrapper)){
                payParam.put(scanType,"Page");
            }
        }
        log.debug("[佰富]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        String buildJsonParam = BaiFuPayUtil.buildJsonParam(params,sign).concat(channelWrapper.getAPI_KEY());
        pay_md5sign = HandlerUtil.getMD5UpperCase(buildJsonParam);
        log.debug("[佰富]-[请求支付]-2.生成加密URL签名完成：{}",pay_md5sign);
        return pay_md5sign;
    }




    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        JSONObject jsonObject = JSON.parseObject(BaiFuPayUtil.buildJsonParam(payParam,sign));
        jsonObject.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
         payParam = Maps.newHashMap();
         payParam.put(paramData, jsonObject.toJSONString());

        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr="";
        try {
            if (1==2  ) { // HandlerUtil.isWY(channelWrapper) ||
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString() );//.replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                resultStr = new String(resultStr.getBytes(ISO88591), UTF8);
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                if(null!=jsonResultStr && jsonResultStr.containsKey(resultCode) && doubleZero.equalsIgnoreCase(jsonResultStr.getString(resultCode)) && jsonResultStr.containsKey(CodeUrl)){
                    if(StringUtils.isNotBlank(jsonResultStr.getString(CodeUrl))){
                        if(HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||HandlerUtil.isFS(channelWrapper)  ){
                            result.put(JUMPURL, jsonResultStr.getString(CodeUrl).contains("qrcode?uuid")?URLDecoder.decode(jsonResultStr.getString(CodeUrl).split("uuid=")[1]):HandlerUtil.UrlDecode(jsonResultStr.getString(CodeUrl)));

                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString(CodeUrl).contains("qrcode?uuid")?URLDecoder.decode(jsonResultStr.getString(CodeUrl).split("uuid=")[1]):HandlerUtil.UrlDecode(jsonResultStr.getString(CodeUrl)));
                        }
                        payResultList.add(result);
                    }
                }else {
                    log.error("[佰富]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
        } catch (Exception e) {
            log.error("[佰富]3.发送支付请求，及获取支付请求结果出错：{}",e.getMessage(),e);
            throw new PayException(resultStr);
        }
        log.debug("[佰富]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[佰富]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}