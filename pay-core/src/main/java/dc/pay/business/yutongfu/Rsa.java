package dc.pay.business.yutongfu;

import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import dc.pay.base.processor.PayException;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RsaUtil;

public class Rsa {


    /**
     * 
     * 
     * @author andrew
     * Nov 7, 2018
     * @throws PayException 
     */
   public static String  rsa(String context,byte[] privateKeyContent) throws PayException{
       try {
           String privateKeyStr = new String(privateKeyContent).replaceAll("-.*", "");
           privateKeyContent = Base64.getDecoder().decode(privateKeyStr.replace("\n", ""));
           PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyContent);
           KeyFactory keyFactory = KeyFactory.getInstance("RSA");
           PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
           java.security.Signature signature = java.security.Signature.getInstance("SHA1WithRSA");
           signature.initSign(privateKey);
           signature.update(context.getBytes(Charset.forName("UTF8")));
           
           String sign = Base64.getEncoder().encodeToString(signature.sign());
           return sign;
        } catch (Exception e) {
            throw new PayException("RSA数字签名出错:"+e.getMessage());
        }
    }



    public static String getSign(Map<String, String> sendMap,String privateKey) throws Exception {
        Map<String, String> transMap = new TreeMap<String, String>(sendMap);
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> item : transMap.entrySet()) {
            if(StringUtils.isEmpty(item.getValue()) || "null".equals(item.getValue())){
                continue;
            }
            stringBuilder.append(item.getKey()).append('=').append(item.getValue()).append('&');
        }
        if(stringBuilder.length() > 0){
            stringBuilder.setLength(stringBuilder.length()-1);
        }
        return rsa(stringBuilder.toString(), privateKey.getBytes());
    }




    public static boolean verify(Map<String,String> respMap,String pubKey){
        boolean result = false;
        String  signature = "signature";
        List paramKeys = MapUtils.sortMapByKeyAsc(respMap);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && org.apache.commons.lang.StringUtils.isNotBlank(respMap.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(respMap.get(paramKeys.get(i))).append("&");
            }
        }
        String paramsStr = signSrc.toString().substring(0, signSrc.toString().length() - 1);
        return  RsaUtil.validateSignByPublicKey(paramsStr, pubKey, respMap.get(signature),"SHA1withRSA");
    }





}
