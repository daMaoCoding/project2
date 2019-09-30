package dc.pay.business.shangyinxin;

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

@RequestPayHandler("SHANGYINXIN")
public final class ShangYinXinPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShangYinXinPayRequestHandler.class);

    private static final String       service  = "service";               //接口名称directPay
    private static final String       merchantId  = "merchantId";         //商户号
    private static final String       notifyUrl  = "notifyUrl";           //通知URL
    private static final String       signType  = "signType";             //签名类型RSA
    private static final String       inputCharset  = "inputCharset";     //参数编码字符集UTF-8
    private static final String       outOrderId  = "outOrderId";         //外部交易号
    private static final String       subject  = "subject";               //商品名称
    private static final String       body  = "body";                     //商品描述
    private static final String       transAmt  = "transAmt";             //交易金额元
    private static final String       payMethod  = "payMethod";           //默认支付方式bankPay
    private static final String       defaultBank  = "defaultBank";       //默认网银(银行代码)
    private static final String       channel  = "channel";               //默认渠道B2C
    private static final String       cardAttr  = "cardAttr";             //卡类型01
    private static final String       returnUrl  = "returnUrl";             //卡类型01
    private static final String       version  = "version";
    private static final String       sign  = "sign";                     //签名


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(service , "directPay");
            payParam.put(merchantId ,channelWrapper.getAPI_MEMBERID() );
            payParam.put(notifyUrl , channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(signType , "RSA");
            payParam.put(inputCharset , "UTF-8");
            payParam.put(outOrderId ,channelWrapper.getAPI_ORDER_ID() );
            payParam.put(subject , subject);
            payParam.put(body , body);
            payParam.put(transAmt , HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(payMethod , "bankPay");
            payParam.put(defaultBank ,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() );
            payParam.put(channel , "B2C");
            payParam.put(cardAttr , "01");
        }

        log.debug("[商银信支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())   || signType.equalsIgnoreCase(paramKeys.get(i).toString())   )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i)));
            if(i!=paramKeys.size()-1) sb.append("&");
        }

        try {
           // linkString = "body=body&cardAttr=01&channel=B2C&defaultBank=CCB&detailUrl=http://192.168.9.13:8080/allscore/allscore_detail.jsp&inputCharset=UTF-8&merchantId=001015013101118&notifyUrl=http://192.168.9.21:8080/allscore/allscore_notifyRSA.jsp&outOrderId=159779225325750&payMethod=bankPay&returnUrl=http://192.168.9.21:8080/allscore/allscore_returnRSA.jsp&service=directPay&subject=test&transAmt=0.01&version=1";
            pay_md5sign = RsaUtil.signByPrivateKey2( sb.toString(),channelWrapper.getAPI_KEY(),"UTF-8");
        } catch (Exception e) {
            log.debug("[商银信支付]-[请求支付]-2.生成加密URL签名失败：{}" , JSON.toJSONString(sb.toString()));
            throw  new PayException("商银信支付,生成加密URL签名失败,可能密钥不正确");
        }
        log.debug("[商银信支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }




    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||( HandlerUtil.isWapOrApp(channelWrapper)  && !channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("GEFU_BANK_WAP_QQ_SM")  )  ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());//.replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(1==1 ) {  //扫码（银联扫码&微信&支付宝&QQ）
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[商银信支付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            payResultList.add(result);
            //log.error("[商银信支付]3.发送支付请求，及获取支付请求结果出错：", e);
            //throw new PayException(e.getMessage(), e);
        }
        log.debug("[商银信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[商银信支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}