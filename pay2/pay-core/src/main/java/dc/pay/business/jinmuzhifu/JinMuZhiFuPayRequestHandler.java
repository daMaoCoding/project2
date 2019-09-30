package dc.pay.business.jinmuzhifu;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("JINMUZHIFU")
public final class JinMuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinMuZhiFuPayRequestHandler.class);

    private static final String      cid	 = "cid";     //  渠道号	1001
    private static final String      total_fee	 = "total_fee";     //  价格	100
    private static final String      title	 = "title";     //  标题描述	test
    private static final String      attach	 = "attach";     //  自定义参数	test
    private static final String      platform	 = "platform";     //  支付类型	CR_WAP
    private static final String      cburl	 = "cburl";     //  跳转地址	http://www.baidu.com
    private static final String      orderno	 = "orderno";     //  订单号	199590173515201707038216131919
    private static final String      token_url	 = "token_url";     //  回调地址	http://www.test.com





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(cid,channelWrapper.getAPI_MEMBERID());
            payParam.put(total_fee,channelWrapper.getAPI_AMOUNT());
            payParam.put(title,channelWrapper.getAPI_ORDER_ID());
            payParam.put(attach,channelWrapper.getAPI_ORDER_ID());
            payParam.put(platform,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(cburl,channelWrapper.getAPI_WEB_URL());
            payParam.put(orderno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(token_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }

        log.debug("[金木支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }


    protected String buildPaySign(Map<String,String> params) throws PayException {
        //attach+cburl+cid+orderno+platform+title+token_url+total_fee+key
        String paramsStr = String.format("%s%s%s%s%s%s%s%s%s",
                params.get(attach),
                params.get(cburl),
                params.get(cid),
                params.get(orderno),
                params.get(platform),
                params.get(title),
                params.get(token_url),
                params.get(total_fee),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[金木支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("err") && "200".equalsIgnoreCase(jsonResultStr.getString("err")) && (jsonResultStr.containsKey("code_url")|| jsonResultStr.containsKey("code_img_url") )  ){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("code_url"))){
                                result.put(JUMPURL,  jsonResultStr.getString("code_url"));
                                payResultList.add(result);
                            }else if(StringUtils.isNotBlank(jsonResultStr.getString("code_img_url"))){

                                result.put(JUMPURL,  jsonResultStr.getString("code_img_url"));  //第三方要求，全部链接打开 @屌丝绅士3223941577
                                payResultList.add(result);

//                                HtmlPage endHtml = HandlerUtil.getEndHtml( jsonResultStr.getString("code_img_url"));
//                                if(null!=endHtml && endHtml.getByXPath("//div[@id='showimg']/img").size()==1 && null!=(HtmlImage) endHtml.getByXPath("//div[@id='showimg']/img").get(0)  ){
//                                    HtmlImage htmlImage = (HtmlImage) endHtml.getByXPath("//div[@id='showimg']/img").get(0);
//                                        String qrContentSrc = htmlImage.getSrcAttribute();
//                                        String qrSrc = QRCodeUtil.decodeByUrl(qrContentSrc);
//                                        if(StringUtils.isNotBlank(qrContentSrc)){
//                                            result.put(QRCONTEXT, qrSrc);
//                                            payResultList.add(result);
//                                        }else{ throw new PayException(endHtml.asXml());}
//
//                                }else{ throw new PayException(endHtml.asXml());}



                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[金木支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[金木支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[金木支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}