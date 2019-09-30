package dc.pay.business.yunbaodaifu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;



/**
 * RSA算法
 * 
 */
public class YunBaoRSAUtils {

    private static final String SIGN_ALGORITHMS = "SHA1WithRSA";
    private static final String ENCRYPT_ALGORITHMS = "RSA/ECB/PKCS1Padding";
    

    public static byte[] sign(PrivateKey privateKey, byte[] contentBytes) throws Exception {
        java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
        signature.initSign(privateKey);
        signature.update(contentBytes);
        return signature.sign();
    }


    public static boolean verify(byte[] contentBytes, PublicKey publicKey, byte[] signBytes) throws Exception{
        java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
        signature.initVerify(publicKey);
        signature.update(contentBytes);
        boolean bverify = signature.verify(signBytes);
        return bverify;
    }
    
    /**
     * 公钥加密
     */
    public static byte[] encrypt(PublicKey publicKey, byte[] contentBytes) throws Exception {
    	//计算 rsa 密钥长度  (1024/2048)
    	int keyLen = ((RSAPublicKey)publicKey).getModulus().bitLength();
    	int keyByteSize = keyLen / 8;
        int nblocksize = keyByteSize - 11;
        int n = contentBytes.length / nblocksize + (contentBytes.length % nblocksize == 0 ? 0 : 1);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(n * keyByteSize );
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGORITHMS);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        
        for(int i = 0; i < n; i++) {
            int begin = i * nblocksize;
            int inputLen = contentBytes.length - begin;  
            inputLen = Math.min(inputLen, nblocksize);
            baos.write(cipher.doFinal(contentBytes,begin,inputLen));
        }
        return baos.toByteArray();
    }
    
    /**
     * 私钥解密
     */
    public static byte[] decrypt(PrivateKey privateKey, byte[] encdata) throws Exception {
    	
    	int keyLen = ((RSAPrivateKey)privateKey).getModulus().bitLength();
    	int keyByteSize = keyLen / 8;
    	
        int n = encdata.length / keyByteSize;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(encdata.length);
        
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGORITHMS);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        
        for(int i = 0; i < n; i++) {
            int begin = i * keyByteSize;
            int inputLen = encdata.length - begin;  
            inputLen = Math.min(inputLen, keyByteSize);
            baos.write(cipher.doFinal(encdata,begin,inputLen));
        }
        return baos.toByteArray();
    }
    
    /**
     * 获取 base64私钥
     */
    public static PrivateKey getPrivateKey(String key) throws Exception {
        byte[] keyBytes;
        keyBytes = YunBaoBase64.decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }
    
    /**
     * 加载文件获取私钥
     */
    public static PrivateKey getPrivateKeyByFile(File file) throws Exception {
    	byte[] keyBytes = Files.readAllBytes(file.toPath());
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
    }
    
    /**
     *  获取 base64编码 公钥
     */
    public static PublicKey getPublicKey(String key) throws Exception {
        return getPublicKey(YunBaoBase64.decode(key));
    }
    
    public static PublicKey getPublicKey(byte[] key) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }
    
    /**
     * 加载文件获取公钥
     */
    public static PublicKey getPublicKeyByFile(File file) throws Exception {
    	byte[] keyBytes = Files.readAllBytes(file.toPath());
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
    }
   
}