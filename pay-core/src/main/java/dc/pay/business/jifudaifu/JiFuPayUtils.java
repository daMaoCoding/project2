package dc.pay.business.jifudaifu;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;


public class JiFuPayUtils {

	public static String CHARSET = "UTF-8";

	/**
	 * 加密业务数据
	 */
	public static String encrypt(String content,String PLATFORM_PUBLIC_KEY) throws Exception {
		//平台公钥
		PublicKey publicKey  = JiFuRSAUtils.getPublicKey(PLATFORM_PUBLIC_KEY);
		return JiFuBase64.encode(JiFuRSAUtils.encrypt(publicKey, content.getBytes(CHARSET)));
	}



	/*
	 * 解密业务数据
	 */
	public static String decrypt(String cipher,String PRIVATE_KEY) throws Exception {
		//商户私钥
		PrivateKey privateKey = JiFuRSAUtils.getPrivateKey(PRIVATE_KEY);
		byte[] plainBytes = JiFuRSAUtils.decrypt(privateKey, JiFuBase64.decode(cipher));
		return new String(plainBytes,CHARSET);
	}

	/**
	 * 商户私钥签名
	 */
	public static String sign(Map<String,String> params,String PRIVATE_KEY) throws Exception {
		//商户私钥
		PrivateKey privateKey = JiFuRSAUtils.getPrivateKey(PRIVATE_KEY);
		//生成签名字符串
		StringBuilder sb = new StringBuilder();
	 
		//对key进行排序
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		for(Object key : keys) {
			sb.append("&").append(key).append("=").append(params.get(key));
		}
		
		String str = sb.substring(1);
		String sign = JiFuBase64.encode(JiFuRSAUtils.sign(privateKey, str.getBytes(CHARSET)));
		return sign;
	}
	
	/**
	 * 平台公钥 验签
	 */
	public static boolean verify(Map<String,String> params,String sign,String PLATFORM_PUBLIC_KEY) throws Exception {
		 
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
		PublicKey publicKey  = JiFuRSAUtils.getPublicKey(PLATFORM_PUBLIC_KEY);
		return JiFuRSAUtils.verify(str.getBytes(CHARSET), publicKey, JiFuBase64.decode(sign));
	}
}
