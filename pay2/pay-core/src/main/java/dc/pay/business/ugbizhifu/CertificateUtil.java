package dc.pay.business.ugbizhifu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;


/**
 * 秘钥证书工具类
 */
public class CertificateUtil {

    /** 非对称密钥算法名称 **/
    public static final String KEY_ALGORITHM = "RSA";

    /** 初始化密钥长度 **/
    public static final int KEY_SIZE = 1024;

    /** RSA公钥 **/
    public static final String PUBLIC_KEY = "publicKey";

    /** RSA私钥 **/
    public static final String PRIVATE_KEY = "privateKey";

    /** 编码字符集 **/
    private static final String CHARSET = "UTF-8";

    private CertificateUtil() {
    }

    //开始
        public static String decryptByPrivateKey(String data, String key) {
            try {
                org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();
                byte[] decryptStr = decryptByPrivateKey(base64.decode(data), base64.decode(key));
                return new String(decryptStr, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        
        public static byte[] decryptByPrivateKey(byte[] data, byte[] key) {
            try {
                // 取得私钥
                PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(key);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA","SunRsaSign");
                // 生成私钥
                PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
                // 数据解密
                Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(),"SunJCE");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                int blockSize = cipher.getOutputSize(data.length);
                return doFinal(data, cipher, blockSize);
            } catch (Exception e) {
                e.printStackTrace();
            } 
            return null;
        }
        
        public static byte[] doFinal(byte[] decryptData, Cipher cipher, int blockSize)
                throws IllegalBlockSizeException, BadPaddingException, IOException {
            int offSet = 0;
            byte[] cache = null;
            int i = 0;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (decryptData.length - offSet > 0) {
                if (decryptData.length - offSet > blockSize) {
                    cache = cipher.doFinal(decryptData, offSet, blockSize);
                } else {
                    cache = cipher.doFinal(decryptData, offSet, decryptData.length - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * blockSize;
            }
            byte[] encryptedData = out.toByteArray();
            out.close();
            return encryptedData;
        }
    //结束
}