package dc.pay.business.xinbibaozhifu;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * DES加密/解密类
 * 
 * @author Nick
 *
 */
public class DESUtil {
	private static final String ASCII = "ASCII";

	private static final String UTF8 = "UTF-8";

	private String keys;

	public String getKeys() {
		return keys;
	}

	public void setKeys(String keys) {
		this.keys = keys;
	}

	public DESUtil() {
	}

	public DESUtil(String keys) {
		this.keys = keys;
	}

	public byte[] desEncrypt(byte[] plainText) throws Exception {
		// ECB算法
		// SecureRandom sr = new SecureRandom();
		// DESKeySpec dks = new DESKeySpec(key.getBytes(ASCII));
		// SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		// SecretKey key = keyFactory.generateSecret(dks);
		// Cipher cipher = Cipher.getInstance("DES");
		// cipher.init(Cipher.ENCRYPT_MODE, key, sr);

		// CBC算法
		IvParameterSpec zeroIv = new IvParameterSpec(keys.getBytes(ASCII));
		SecretKeySpec key = new SecretKeySpec(keys.getBytes(ASCII), "DES");
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
		byte encryptedData[] = cipher.doFinal(plainText);
		return encryptedData;
	}

	public byte[] desDecrypt(byte[] encryptText) throws Exception {
		// ECB算法
		// SecureRandom sr = new SecureRandom();
		// DESKeySpec dks = new DESKeySpec(key.getBytes(ASCII));
		// SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		// SecretKey key = keyFactory.generateSecret(dks);
		// Cipher cipher = Cipher.getInstance("DES");
		// cipher.init(Cipher.DECRYPT_MODE, key, sr);

		// CBC算法
		IvParameterSpec zeroIv = new IvParameterSpec(keys.getBytes(ASCII));
		SecretKeySpec skey = new SecretKeySpec(keys.getBytes(ASCII), "DES");
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, skey, zeroIv);
		byte decryptedData[] = cipher.doFinal(encryptText);
		return decryptedData;
	}

	/**
	 * 加密
	 * 
	 * @param input
	 * @return
	 * @throws Exception
	 */
	public String encrypt(String input) throws Exception {
		return parseByte2HexStr(desEncrypt(input.getBytes(UTF8)));
	}

	/**
	 * 解密
	 * 
	 * @param input
	 * @return
	 * @throws Exception
	 */
	public String decrypt(String input) throws Exception {
		byte[] result = parseHexStr2Byte(input);
		return new String(desDecrypt(result), UTF8);
	}

	/**
	 * 将16进制转换为二进制
	 * 
	 * @param hexStr
	 * @return
	 */
	public byte[] parseHexStr2Byte(String hexStr) {
		if(hexStr.length() < 1) {
			return null;
		}
		byte[] result = new byte[hexStr.length() / 2];
		for(int i = 0; i < hexStr.length() / 2; i++) {
			int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
			result[i] = (byte) (high * 16 + low);
		}
		return result;
	}

	/**
	 * 将二进制转换成16进制
	 * 
	 * @param buf
	 * @return
	 */
	public String parseByte2HexStr(byte buf[]) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < buf.length; i++) {
			String hex = Integer.toHexString(buf[i] & 0xFF);
			if(hex.length() == 1) {
				hex = '0' + hex;
			}
			sb.append(hex.toUpperCase());
		}
		return sb.toString();
	}

	public static void main(String args[]) {
		try {
			DESUtil d = new DESUtil("abcdefgh");
			String p = d.encrypt("agent=btx");
			System.out.println("密文:" + p);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}