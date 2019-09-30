package dc.pay.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.util.ByteArrayBuffer;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * DES 算法
 * 
 * @author andrew
 * Dec 30, 2017
 */
public class DesUtil {

	private static final Log log = LogFactory.getLog(DesUtil.class);

	public static byte[] appendIntToByteArrayBuffer(int iSource, int iArrayLen,
			ByteArrayBuffer buffer) {
		byte[] bLocalArr = new byte[iArrayLen];
		for (int i = 0; (i < 4) && (i < iArrayLen); i++) {
			bLocalArr[i] = (byte) (iSource >> 8 * i & 0xFF);
		}
		buffer.append(bLocalArr, 0, bLocalArr.length);
		return bLocalArr;
	}

	public static byte[] appendLongToByteArrayBuffer(long iSource,
			int iArrayLen, ByteArrayBuffer buffer) {
		byte[] bLocalArr = new byte[iArrayLen];
		for (int i = 0; (i < 8) && (i < iArrayLen); i++) {
			bLocalArr[i] = (byte) (iSource >> 8 * i & 0xFF);
		}
		buffer.append(bLocalArr, 0, bLocalArr.length);
		return bLocalArr;
	}

	public static int byteArrayToInt(byte[] bRefArr) {
		int iOutcome = 0;
		byte bLoop;
		for (int i = 0; i < bRefArr.length; i++) {
			bLoop = bRefArr[i];
			iOutcome += (bLoop & 0xFF) << (8 * i);
		}
		return iOutcome;
	}

	public static long byteArrayToLong(byte[] bRefArr) {
		long lOutcome = 0;
		byte bLoop;
		for (int i = 0; i < bRefArr.length; i++) {
			bLoop = bRefArr[i];
			lOutcome += (bLoop & 0xFF) << (8 * i);
		}
		return lOutcome;
	}

	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	public static byte[] getMD5Byte(String str) {
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(str.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException e) {
			System.out.println("NoSuchAlgorithmException caught!");
			System.exit(-1);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		byte[] byteArray = messageDigest.digest();
		return byteArray;
	}

	public static String getMD5str(String str) {
		String result = byte2Hex(getMD5Byte(str));
		return result;
	}

	public static String desEncrypt(String src, String key) throws Exception {
		if(src==null || src.length()==0)
			return "";
		log.debug("desEncrypt:"+src);
		DESKeySpec ks = new DESKeySpec(key.getBytes("UTF-8"));
		SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
		SecretKey sk = skf.generateSecret(ks);
		Cipher cip = Cipher.getInstance("DES/CBC/PKCS5Padding");// Cipher.getInstance("DES");
		byte[] IV = key.getBytes("UTF-8");
		IvParameterSpec iv2 = new IvParameterSpec(IV);
		cip.init(Cipher.ENCRYPT_MODE, sk, iv2);// IV�ķ�ʽb
		byte[] input = cip.doFinal(src.getBytes("UTF-8"));
		log.debug(input);
		String dest = byte2Hex(input);
		log.debug("desEncrypt:"+dest);
		return dest;
	}

	public static String desDecrypt(String src, String key) throws Exception {
		if(src==null || src.length()==0)
			return "";
		log.debug("deDecrypt:"+src);
		DESKeySpec ks = new DESKeySpec(key.getBytes("UTF-8"));
		SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
		SecretKey sk = skf.generateSecret(ks);
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		byte[] IV = key.getBytes("UTF-8");
		IvParameterSpec iv2 = new IvParameterSpec(IV);
		cipher.init(Cipher.DECRYPT_MODE, sk, iv2);// IV�ķ�ʽ
		byte[] data = hex2Byte(src);
		byte decryptedData[] = cipher.doFinal(data);
		String strResult=new String(decryptedData, "UTF-8");
		log.debug("deDecrypt:"+strResult);
		return strResult;
	}





	public static String des3Decrypt(String src, String key) throws Exception {
		if(src==null || src.length()==0)
			return "";
		DESKeySpec ks = new DESKeySpec(key.getBytes("UTF-8"));
		SecretKeyFactory skf = SecretKeyFactory.getInstance("DESede");
		SecretKey sk = skf.generateSecret(ks);
		Cipher cipher = Cipher.getInstance("DESede");
		byte[] IV = key.getBytes("UTF-8");
		IvParameterSpec iv2 = new IvParameterSpec(IV);
		cipher.init(Cipher.DECRYPT_MODE, sk, iv2);// IV�ķ�ʽ
		byte[] data = hex2Byte(src);
		byte decryptedData[] = cipher.doFinal(data);
		return new String(decryptedData, "UTF-8");
	}

	public static String byte2Hex(byte[] b) {
		String hs = "";
		String stmp = "";
		for (int n = 0; n < b.length; n++) {
			stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
			if (stmp.length() == 1) {
				hs = hs + "0" + stmp;
			} else {
				hs = hs + stmp;
			}
		}
		return hs.toUpperCase();
	}

	public static byte[] hex2Byte(String hexString) {
		if (hexString.length() % 2 == 1) {
			return null;
		}
		byte[] ret = new byte[hexString.length() / 2];
		for (int i = 0; i < hexString.length(); i += 2) {
			ret[i / 2] = Integer.decode("0x" + hexString.substring(i, i + 2))
					.byteValue();
		}
		return ret;
	}

	public static int stringToByte(String in, byte[] b) throws Exception {
		if (b.length < in.length() / 2) {
			throw new Exception("byte array too small");
		}
		int j = 0;
		StringBuffer buf = new StringBuffer(2);
		for (int i = 0; i < in.length(); i++, j++) {
			buf.insert(0, in.charAt(i));
			buf.insert(1, in.charAt(i + 1));
			int t = Integer.parseInt(buf.toString(), 16);
			System.out.println("byte hex value:" + t);
			b[j] = (byte) t;
			i++;
			buf.delete(0, 2);
		}
		return j;
	}

	 public static String encryption(String plainText) {
	        String re_md5 = "";
	        try {
	            MessageDigest md = MessageDigest.getInstance("MD5");
	            md.update(plainText.getBytes());
	            byte b[] = md.digest();
	            int i;
	            StringBuffer buf = new StringBuffer("");
	            for (int offset = 0; offset < b.length; offset++) {
	                i = b[offset];
	                if (i < 0)
	                    i += 256;
	                if (i < 16)
	                    buf.append("0");
	                buf.append(Integer.toHexString(i));
	            }
	            re_md5 = buf.toString();
	        } catch (NoSuchAlgorithmException e) {
	            e.printStackTrace();
	        }
	        return re_md5.toUpperCase();
	    }


//-----------------------DES DES/CBC/PKCS7Padding  by tony------------------------

	private static final String Algorithm = "DESede";

	public static byte[] desEncrypt(byte[] bytes, byte[] key,byte[] iv) throws Exception {
		if (bytes == null || key == null) return null;
		//Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"),new IvParameterSpec(iv));
		return cipher.doFinal(bytes);
	}

	public static byte[] desDecrypt(byte[] bytes, byte[] key,byte[] iv) throws Exception {
		if (bytes == null || key == null) return null;
		//Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DES"),new IvParameterSpec(iv));
		return cipher.doFinal(bytes);
	}


	public static String desEncrypt64(String str, String key,String iv) throws Exception {
		iv =  iv.substring(0,8);
		byte[] bytes = desEncrypt(str.getBytes("utf-8"),key.getBytes("utf-8"),iv.getBytes("utf-8"));
		return new BASE64Encoder().encode(bytes);
	}



	public static String desDecrypt64(String str, String key,String iv) throws Exception {
		byte[] bytes = new BASE64Decoder().decodeBuffer(str);
		iv =  iv.substring(0,8);
		bytes = desDecrypt(bytes,key.getBytes("utf-8"),iv.getBytes("utf-8"));
		return new String(bytes, "utf-8");
	}


	//加密方法 str为传输的值 key取商户私钥字符串的前16位
	public static String aesEncrypt(String str, String key) throws Exception {
		if (str == null || key == null) return null;
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes("utf-8"), "AES"));
		byte[] bytes = cipher.doFinal(str.getBytes("utf-8"));
		return new BASE64Encoder().encode(bytes);
	}

	public static String aesDecrypt(String str, String key) throws Exception {
		if (str == null || key == null) return null;
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes("utf-8"), "AES"));
		byte[] bytes = new BASE64Decoder().decodeBuffer(str);
		bytes = cipher.doFinal(bytes);
		return new String(bytes, "utf-8");
	}


	/*
	 * 加密
	 */
	public static byte[] encryptMode (byte[] keybyte, byte[] src) {
		try {
			SecretKey deskey = new SecretKeySpec (keybyte, Algorithm);
			Cipher c1 = Cipher.getInstance ("DESede/ECB/PKCS5Padding"); //DESede/ECB/PKCS5Padding, DES模式：CBC填充方式：PKCS7Padding
			c1.init (Cipher.ENCRYPT_MODE, deskey);

			return c1.doFinal(src);
		} catch (java.security.NoSuchAlgorithmException e1) {
			e1.printStackTrace ();
		} catch (javax.crypto.NoSuchPaddingException e2) {
			e2.printStackTrace ();
		} catch (java.lang.Exception e3) {
			e3.printStackTrace ();
		}
		return null;
	}

	/*
	 * 解密
	 */
	public static byte[] decryptMode (byte[] keybyte, byte[] src) {
		try {
			SecretKey deskey = new SecretKeySpec (keybyte, Algorithm);
			Cipher c1 = Cipher.getInstance ("DESede/ECB/PKCS5Padding"); //DESede/CBC/PKCS7Padding,DESede/ECB/PKCS5Padding
			c1.init (Cipher.DECRYPT_MODE, deskey);
			return c1.doFinal (src);
		} catch (java.security.NoSuchAlgorithmException e1) {
			e1.printStackTrace ();
		} catch (javax.crypto.NoSuchPaddingException e2) {
			e2.printStackTrace ();
		} catch (java.lang.Exception e3) {
			e3.printStackTrace ();
		}
		return null;
	}

	public void  testDes64() throws Exception {
		String str = "测试加密内容";
		String fullKey="114043565CB7FDCF17EB91E527604C4D";

		String key = fullKey.substring(fullKey.length()-8);
		String iv = fullKey.substring(0,24);

		String data64Enc = DesUtil.desEncrypt64(str, key, iv);
		String encode = URLEncoder.encode(data64Enc, "utf-8");
		System.out.println(encode);
		//------------------------------------------
		String data64Desc = URLDecoder.decode(encode, "utf-8");
		String decode = DesUtil.desDecrypt64(data64Desc, key, iv);
		System.out.println(decode);
	}
//-----------------------DES DES/CBC/PKCS7Padding  by tony------------------------






	public static void main(String[] args){
		 String strData="ABCDEFG";
		 try {
			 String strData1=DesUtil.desEncrypt(strData, "clcnptxr");
			System.out.println(strData1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	 } 
}