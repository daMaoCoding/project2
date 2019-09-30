package dc.pay.business.xinzhinengyundaifu.utils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PayUtils {

	private static class TrustAllCerts implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
	}
	private static class TrustAllHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
	private static SSLSocketFactory createSSLSocketFactory() {
		SSLSocketFactory ssfFactory = null;

		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null,  new TrustManager[] { new TrustAllCerts() }, new SecureRandom());

			ssfFactory = sc.getSocketFactory();
		} catch (Exception e) {
		}

		return ssfFactory;
	}
	/**
	 * 加密业务数据
	 * @param content
	 * @return
	 * @throws Exception
	 */
	public static String encrypt(String content,String platform_public_key) throws Exception {
		//平台公钥
		PublicKey publicKey  = RSAUtils.getPublicKey(platform_public_key);
		return Base64.encode(RSAUtils.encrypt(publicKey, content.getBytes(Config.CHARSET)));
	}
	
	/*
	 * 解密业务数据
	 */
	public static String decrypt(String cipher,String private_key) throws Exception {
		//商户私钥
		PrivateKey privateKey = RSAUtils.getPrivateKey(private_key);
		byte[] plainBytes = RSAUtils.decrypt(privateKey, Base64.decode(cipher));
		return new String(plainBytes,Config.CHARSET);
	}

	/**
	 * 商户私钥签名
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public static String sign(Map<String,String> params,String private_key) throws Exception {
		//商户私钥
		PrivateKey privateKey = RSAUtils.getPrivateKey(private_key);
		//生成签名字符串
		StringBuilder sb = new StringBuilder();
	 
		//对key进行排序
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		for(Object key : keys) {
			sb.append("&").append(key).append("=").append(params.get(key));
		}
		
		String str = sb.substring(1);
		System.out.println("签名字符串："+str);
		String sign = Base64.encode(RSAUtils.sign(privateKey, str.getBytes(Config.CHARSET)));
		return sign;
	}
	
	/**
	 * 平台公钥 验签
	 * @param params
	 * @return
	 * @throws Exception 
	 */
	public static boolean verify(Map<String,String> params,String sign,String platform_public_key) throws Exception {
		 
		//生成签名字符串
		StringBuilder sb = new StringBuilder();
		//对key进行排序
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		for(Object key : keys) {
		    System.out.println("key=======>"+key);
		    if (!"sign".equals(key)) {
		        sb.append("&").append(key).append("=").append(params.get(key));
            }
		}
		
		String str = sb.substring(1);
		System.out.println("验签字符串："+str);
		
		//平台公钥
		PublicKey publicKey  = RSAUtils.getPublicKey(platform_public_key);
		return RSAUtils.verify(str.getBytes(Config.CHARSET), publicKey, Base64.decode(sign));
	}
}
