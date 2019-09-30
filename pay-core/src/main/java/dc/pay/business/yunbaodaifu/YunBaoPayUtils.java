package dc.pay.business.yunbaodaifu;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;



public class YunBaoPayUtils {

	public static String CHARSET = "UTF-8";

	/**
	 * 加密业务数据
	 */
	public static String encrypt(String content,String PLATFORM_PUBLIC_KEY) throws Exception {
		//平台公钥
		PublicKey publicKey  = YunBaoRSAUtils.getPublicKey(PLATFORM_PUBLIC_KEY);
		return YunBaoBase64.encode(YunBaoRSAUtils.encrypt(publicKey, content.getBytes(CHARSET)));
	}



	/*
	 * 解密业务数据
	 */
	public static String decrypt(String cipher,String PRIVATE_KEY) throws Exception {
		//商户私钥
		PrivateKey privateKey = YunBaoRSAUtils.getPrivateKey(PRIVATE_KEY);
		byte[] plainBytes = YunBaoRSAUtils.decrypt(privateKey, YunBaoBase64.decode(cipher));
		return new String(plainBytes,CHARSET);
	}

	/**
	 * 商户私钥签名
	 */
	public static String sign(Map<String,String> params,String PRIVATE_KEY) throws Exception {
		//商户私钥
		PrivateKey privateKey = YunBaoRSAUtils.getPrivateKey(PRIVATE_KEY);
		//生成签名字符串
		StringBuilder sb = new StringBuilder();
	 
		//对key进行排序
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		for(Object key : keys) {
			sb.append("&").append(key).append("=").append(params.get(key));
		}
		
		String str = sb.substring(1);
		String sign = YunBaoBase64.encode(YunBaoRSAUtils.sign(privateKey, str.getBytes(CHARSET)));
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
		PublicKey publicKey  = YunBaoRSAUtils.getPublicKey(PLATFORM_PUBLIC_KEY);
		return YunBaoRSAUtils.verify(str.getBytes(CHARSET), publicKey, YunBaoBase64.decode(sign));
	}
}
