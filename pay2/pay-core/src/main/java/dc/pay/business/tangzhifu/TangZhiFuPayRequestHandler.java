package dc.pay.business.tangzhifu;

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

@RequestPayHandler("TANGZHIFU")
public final class TangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TangZhiFuPayRequestHandler.class);


     private static final String      merid = "merid"; //	商户编号	20	字符串	由平台分配
     private static final String      Merorderno = "Merorderno"; //	商户订单号	32	字符串	自主生成
     private static final String      Toamount = "Toamount"; //	金额	10	字符串	单位：元
     private static final String      Return_url = "Return_url"; //	返回地址		字符串	必填（支付成功之后返回）
     private static final String      Paymentype = "Paymentype"; //	支付方式	10	Int	必填（1-支付宝 2-微信 3-QQ）
     private static final String      Notifyurl = "Notifyurl"; //	异步通知地址
     private static final String      Sign = "Sign"; //	签名值


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merid,channelWrapper.getAPI_MEMBERID());
            payParam.put(Merorderno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(Toamount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(Return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(Paymentype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(Notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[唐支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
//        String pay_md5sign = null;
//        List paramKeys = MapUtils.sortMapByKeyAsc(params);
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < paramKeys.size(); i++) {
//            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
//                continue;
//            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
//        }
//        sb.append("key=" + channelWrapper.getAPI_KEY());
//        String signStr = sb.toString(); //.replaceFirst("&key=","")
//        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
//        log.debug("[唐支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
//        return pay_md5sign;

        try {
            String jsonStr = JSON.toJSONString(params);
            String encrypt = AES128ECB.Encrypt(jsonStr, channelWrapper.getAPI_KEY());
           // String repleasResult =  encrypt.replaceAll("\\+","%2B");
            return   encrypt;
        } catch (Exception e) {
            throw new PayException("请求支付签名失败。");
        }

    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();

        //参数
        Map<String, String> lastParams = Maps.newHashMap();
        lastParams.put(Sign,pay_md5sign);
        lastParams.put(merid,channelWrapper.getAPI_MEMBERID());


        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),lastParams).toString().replace("method='post'","method='get'"));
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


              //  HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), lastParams);
              //  System.out.println(endHtml.asXml());


                String jsonResultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), lastParams); //第三方后加的新json接口
                JSONObject resultJson = JSON.parseObject(jsonResultStr);

                if(null!=resultJson && resultJson.containsKey("code") && "200".equalsIgnoreCase(resultJson.getString("code"))  && resultJson.containsKey("description") && "OK".equalsIgnoreCase(resultJson.getString("description"))   && resultJson.containsKey("url")){
                            if(StringUtils.isNotBlank(resultJson.getString("url"))){

                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, resultJson.getString("url"));
                                }else{
                                    result.put(QRCONTEXT, resultJson.getString("url"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(jsonResultStr);
                    }
            }
        } catch (Exception e) { 
             log.error("[唐支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[唐支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[唐支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}