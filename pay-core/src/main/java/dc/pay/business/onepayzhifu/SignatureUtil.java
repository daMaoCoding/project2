package dc.pay.business.onepayzhifu;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

//import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.alibaba.druid.util.StringUtils;
import org.apache.shiro.codec.Base64;


public class SignatureUtil {
	//按照规则生成需要签名的字符串
	public static String generateSignContent(Map<String, String> map){
        if (null == map || map.isEmpty()) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        Map<String, String> tmpMap = map;
        try {
            List<Map.Entry<String, String>> infoIds = new ArrayList<>(tmpMap.entrySet());
            // 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）
            Collections.sort(infoIds, (Map.Entry<String, String> o1, Map.Entry<String, String> o2) ->o1.getKey().compareTo(o2.getKey())
            );

            // 构造URL 键值对的格式
            for (Map.Entry<String, String> item : infoIds) {
                String key = item.getKey();
                String value = item.getValue();
                if (StringUtils.isEmpty(value) || key.equalsIgnoreCase("signType") || key.equalsIgnoreCase("sign") || "null".equalsIgnoreCase(value)) {
                    continue;
                }

                buf.append(key + "=" + value+"&");
            }
            buf.deleteCharAt(buf.length()-1);
        } catch (Exception e) {
            return null;
        }
        return buf.toString();
    }
	
	//对需要加密的数据进行加密  生成原始签名   （即文档api中的十进制签名字符串）            参数：拼接的字符串  签名类型rsa  私钥数据
	public static String sign(String serializationParam,String signType,String privateKey) {
        String stringA =serializationParam;
        /*String stringA="amountFee=20.00&currency=CNY&deviceType=WEB&goodsTitle=ANP&inputCharset=UTF-8&issuingBank=UNIONPAY&merchantId=3411&merchantTradeId=OP051810091409574598AAE&notifyUrl=http://c0d999be.ngrok.io/Payment/Callback/10141/2&payIp=::1&payType=EC&returnUrl=http://c0d999be.ngrok.io/Payment/Callback/10141/1&subIssuingBank=CCB&version=1.0";*/
        /*String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDYwKxJKBbqVCf4MS4way9DP180uK8lwCqYuNGocgBUtdCL1g+g/h+0AKKIU/1amHHCnEiEX7s2mZdE12wHZ5LPzcYcb3UaHy1lbN/lrE6THKcc4m/Tm60yiGrx0tAlvU7rcWjxlUZgFnoQ7z3ASQEYxNWeKmTqSbK+3+29AXyhWJV32qcI4K74zZwmkHi/C8y7kv3ZlmjFh/CQD+wPG1ma/lvcTDUfVcCu00cCNL2YNc9mrOv5Olku1O9lkIeQsnwemPD76PmIRm16pfumyYOw7PDL7rVUZA4N7AIV80YHaT2eTjfp9OPGoTlKPVnTrukXWltvvxjorLAgl/ftUlcTAgMBAAECggEBAJaGz98KSUe44/0X4nTbqvnvo6WQcIot9ycSiv+JFDPABDFomnf6o3e1TSqR8Yud/LrjH/VWKkSTy5qSZEKMKkfIJsIOoWtfpX4fayosTHEb3+5OTszM+y3x0RtZhRYAbzNREklbmoEWevURwHet+2YBJDzwQh72yXyHNKoiwzOkcuA+DKpycYTwrMLtOXObFKftOlT9U/jKrR8UGGvldSOj6HaPJwbxI+NF4NXK4Rb9u9fgmust3Fbe8WDukOLECpJ+BLWizLn86LlU9p6VQ9IDyQ8w87ZncciwQ9i0WzH4n4usZiWntM6KaChhDdcaaMwM2uijrMLvqd3jUOIUr3ECgYEA8906aigi0SoPvUI2JfkcwPPW93noW04BqDs3NbctX2fD5y6BDBUUyJhvyXX8JS8CCQzESU8D8UuFNn7Ady+kbufseDIpwYmCDsZ5iYmnaL0+O/nAJE57B+UMBhOia5TE6d1ezyJnYpXcucN8XepOlwLCZDpvXnhsvXoGDf2X7YsCgYEA44oM12BOHR+Yu2V2u13d8fwob2jPkK+3ys9+TAYw8KMwZeq3tOHpeg/uxqHWsmusKaFp8y9EgoLvYT724xvwK8OpKdALN/IdB3Tp4177pvCDJdEszQU9cfOyuAofMszAbmQQosPRmHEf/JaxeD2WvxX5QHlCzlGPBZ1BHBWp/ZkCgYACZmn9w3lcP6K6BR3NT99VktDJzLxZsbHYewS3FZ9vsfC+pLmGaeqzcrSe8fdSUl0dw69m3U03obuk5qh0Q9bFdKjiIfRBQF40UQW/McihAkPLiVC3ysHxOPLeWHEfx+4MMy6MsVT9JmZuuZrOvdCaMd/B5sVT07GWR84pcxZ8swKBgGIt0Pilr07VsVs6no6eJAQj33ai7NzrVQyHl2FKUpeicmGnc3jy+YEXmtnOPwthdjnIUiUCQjdmM8/sES9RgMIBfN5zzENpjqkHA1x+QWt4373Tkdcs6aRfmL/cbjXHL0jymFMC109FkJ22kADn4kG1Q4VUTUUk3+d2QP/TQTQRAoGBAOkSubwaE9lhVNtkcCImLobXChWp3+DW+uL9fzTpC9Rs9eCVkJXrJjCf/2jPb1JZi0x4buW/zQDI4mVqv64jXwVGiu+P0Wh7B67CSBwJKh663tQheyT2UAzFs8qrxv7vwcUj/9SzM+PBXgeQmCtuEDlcM5saNCsjxIs5qKYJEwcJ";*/
        String input_charset = "UTF-8";
        try {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(privateKey));
            KeyFactory keyf = KeyFactory.getInstance(signType,"SunRsaSign");
            PrivateKey priKey = keyf.generatePrivate(priPKCS8);
            java.security.Signature signature = java.security.Signature.getInstance("SHA1WithRSA","SunRsaSign");
            signature.initSign(priKey);
            signature.update(stringA.getBytes("UTF-8"));
            byte[] signed = signature.sign();
            return new String(Base64.encode(signed));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
	
	
	//签名验证操作    参数对应    				公钥                       拼接的字符串         需要验签的十六进制签名数据          签名方式  RSA
	public static  boolean rsaYanQian(String publicKey,String serializationParam,String signSixteen,String signType){
        String stringA = serializationParam;
        
        
       /* String signMap =signTen;
        String str = ByteUtils.toHexAscii(signMap.getBytes());*/
       
        try {
            String decSign = new String(ByteUtils.fromHexAscii(signSixteen), "utf-8");
            byte[] keyBytes = Base64.decode(publicKey);
            X509EncodedKeySpec bobPubKeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance(signType,"SunRsaSign");
            PublicKey pubKey = factory.generatePublic(bobPubKeySpec);
            Signature signetcheck = Signature.getInstance("SHA1WithRSA","SunRsaSign");
            signetcheck.initVerify(pubKey);
            signetcheck.update(stringA.trim().getBytes("utf-8"));
            byte[] signByte = Base64.decode(decSign);
            return signetcheck.verify(signByte);
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
	//十六进制转十进制
	public static byte[] sixteenToTen(String sign) {
		return ByteUtils.fromHexAscii(sign);
	}
	//十进制转十六进制
	public static String tenToSixteen(String signsix) {
		return ByteUtils.toHexAscii(signsix.getBytes());
	}
}
