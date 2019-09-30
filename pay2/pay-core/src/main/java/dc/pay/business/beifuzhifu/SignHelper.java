package dc.pay.business.beifuzhifu;

import java.security.MessageDigest;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


public class SignHelper {



	/**
	 * 按照key的字母默认排序asc
	 * 
	 * @param map
	 *            需要排序的map
	 * @return 生成结果string
	 */
	public static String sortSign(TreeMap<String, String> map) {
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String value = entry.getValue();
			if (!value.isEmpty()) {
				sb.append(String.format("%s%s", entry.getKey(), value));
			}
		}
		return sb.toString();
	}

	/**
	 * 随机数
	 * 
	 * @return 获取随字符串
	 */
	public static String genNonceStr() {
		Random random = new Random();
		return MD5(String.valueOf(random.nextInt(10000)));
	}

	/**
	 * MD5加密
	 * 
	 * @param string
	 * @return
	 */
	public final static String MD5(String string) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		try {
			byte[] btInput = string.getBytes();
			// 获得MD5摘要算法的 MessageDigest 对象
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			// 使用指定的字节更新摘要
			mdInst.update(btInput);
			// 获得密文
			byte[] md = mdInst.digest();
			// 把密文转换成十六进制的字符串形式
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			return new String(str);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
