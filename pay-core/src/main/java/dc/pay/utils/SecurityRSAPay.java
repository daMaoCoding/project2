package dc.pay.utils;


import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public abstract class SecurityRSAPay {
	/**
	 * 非对称加密密钥算法
	 */
	public static final String KEY_ALGORITHM = "RSA";
	
	/**
	 * 数字签名
	 * 签名/验证算法
	 */
	public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
	
	public static final String  CIPHERPROVIDER = "SunJCE";
	/**
	 * 签名
	 * 
	 * @param data
	 *            待签名数据
	 * @param privateKey
	 *            私钥
	 * @return byte[] 数字签名
	 * @throws Exception
	 */
	public static byte[] sign( byte[] data, byte[] privateKey) throws Exception {
		// 取私钥匙对象
		PrivateKey priKey = getPK8PrivateKey(privateKey, KEY_ALGORITHM);

		// 实例化Signature
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);

		// 初始化Signature
		signature.initSign(priKey);
		
		// 更新
		signature.update(data);

		// 签名
		return signature.sign();
	}

	/**
	 * 校验
	 * 
	 * @param data
	 *            待校验数据
	 * @param publicKey
	 *            公钥
	 * @param sign
	 *            数字签名
	 * 
	 * @return boolean 校验成功返回true 失败返回false
	 * @throws Exception
	 * 
	 */
	public static boolean verify(byte[] data, byte[] publicKey, byte[] sign)
			throws Exception {

		// 生成公钥
		PublicKey pubKey = getX509PublicKey(publicKey, KEY_ALGORITHM);

		// 实例化Signature
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);

		// 初始化Signature
		signature.initVerify(pubKey);

		// 更新
		signature.update(data);

		// 验证
		return signature.verify(sign);
	}


	/**
	 * 公钥加密
	 * 
	 * @param data
	 *            待加密数据
	 * @param key
	 *            公钥
	 * @return byte[] 加密数据
	 * @throws Exception
	 */
	public static byte[] encryptByPublicKey( byte[] data, byte[] publicKey){
	    try {
			Key key = getX509PublicKey(publicKey, KEY_ALGORITHM);
			return  encrypt("RSA/ECB/PKCS1Padding", key,data, 64);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    return  null;
	}

	/**
	 * 私钥解密
	 * 
	 * @param data
	 *            待解密数据
	 * @param key
	 *            私钥
	 * @return byte[] 解密数据
	 * @throws Exception
	 */
	public static byte[] decryptByPrivateKey(byte[] data, byte[] privateKey){
		try{
			Key key = getPK8PrivateKey(privateKey, KEY_ALGORITHM);
			return decrypt("RSA/ECB/PKCS1Padding", key, data,128);
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			//LogUtils.getInstance().addDaoExceptionLog(e, "RSA解密数据");
			return null;
		}
	}

	/**
     * 私钥解密
     * 
     * @param data
     *            待解密数据
     * @param key
     *            私钥
     * @return byte[] 解密数据
     * @throws Exception
     */
    public static String decryptByPrivateKey2(byte[] data, byte[] privateKey){
        try{
            Key key = getPK8PrivateKey(privateKey, KEY_ALGORITHM);
            byte[] result = decrypt2("RSA/ECB/PKCS1Padding", key, data,256);
            if(result.length>0){
                return new String(result);    
            }
            return "";
            
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            //LogUtils.getInstance().addDaoExceptionLog(e, "RSA解密数据");
            return null;
        }
    }
	
	/**
	 * 私钥加密
	 * 
	 * @param data
	 *            待加密数据
	 * @param key
	 *            私钥
	 * @return byte[] 加密数据
	 * @throws Exception
	 */
	public static byte[] encryptByPrivateKey(byte[] data, byte[] privateKey) {
	    try {
			Key key = getPK8PrivateKey(privateKey, KEY_ALGORITHM);
			return  encrypt("RSA/ECB/PKCS1Padding", key,data, 117);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return  null;
	}
	
	
 
	/**
	 * 公钥解密
	 * 
	 * @param data
	 *            待解密数据
	 * @param key
	 *            公钥
	 * @return byte[] 解密数据
	 * @throws Exception
	 */
	public static byte[] decryptByPublicKey(byte[] data, byte[] publicKey){
		try{
			Key key = getX509PublicKey(publicKey, KEY_ALGORITHM);
			return decrypt("RSA/ECB/PKCS1Padding", key, data,128);
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			//LogUtils.getInstance().addDaoExceptionLog(e, "RSA解密数据");
			return null;
		}
	}
	
	private static PublicKey getX509PublicKey(byte pem[], String algorithm)
			throws Exception {
		PublicKey publicKey = null;
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(pem);
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
		publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
		return publicKey;
	}

	private static PrivateKey getPK8PrivateKey(byte pem[], String algorithm)
			throws Exception {
		PrivateKey privateKey = null;
		PKCS8EncodedKeySpec pKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(pem);
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
		privateKey = keyFactory.generatePrivate(pKCS8EncodedKeySpec);
		return privateKey;
	}

	private static byte[] encrypt(String transformation, Key key, byte data[],
			int blockSize) throws Exception {
		Cipher cipher = Cipher.getInstance(transformation);
		cipher.init(1, key);
		return finalCryption(cipher, data, blockSize);
	}

	private static byte[] decrypt(String transformation, Key key, byte data[],
			int blockSize) throws Exception {
		Cipher cipher = Cipher.getInstance(transformation);
		cipher.init(2, key);
		return finalCryption(cipher, data, blockSize);
	}
	
	private static byte[] decrypt2(String transformation, Key key, byte data[],
            int blockSize) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation,CIPHERPROVIDER);
        cipher.init(2, key);
        return finalCryption(cipher, data, blockSize);
    }

	private static byte[] finalCryption(Cipher cipher, byte data[],
			int blockSize) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte abyte0[];
		int dataLen = data.length;
		int blockNum = dataLen / blockSize;
		int lastSize = dataLen % blockSize;
		int off = 0;
		for (int i = 0; i < blockNum; i++) {
			baos.write(cipher.doFinal(data, off, blockSize));
			off += blockSize;
		}

		if (lastSize > 0)
			baos.write(cipher.doFinal(data, off, lastSize));
		abyte0 = baos.toByteArray();
		try {
			baos.close();
		} catch (Exception exception1) {
		}
		return abyte0;

	}
	
}
