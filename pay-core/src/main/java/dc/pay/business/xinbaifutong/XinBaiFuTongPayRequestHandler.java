package dc.pay.business.xinbaifutong;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XINBAIFUTONG")
public final class XinBaiFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinBaiFuTongPayRequestHandler.class);
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";
    public static final String  encoding = "UTF-8";

    public static final String  merchantId     = "merchantId";   //商户号
    public static final String  notifyUrl      = "notifyUrl";    //通知URL
    public static final String  outOrderId     = "outOrderId";   //交易号
    public static final String  subject        = "subject";      //订单名称
    public static final String  transAmt       = "transAmt";     //交易金额
    public static final String  scanType       = "scanType";     //扫码类型
    public static final String  sign           = "sign";         //签名

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> map = new HashMap<String, String>();
        map.put(merchantId, channelWrapper.getAPI_MEMBERID());
        map.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        map.put(outOrderId, channelWrapper.getAPI_ORDER_ID());
        map.put(subject, "PAY");
        map.put(transAmt, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        map.put(scanType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        log.debug("[新百付通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(map));
        return map;
    }


    protected String buildPaySign(Map payParam) throws PayException {
        String paramsStr = String.format("merchantId=%s&notifyUrl=%s&outOrderId=%s&scanType=%s&subject=%s&transAmt=%s",
                payParam.get(merchantId),
                payParam.get(notifyUrl),
                payParam.get(outOrderId),
                payParam.get(scanType),
                payParam.get(subject),
                payParam.get(transAmt));

        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey2(paramsStr,channelWrapper.getAPI_KEY());	// 签名
        } catch (Exception e) {
            log.error("[新百付通]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        //String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新百付通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            HashMap<String, String> result = Maps.newHashMap();
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
                payResultList.add(result);
            }else{
                String respTxt = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject respJson = JSON.parseObject(respTxt);
                if (StringUtils.isEmpty(respTxt)||null==respJson || respTxt.contains("</html>") ||"E".equalsIgnoreCase(respJson.getString("respType"))  || !respJson.containsKey("respType") || !respJson.containsKey("transAmt")|| !respJson.containsKey("payCode")|| StringUtils.isBlank(respJson.getString("payCode"))) {
                    log.error("[新百付通]3.发送支付请求，及获取支付请求结果：" + respTxt + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException("请求结果出错:"+respTxt);
                }
                result.put(QRCONTEXT, respJson.getString("payCode"));
                result.put(PARSEHTML, respTxt);
                payResultList.add(result);
            }

        } catch (Exception e) {
            log.error("[新百付通]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[新百付通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
               requestPayResult = super.buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[新百付通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }



    /**
     * 组装请求参数
     */
    private Map<String, String> getRequestMap(String jsonParam, String serverPublicKey, String privateKey, String version, String merId){
        try{
            //服务器公钥加密
            byte by[] = SecurityRSAPay.encryptByPublicKey(jsonParam.getBytes(encoding), Base64Local.decode(serverPublicKey));
            String baseStrDec = Base64Local.encodeToString(by, true);
            //商户自己的私钥签名
            byte signBy[] = SecurityRSAPay.sign(by, Base64Local.decode(privateKey));
            String sign = Base64Local.encodeToString(signBy, true);

            //组装请求参数
            Map<String,String>  map = new HashMap<String, String>();
            map.put("encParam", baseStrDec);
            map.put("merId", merId);
            map.put("sign",sign);
            map.put("version",  version);
            return map;
        }catch (Exception e){
            log.error("[新百付通]-[请求支付]-3.组装请求参数错误"+e.getMessage(),e);
        }
        return null;
    }




}