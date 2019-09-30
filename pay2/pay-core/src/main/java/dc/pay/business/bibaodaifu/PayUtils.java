package dc.pay.business.bibaodaifu;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;

public class PayUtils {

	
	
	/**
	 * 加密业务数据
	 * @param content
	 * @return
	 * @throws Exception
	 */
	public static String encrypt(String content,String public_key) throws Exception {
		//平台公钥
		PublicKey publicKey  = RSAUtils.getPublicKey(public_key);
		return Base64.encode(RSAUtils.encrypt(publicKey, content.getBytes("UTF-8")));
	}
	
	/*
	 * 解密业务数据
	 */
	public static String decrypt(String cipher,String private_key) throws Exception {
		//商户私钥
		PrivateKey privateKey = RSAUtils.getPrivateKey( private_key);
		byte[] plainBytes = RSAUtils.decrypt(privateKey, Base64.decode(cipher));
		return new String(plainBytes,"UTF-8");
	}

	/**
	 * 商户私钥签名
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public static String sign(Map<String,String> params,String private_key) throws Exception {
		//商户私钥
		PrivateKey privateKey = RSAUtils.getPrivateKey( private_key);
		//生成签名字符串
		StringBuilder sb = new StringBuilder();
	 
		//对key进行排序
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		for(Object key : keys) {
			sb.append("&").append(key).append("=").append(params.get(key));
		}
		
		String str = sb.substring(1);
		String sign = Base64.encode(RSAUtils.sign(privateKey, str.getBytes("UTF-8")));
		return sign;
	}
	
	/**
	 * 平台公钥 验签
	 * @param params
	 * @return
	 * @throws Exception 
	 */
	public static boolean verify(Map<String,String> params,String sign,String public_key) throws Exception {
		 
		//生成签名字符串
		StringBuilder sb = new StringBuilder();
		//对key进行排序
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		for(Object key : keys) {
			sb.append("&").append(key).append("=").append(params.get(key));
		}
		
		String str = sb.substring(1);
		
		
		//平台公钥
		PublicKey publicKey  = RSAUtils.getPublicKey(public_key);
		return RSAUtils.verify(str.getBytes("UTF-8"), publicKey, Base64.decode(sign));
	}
}
