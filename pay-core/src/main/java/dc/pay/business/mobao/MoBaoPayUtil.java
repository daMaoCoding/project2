package dc.pay.business.mobao;/**
 * Created by admin on 2017/6/8.
 */

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
public class MoBaoPayUtil {
    private static final Logger log =  LoggerFactory.getLogger(MoBaoPayUtil.class);
    public static  Map<String,String> parseBankFlag(String flag){
        if(!flag.isEmpty() && flag.contains(",")){
            String[] split = flag.split(",");
            if(split.length==3){
                String apiName       = split[0];
                String choosePayType = split[1];
                String bankCode      = split[2];
                choosePayType        = "null".equalsIgnoreCase(choosePayType)||"-".equalsIgnoreCase(choosePayType)||StringUtils.isBlank(choosePayType)?"":choosePayType;
                bankCode             = "null".equalsIgnoreCase(bankCode)     ||"-".equalsIgnoreCase(choosePayType)||StringUtils.isBlank(bankCode)     ?"":bankCode;
                Map<String, String> bankFlags = Maps.newHashMap();
                bankFlags.put("apiName", apiName);
                bankFlags.put("choosePayType", choosePayType);
                bankFlags.put("bankCode", bankCode);
                return bankFlags;
            }
        }
      return null;
    }
    public static String generatePayRequest(Map<String, String> paramsMap)throws PayException {
        if (!paramsMap.containsKey("apiName")|| StringUtils.isBlank(paramsMap.get("apiName"))) {
            log.error("[摩宝支付]请求支付参数错误，apiName不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("apiVersion")|| StringUtils.isBlank(paramsMap.get("apiVersion"))) {
            log.error("[摩宝支付]请求支付参数错误，apiVersion不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("platformID")|| StringUtils.isBlank(paramsMap.get("platformID"))) {
            log.error("[摩宝支付]请求支付参数错误，platformID不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("merchNo") || StringUtils.isBlank("merchNo")) {
            log.error("[摩宝支付]请求支付参数错误，merchNo不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("orderNo") || StringUtils.isBlank("orderNo")) {
            log.error("[摩宝支付]请求支付参数错误，orderNo不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("tradeDate")|| StringUtils.isBlank("tradeDate")) {
            log.error("[摩宝支付]请求支付参数错误，tradeDate不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("amt") || StringUtils.isBlank("amt")) {
            log.error("[摩宝支付]请求支付参数错误，amt不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("merchUrl")|| StringUtils.isBlank("merchUrl")) {
            log.error("[摩宝支付]请求支付参数错误，merchUrl不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("merchParam")) {
            log.error("[摩宝支付]请求支付参数错误，merchParam不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        if (!paramsMap.containsKey("tradeSummary")|| StringUtils.isBlank("tradeSummary")) {
            log.error("[摩宝支付]请求支付参数错误，tradeSummary不能为空："+ JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        String paramsStr = String.format("apiName=%s&apiVersion=%s&platformID=%s&merchNo=%s&orderNo=%s&tradeDate=%s&amt=%s&merchUrl=%s&merchParam=%s&tradeSummary=%s",
                                  paramsMap.get("apiName"), paramsMap.get("apiVersion"),paramsMap.get("platformID"),
                                  paramsMap.get("merchNo"), paramsMap.get("orderNo"), paramsMap.get("tradeDate"),
                                  paramsMap.get("amt"), paramsMap.get("merchUrl"),paramsMap.get("merchParam"), paramsMap.get("tradeSummary"));
        return paramsStr;
    }
    public static String signData(String sourceData,String key) throws PayException {
        String signStrintg = "";
        if ("MD5".equals(PayEumeration.SIGN_TYPE)) {
            signStrintg = signByMD5(sourceData, key);
        }
        signStrintg =signStrintg.replaceAll("\r", "").replaceAll("\n", "");
        return signStrintg;
    }
    public static String signByMD5(String sourceData, String key) throws PayException {
        String data = sourceData + key;
        MessageDigest md5 = null;
        byte[] sign = new byte[0];
        try {
            md5 = MessageDigest.getInstance("MD5");
            sign = md5.digest(data.getBytes(PayEumeration.CHAR_SET));
            return Bytes2HexString(sign).toUpperCase();
        } catch (Exception e) {
            log.error("[摩宝支付]生成MD5签名出错，"+e.getMessage()+","+sourceData,e);
            throw new PayException(SERVER_MSG.REQUEST_PAY_BUILDSIGN_ERROR);
        }
    }
    public static String Bytes2HexString(byte[] b) {
        StringBuffer ret = new StringBuffer(b.length);
        String hex = "";
        for (int i = 0; i < b.length; i++) {
            hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret.append(hex.toUpperCase());
        }
        return ret.toString();
    }
    public static String buildWxUrlForManagerCGI(Map<String, String> paramsMap) {
        String url = "https://trade.mobaopay.com/standard/gateway/manager.cgi?";
        String paramsStr = String.format("orderId=%s&channelType=%s&bankCode=%s&payType=%s&amt=%s&merchNo=%s&__token=%s&__long=%s&m=%s&_=%s",
                paramsMap.get("orderId"), paramsMap.get("channelType"),"WXSP",
                "16", paramsMap.get("amt").trim().concat("+"), paramsMap.get("merchNo"),
                paramsMap.get("__token"), "ZElJ4R73oucRitV1","getCodeUrl",System.currentTimeMillis());
        return url.concat(paramsStr);
    }
    public static String buildZfbUrlForManagerCGI(Map<String, String> paramsMap) {
        String url = "https://trade.mobaopay.com/standard/gateway/manager.cgi?";
        String paramsStr = String.format("orderId=%s&channelType=%s&bankCode=%s&payType=%s&amt=%s&merchNo=%s&__token=%s&__long=%s&m=%s&_=%s",
                paramsMap.get("orderId"), paramsMap.get("channelType"),"ALSP",
                "15", paramsMap.get("amt").trim().concat("+"), paramsMap.get("merchNo"),
                paramsMap.get("__token"), "ZElJ4R73oucRitV2","getCodeUrl",System.currentTimeMillis());
        return url.concat(paramsStr);
    }
    public static boolean verifyData(String signData, String srcData,String api_key) throws PayException {
            if (signData.equalsIgnoreCase(signByMD5(srcData, api_key))) {
                return true;
            } else {
                return false;
            }
    }
}