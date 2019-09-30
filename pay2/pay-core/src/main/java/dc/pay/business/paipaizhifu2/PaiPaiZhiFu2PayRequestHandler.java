package dc.pay.business.paipaizhifu2;

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

/**
 * @author Cobby
 * May 30, 2019
 */
@RequestPayHandler("PAIPAIZHIFU2")
public final class PaiPaiZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PaiPaiZhiFu2PayRequestHandler.class);

    private static final String merchantID             ="merchantID";//商户ID     Y   Y    商户ID,由支付平台分配
    private static final String bankCode               ="bankCode";  //银行类型    Y   Y    银行类型,具体请参考附录1
    private static final String tradeAmt               ="tradeAmt";  //金额       Y   Y    单位元（人民币）,2位小数,最小支付金额为0.02
    private static final String orderNo                ="orderNo";   //商户订单号  Y    Y    商户系统订单号，该订单号将作为支付接口的返回数据。该值需在商户系统内唯一，系统暂时不检查该值是否唯一
    private static final String notifyUrl              ="notifyUrl"; //异步通知地址 Y   Y    下行异步通知过程的返回地址，需要以http://开头且没有任何参数
    private static final String returnUrl              ="returnUrl"; //同步通知地址 Y   Y    下行同步通知过程的返回地址
    private static final String payerID                ="payerID";   //支付用户ID   Y        商户平台支付用户的ID
    private static final String payerIP                ="payerIP";   //支付用户IP   Y    Y    用户在下单时的真实IP，支付接口将会判断玩家支付时的ip和该值是否相同。若不相同，接口将提示用户支付风险
//    private static final String attach                 ="attach";//备注消息           备注信息，下行中会原样返回。若该值包含中文，请注意编码


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantID, channelWrapper.getAPI_MEMBERID());
                put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(tradeAmt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(payerID,channelWrapper.getAPI_ORDER_ID());
                put(payerIP,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[派派支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //merchantID={}&bankCode={}&tradeAmt={}&orderNo={}&notifyUrl={}&returnUrl={}&payerIP={}{key}
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merchantID+"=").append(api_response_params.get(merchantID)).append("&");
        signSrc.append(bankCode+"=").append(api_response_params.get(bankCode)).append("&");
        signSrc.append(tradeAmt+"=").append(api_response_params.get(tradeAmt)).append("&");
        signSrc.append(orderNo+"=").append(api_response_params.get(orderNo)).append("&");
        signSrc.append(notifyUrl+"=").append(api_response_params.get(notifyUrl)).append("&");
        signSrc.append(returnUrl+"=").append(api_response_params.get(returnUrl)).append("&");
        signSrc.append(payerIP+"=").append(api_response_params.get(payerIP));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[派派支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[派派支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("errcode") && "0".equalsIgnoreCase(jsonObject.getString("errcode"))
                        && jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode"))) {
                    String code_url = jsonObject.getString("qrcode");
                    if (handlerUtil.isWapOrApp(channelWrapper)) {
                        result.put(JUMPURL, code_url);
                    }else{
                        result.put(QRCONTEXT, code_url);
                    }
                }else {
                    log.error("[派派支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[派派支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[派派支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[派派支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}