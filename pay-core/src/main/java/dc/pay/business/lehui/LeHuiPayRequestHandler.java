package dc.pay.business.lehui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 5, 2018
 */
@RequestPayHandler("LEHUI")
public final class LeHuiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LeHuiPayRequestHandler.class);

    //参数名             类型           必填          说明
    //merchantID         String          是          商户号
    //notifyUrl          String          是          异步回调推送支付结果通知商家服务器URL(由商家自行生产)
    //outTradeNo         String          是          商家订单号（由商家自行生产）
    //payMoney           Int             是          订单总金额，单位为分 (10.00=1000)
    //goodsName          String          是          商品名称
    //returnUrl          String          否          返回商家支付结果页面URL（由商家自行生产)
    //remark             String          否          自定义字段（回调时原样返回）
    //channel            int             是          渠道：10001-银行卡支付   50001-京东钱包支付
    //bankId             String          否          银行代码，银行卡支付时选填，如有填写直接转跳至银行
    //sign               String          是          参数签名字符串
    private static final String merchantID                 ="merchantID";
    private static final String notifyUrl                  ="notifyUrl";
    private static final String outTradeNo                 ="outTradeNo";
    private static final String payMoney                   ="payMoney";
    private static final String goodsName                  ="goodsName";
    private static final String returnUrl                  ="returnUrl";
    private static final String remark                     ="remark";
    private static final String channel                    ="channel";
    private static final String bankId                     ="bankId";
//    private static final String sign                       ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantID, channelWrapper.getAPI_MEMBERID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                put(payMoney,  channelWrapper.getAPI_AMOUNT());
                put(goodsName,"name");
                //非扫码时传入
                if (!handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                    put(returnUrl,channelWrapper.getAPI_WEB_URL());
                }
                put(remark, channelWrapper.getAPI_MEMBERID());
                if (handlerUtil.isWY(channelWrapper) && !handlerUtil.isWebYlKjzf(channelWrapper)) {
                    put(bankId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(channel,"10001");
                }else {
                    put(channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        log.debug("[乐汇]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(merchantID)).append("&");
        signSrc.append(api_response_params.get(notifyUrl)).append("&");
        signSrc.append(api_response_params.get(outTradeNo)).append("&");
        signSrc.append(api_response_params.get(payMoney)).append("&");
        signSrc.append(api_response_params.get(channel)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = null;
        try {
            signMd5 = HandlerUtil.getMD5UpperCase(URLEncoder.encode(paramsStr,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("[乐汇]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[乐汇]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[乐汇]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[乐汇]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
               log.error("[乐汇]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
               throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[乐汇]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("resultCode") && "100".equalsIgnoreCase(resJson.getString("resultCode"))  && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
                JSONObject resJson2 = resJson.getJSONObject("data");
                String qr_code = resJson2.getString("qr_code");
                if (StringUtils.isBlank(qr_code)) {
                    log.error("[乐汇]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                 }
                String qr = QRCodeUtil.decodeByUrl(qr_code);
                if (StringUtils.isBlank(qr)) {
                    log.error("[乐汇]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, qr);
            }else {
                log.error("[乐汇]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[乐汇]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[乐汇]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}