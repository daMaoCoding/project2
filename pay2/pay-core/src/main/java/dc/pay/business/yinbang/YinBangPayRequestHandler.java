package dc.pay.business.yinbang;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
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

@RequestPayHandler("YINBANG")
public final class YinBangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YinBangPayRequestHandler.class);
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";
    public static final String  encoding = "UTF-8";
    public static final String VERSION = "1.0.9";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误：商户号&终端号");
        }
        String tradeMoney = channelWrapper.getAPI_AMOUNT();// 交易金额
        String payType =channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();// 支付方式
        String appSence = "1001";// 应用场景
        String asynURL =channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL();// 异步地址
        String businessOrdid = channelWrapper.getAPI_ORDER_ID();
        String merId = channelWrapper.getAPI_MEMBERID().split("&")[0];
        String terId = channelWrapper.getAPI_MEMBERID().split("&")[1];
        String SERVER_PUBLIC_KEY = channelWrapper.getAPI_PUBLIC_KEY();
        String PRIVATE_KEY = channelWrapper.getAPI_KEY();
        // 拼接请求参数
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("businessOrdid", businessOrdid);
        map.put("orderName", "PAY");
        map.put("merId", merId);
        map.put("terId", terId);
        map.put("tradeMoney", tradeMoney);
        map.put("appSence", appSence);
        map.put("selfParam", "helloWorld");// 商户自己传递
        map.put("payType", payType);
        map.put("asynURL", asynURL);
        String json = GsonUtil.toJson(map);
        // 获取请求的参数
        Map<String, String> payParam = getRequestMap(json,SERVER_PUBLIC_KEY, PRIVATE_KEY, VERSION, merId);
        log.debug("[银邦]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }


    protected String buildPaySign(Map payParam) throws PayException {
        if(null!=payParam && payParam.containsKey("sign") && StringUtils.isNotBlank(String.valueOf(payParam.get("sign")))){
            log.debug("[银邦]-[请求支付]-2.生成加密URL签名完成：" + String.valueOf(payParam.get("sign")));
            return String.valueOf(payParam.get("sign"));
        }else{
            log.debug("[银邦]-[请求支付]-2.生成加密URL签名出错，未找到sign：" + JSON.toJSONString(payParam));
            return null;
        }
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      //  payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map<String, String> resultMap = new HashMap<String, String>();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            String respTxt = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();

            // 服务器没有返回数据或返回异常
            if (StringUtils.isEmpty(respTxt) || respTxt.contains("</html>")) {
                log.error("[银邦]3.发送支付请求，及获取支付请求结果：" + respTxt + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException("请求结果出错:"+respTxt);
            }

            Map<String, String> respMap = GsonUtil.fromJson(respTxt, Map.class);
             //验签和解密
            JsonResult verifyResult = YinBangUtil.verifyAndDecrypt(respMap.get("encParam"), respMap.get("sign"), channelWrapper.getAPI_PUBLIC_KEY(),channelWrapper.getAPI_KEY());


            if (!"1000".equals(verifyResult.getRespCode()) && null!= verifyResult.getData()) {
                String respDesc = JSON.parseObject(verifyResult.getData().toString()).getString("respDesc");
                log.error("[银邦]3.发送支付请求，及获取支付请求结果：" + respDesc + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException("请求结果出错");
            }
            try {
                resultMap = GsonUtil.fromJson(verifyResult.getData().toString(), Map.class);
            } catch (Exception e) {
                log.error("[银邦]3.发送支付请求，及获取支付请求结果：转换json格式错误" + GsonUtil.toJson(verifyResult) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException("请求结果出错:"+respTxt);
            }
            String code = resultMap.get("respCode"); // 返回码返回1000表示成功。当respCode为1000时，订单数据才有效。
            if (!"1000".equals(code)) {
                log.error("[银邦]3.发送支付请求，及获取支付请求结果：转换json格式错误" + GsonUtil.toJson(resultMap) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultMap.get("respDesc:"+respTxt));
            }

            HashMap<String, String> result = Maps.newHashMap();
            result.put(QRCONTEXT, resultMap.get("code_url"));
            result.put(PARSEHTML, GsonUtil.toJson(resultMap));
            payResultList.add(result);
        } catch (Exception e) {
            log.error("[银邦]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[银邦]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(JUMPURL)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayHtmlContent(null);
                    requestPayResult.setRequestPayJumpToUrl(resultMap.get(JUMPURL));
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                }
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[银邦]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
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
            log.error("[银邦]-[请求支付]-3.组装请求参数错误"+e.getMessage(),e);
        }
        return null;
    }




}