package dc.pay.business.fulianka;

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
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("FULIANKAZHIFU")
public final class FuLianKaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FuLianKaPayRequestHandler.class);


    private static final String   serviceCode	 = "serviceCode";   //服务码	S	M	必填，网银、扫码、Wap支付为：ONLINE_PAY
    private static final String   merchantOrderNo	 = "merchantOrderNo";   //商户唯一的请求订单号	S(6-48)	M	订单号长度为16-64字节
    private static final String   signType	 = "signType";   //签名类型	S	M	必填
    private static final String   sign	 = "sign";   //签名	S	M	必填;签名字符串
    private static final String   partnerId	 = "partnerId";   //商户ID	FS(20)	M	必填,签约的服务平台账号对应的合作方ID,由服务平台分配。定长20字符(需要在merchant表中配置)
    private static final String   clientIp	 = "clientIp";   //客户端IP	S	O	客户端IP，部分渠道需要避光，如果可以，都传入，目前H5支付时必填
    private static final String   amount	 = "amount";   //交易金额	M	M	必填
    private static final String   payType	 = "payType";   //支付方式:  SCAN_QR/H5
    private static final String   payChannel	 = "payChannel";   //支付机构编码 WEIXIN:微信  ALIPAY:支付宝
    private static final String   notifyUrl	 = "notifyUrl";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(serviceCode,"ONLINE_PAY");
            payParam.put(merchantOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(signType,"MD5");
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(partnerId,channelWrapper.getAPI_MEMBERID());
            payParam.put(clientIp,channelWrapper.getAPI_Client_IP());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(payChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
        }

        log.debug("[付联卡支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        String signStr = sb.toString().replaceFirst("&key=","");
       // pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        pay_md5sign = DigestUtils.md5Hex(signStr);
        log.debug("[付联卡支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper) &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("resultCode") && "SUCCESS".equalsIgnoreCase(jsonResultStr.getString("resultCode"))
                            && jsonResultStr.containsKey("returnCode") && "SUCCESS".equalsIgnoreCase(jsonResultStr.getString("returnCode"))
                            && jsonResultStr.containsKey("payUrl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payUrl"))){
                                if( HandlerUtil.isWY(channelWrapper)||  HandlerUtil.isYLKJ(channelWrapper) ||  HandlerUtil.isWapOrApp(channelWrapper)  ){
                                    result.put(JUMPURL,  jsonResultStr.getString("payUrl"));
                                }else{
                                    result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(jsonResultStr.getString("payUrl")));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[付联卡支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[付联卡支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[付联卡支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}