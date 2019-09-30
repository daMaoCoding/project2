package dc.pay.business.heyuan;

import java.nio.charset.StandardCharsets;

public class Base64 {



	/**
	 * 使用Base64加密算法加密字符串 return
	 */
	public static String encode(byte []  plainBytes) {
		byte[] b = plainBytes;
		org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();
		b = base64.encode(b);
		String s = new String(b,StandardCharsets.UTF_8);
		return s;
	}
	
	

	
	/**
	 * 使用Base64解密算法解密字符串 return
	 */
	public static byte [] decode(String encodeStr) {
		byte[] b = encodeStr.getBytes(StandardCharsets.UTF_8);
		org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();
		b = base64.decode(b);
		//String s = new String(b,StandardCharsets.UTF_8);
		return b;
	}

	/**
	 * 使用Base64加密算法加密字符串 return
	 */
	@Deprecated
	public static String encode(String plainText) {
		byte[] b = plainText.getBytes(StandardCharsets.UTF_8);
		org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();
		b = base64.encode(b);
		String s = new String(b,StandardCharsets.UTF_8);
		return s;
	}
	
	/**
	 * 使用Base64解密算法解密字符串 return
	 */
	@Deprecated
	public static byte [] decode(byte []  encodeBytes) {
		byte[] b = encodeBytes;
		org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();
		b = base64.decode(b);
		//String s = new String(b);
		return b;
	}

}