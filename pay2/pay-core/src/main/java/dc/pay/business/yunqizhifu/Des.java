package dc.pay.business.yunqizhifu;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * @author by Administrator  2017/11/29.
 */
public class Des {

    // 向量
    private final static String iv = "87654321";
    // 加解密统一使用的编码方式
    private final static String encoding = "UTF-8";
    /**
     * 3DES加密
     * @param plainText 普通文本
     * @param secretKey
     * @return
     * @throws Exception
     */
    public static String encode(String plainText,String secretKey){
        try{
            Key deskey = null;
            DESedeKeySpec spec = new DESedeKeySpec(secretKey.getBytes());
            SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
            deskey = keyfactory.generateSecret(spec);

            Cipher cipher = Cipher.getInstance("desede/CBC/PKCS5Padding");
            IvParameterSpec ips = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, deskey, ips);
            byte[] encryptData = cipher.doFinal(plainText.getBytes(encoding));
            //return Des3Base64.encode(encryptData);
			byte[] bb = Base64.getEncoder().encode(encryptData);
//            byte[] bb = Base64.encode(encryptData);
            return new String(bb,"UTF-8");
        }catch(Exception e){
            e.printStackTrace();
        }
        return plainText;

    }

	/**
	 * 3DES解密
	 *
	 * @param encryptText 加密文本
	 * @return
	 * @throws Exception
	 */
	public static String decode(String encryptText,String secretKey){
		try{
			Key deskey = null;
			DESedeKeySpec spec = new DESedeKeySpec(secretKey.getBytes());
			SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
			deskey = keyfactory.generateSecret(spec);
			Cipher cipher = Cipher.getInstance("desede/CBC/PKCS5Padding");
			IvParameterSpec ips = new IvParameterSpec(iv.getBytes());
			cipher.init(Cipher.DECRYPT_MODE, deskey, ips);

			//byte[] decryptData = cipher.doFinal(Des3Base64.decode(encryptText));
			byte[] decryptData = cipher.doFinal(Base64.getDecoder().decode(encryptText.getBytes("UTF-8")));
//			byte[] decryptData = cipher.doFinal(Base64.decode(encryptText.getBytes("UTF-8")));
			return new String(decryptData, encoding);
		}catch(Exception e){
			e.printStackTrace();
		}
		return encryptText;
	}

		/**
	 * 基于MD5算法的单向加密
	 *
	 * @param strSrc 明文
	 * @return 返回密文
	 */
	public static final String encryptMd5(String strSrc) {
		String outString = null;
		try {
			outString = Hex.encodeHexString((encodeMD5(strSrc.getBytes(encoding))));
		} catch (Exception e) {
			throw new RuntimeException("加密错误，错误信息：", e);
		}
		return outString;
	}
/**
	 * MD5加密
	 *
	 * @param data 待加密数据
	 * @return byte[] 消息摘要
	 * @throws Exception
	 */

	public static byte[] encodeMD5(byte[] data) throws Exception {
		// 初始化MessageDigest
		MessageDigest md = MessageDigest.getInstance("MD5");
		// 执行消息摘要
		return md.digest(data);
	}


	public static String  getMd5Data(Map<String,Object> map, String md5Key) throws Exception {

		if(map==null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (String key : map.keySet()) {
			sb.append(key + "=" + map.get(key) + "&");
		}
		//机构密钥
//		String workKey = "DC5C3E4AD3DB5F456C0833ED33DBE7D8";
		String signContent = sb.toString()+ md5Key;
		String mac = encryptMd5(signContent);

		return mac;
	}

	

		
}
