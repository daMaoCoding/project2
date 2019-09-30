package dc.pay.business.chenghui;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("CHENGHUI")
public final class ChengHuiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChengHuiPayRequestHandler.class);

    //Request头设置Content-Type为application/json;charse=UTF-8

    private static final String       payTypeKey    = "payTypeKey";     //  支付类型
    private static final String       tradeNo    = "tradeNo";           //  交易号
    private static final String       outTradeNo    = "outTradeNo";     //  商户订单号
    private static final String       body    = "body";                 //  商品描述
    private static final String       totalFee    = "totalFee";         //  总金额
    private static final String       requestIp    = "requestIp";       //  终端IP
    private static final String       nonceStr    = "nonceStr";         //  随机字符串
    private static final String       payIdentity    = "payIdentity";   //  支付用户标识
    private static final String       notifyUrl    = "notifyUrl";       //  通知地址
    private static final String       sign    = "sign";
    //private static final String       merchNo    = "merchNo";           //  商户号

    private static final String       codeUrl    = "codeUrl";
    private static final String       redirectUrl    = "redirectUrl";
    private static final String       returnCode    = "returnCode";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(payTypeKey , channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(tradeNo , channelWrapper.getAPI_MEMBERID());
            payParam.put(outTradeNo ,channelWrapper.getAPI_ORDER_ID() );
            payParam.put(body ,body );
            payParam.put(totalFee ,channelWrapper.getAPI_AMOUNT() );
            payParam.put(requestIp ,channelWrapper.getAPI_Client_IP() );
            payParam.put(nonceStr ,HandlerUtil.getRandomStrStartWithDate(5) );
            payParam.put(payIdentity ,HandlerUtil.getRandomStrStartWithDate(5) );
            payParam.put(notifyUrl , channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            //payParam.put(merchNo , );

        }

        log.debug("[诚汇]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toUpperCase();
        log.debug("[诚汇]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                String qrContextUrl=null,jumpUrl=null;
                if(HandlerUtil.valJsonObj(jsonResultStr,returnCode,"SUCCESS")){
                    if(HandlerUtil.valJsonObj(jsonResultStr,codeUrl,""))       qrContextUrl =jsonResultStr.getString(codeUrl);
                    if(HandlerUtil.valJsonObj(jsonResultStr,redirectUrl,""))   jumpUrl =jsonResultStr.getString(redirectUrl);
                }

                if(StringUtils.isNotBlank(qrContextUrl)){
                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(qrContextUrl));
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(jumpUrl)){
                    result.put(JUMPURL, HandlerUtil.UrlDecode(jumpUrl));
                    payResultList.add(result);
                }else{
                    log.error("[诚汇]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }

        } catch (Exception e) {
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //payResultList.add(result);
            log.error("[诚汇]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[诚汇]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[诚汇]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}