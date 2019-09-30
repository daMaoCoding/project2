package dc.pay.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * md5加密(未使用base64编码)
 */
public class MD5Util {
    private static final char DIGITS_LOWER[] = { '0', '1', '2', '3', '4', '5','6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static final char DIGITS_UPPER[] = { '0', '1', '2', '3', '4', '5','6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	private static final String DEFAULT_ENCODING = "UTF8";
	private static final String CHAR_SET = "UTF-8";
	private static final String ALGORITH = "MD5";
	private static final MessageDigest md = getMessageDigest(ALGORITH);

	/**
	 * MD5(32位的十六进制表示)
	 * 
	 * @param srcStr
	 *            源字符串
	 * @param encode
	 *            编码方式
	 * @return
	 */
	public static String digest(String srcStr, String encode) {
		byte[] rstBytes;
		try {
			rstBytes = md.digest(srcStr.getBytes(encode));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		return toHex(rstBytes, true);
	}

	/**
	 * 默认使用UTF8编码进行解析
	 * 
	 * @param srcStr
	 *            源字符串
	 * @return
	 */
	public static String digest(String srcStr) {
		return digest(srcStr, DEFAULT_ENCODING);
	}

	/**
	 * 获取摘要处理实例
	 * 
	 * @param algorithm
	 *            摘要的算法
	 * @return
	 */
	private static MessageDigest getMessageDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 字节码转十六进制的字符串
	 * 
	 * @param bytes
	 *            字节数组
	 * @param flag
	 *            大小写标志,true为小写，false为大写
	 * @return
	 */
	public static String toHex(byte[] bytes, boolean flag) {
		return new String(processBytes2Hex(bytes, flag ? DIGITS_LOWER : DIGITS_UPPER));
	}

	/**
	 * 将字节数组转化为十六进制字符串
	 * 
	 * @param bytes
	 *            字节数组
	 * @param digits
	 *            数字加字母字符数组
	 * @return
	 */
	private static char[] processBytes2Hex(byte[] bytes, char[] digits) {
		// bytes.length << 1肯定是32，左移1位是因为一个字节是8位二进制，一个十六进制用4位二进制表示
		// 当然可以写成l = 32，因为MD5生成的字节数组必定是长度为16的字节数组
		int l = bytes.length << 1;
		char[] rstChars = new char[l];
		int j = 0;
		for (int i = 0; i < bytes.length; i++) {
			// 得到一个字节中的高四位的值
			rstChars[j++] = digits[(0xf0 & bytes[i]) >>> 4];
			// 得到一个字节中低四位的值
			rstChars[j++] = digits[0x0f & bytes[i]];
		}
		return rstChars;
	}
	public static String encode(byte[] source) {
		String s = null;
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			md.update(source);
			byte tmp[] = md.digest(); 
			char str[] = new char[16 * 2]; 
			int k = 0; 
			for (int i = 0; i < 16; i++) { 
				byte byte0 = tmp[i]; 
				str[k++] = hexDigits[byte0 >>> 4 & 0xf]; 
				str[k++] = hexDigits[byte0 & 0xf]; 
			}
			s = new String(str); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}


	public static String signByMD5(String sourceData, String key) throws Exception {
		String data = sourceData + key;
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		byte[] sign = md5.digest(data.getBytes(CHAR_SET));

		return Bytes2HexString(sign).toUpperCase();
	}

	/**
	 * 将byte数组转成十六进制的字符串
	 *
	 * @param b
	 *            byte[]
	 * @return String
	 */
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



	/**
	 * @param decript 要加密的字符串
	 * @return 加密的字符串
	 * MD5加密
	 */
	public final static String MD5(String decript) {
		char hexDigits[] = { // 用来将字节转换成 16 进制表示的字符
				'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
				'e', 'f'};
		try {
			byte[] strTemp = decript.getBytes();
			MessageDigest mdTemp = MessageDigest.getInstance("MD5");
			mdTemp.update(strTemp);
			byte tmp[] = mdTemp.digest(); // MD5 的计算结果是一个 128 位的长整数，
			// 用字节表示就是 16 个字节
			char strs[] = new char[16 * 2]; // 每个字节用 16 进制表示的话，使用两个字符，
			// 所以表示成 16 进制需要 32 个字符
			int k = 0; // 表示转换结果中对应的字符位置
			for (int i = 0; i < 16; i++) { // 从第一个字节开始，对 MD5 的每一个字节
				// 转换成 16 进制字符的转换
				byte byte0 = tmp[i]; // 取第 i 个字节
				strs[k++] = hexDigits[byte0 >>> 4 & 0xf]; // 取字节中高 4 位的数字转换,
				// >>> 为逻辑右移，将符号位一起右移
				strs[k++] = hexDigits[byte0 & 0xf]; // 取字节中低 4 位的数字转换
			}
			return new String(strs).toUpperCase(); // 换后的结果转换为字符串
		} catch (Exception e) {
			return null;
		}
	}


}
