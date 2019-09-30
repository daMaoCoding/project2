package dc.pay.business.heyuan;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

public class AesSignUtil {

	/**
	 * 加密
	 * @param key 密钥
	 * @param dataJson 加密json字符串
	 * @return 加密报文
	 * @throws Exception
	 */
	public static Map<String, String> encrypt(String key, String dataJson) throws Exception{
		Map<String, String> retunParam = new HashMap<>();
		String signKey = key.substring(16);
        String dataKey = key.substring(0,16);
        String sign = DigestUtils.sha1Hex(dataJson + signKey);
//        System.out.println("加密报文签名:" + sign);
        String encryptData = Base64.encode(AES.encode(dataJson, dataKey));
//        System.out.println("加密报文内容:" + encryptData);
		retunParam.put("encryptData", encryptData);
		retunParam.put("sign", sign);
		return retunParam;
	}
	
	/**
	 * 解密
	 * @param key
	 * @param encryptData
	 * @param signature
	 * @return 解密后的数据
	 * @throws Exception
	 */
	public static String decrypt(String key, String encryptData, String signature) throws Exception{
		String dataPlain = AES.decode(Base64.decode(encryptData), key.substring(0, 16));
//		System.out.println("解密报文内容:" + dataPlain);
		String checkSign = DigestUtils.sha1Hex(dataPlain + key.substring(16));
//		System.out.println("解密报文内容生产签名:" + checkSign);
		signature = URLDecoder.decode(signature, StandardCharsets.UTF_8.name());
//		System.out.println("加密报文里的签名:" + checkSign);
		if(!signature.equals(checkSign)){
			throw new Exception("签名验证失败");
		}
		return dataPlain;
	}
	
}
