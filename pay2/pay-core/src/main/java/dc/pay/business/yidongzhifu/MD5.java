package dc.pay.business.yidongzhifu;

import java.nio.charset.Charset;
import java.security.MessageDigest;

public class MD5 {

	private static final String MD5 = "MD5";

	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final char[] toDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };

	protected static char[] encodeHex(final byte[] data) {
		final int l = data.length;
		final char[] out = new char[l << 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
			out[j++] = toDigits[0x0F & data[i]];
		}
		return out;
	}

	/**
	 * 计算MD5
	 * 
	 * @param content
	 */
	public static String md5ToHex(String content) {
		try {
			MessageDigest md = MessageDigest.getInstance(MD5);
			md.update(content.getBytes(UTF_8));
			return new String(encodeHex(md.digest()));
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
