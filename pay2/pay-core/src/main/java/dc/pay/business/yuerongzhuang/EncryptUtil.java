package dc.pay.business.yuerongzhuang;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * <p>
 * 字符串格式的密钥在未特殊说明情况下都为BASE64编码格式<br/>
 * 由于非对称加密速度极其缓慢，一般文件不使用它来加密而是使用对称加密，<br/>
 * 非对称加密算法可以用来对对称加密的密钥加密，这样保证密钥的安全也就保证了数据的安全
 * </p>
 * Created by hongshuiqiao on 2017/6/14.
 */
public class EncryptUtil {
    private final static int RSA_MAX_DECRYPT_BLOCK = 128;
    private final static int RSA_MAX_ENCRYPT_BLOCK = 117;

    /**
     * BASE64解密
     *
     * @param data 需要解密的密码字符串
     * @return
     * @throws Exception
     */
    public static byte[] decryptBASE64(String data) throws Exception {
        return Base64.decode(data);
    }

    /**
     * BASE64加密
     *
     * @param data 需要加密的字符数组
     * @return
     * @throws Exception
     */
    public static String encryptBASE64(byte[] data) throws Exception {
        return Base64.encode(data);
    }

    /**
     * 返回公钥对象
     *
     * @param publicKey
     * @return
     * @throws Exception
     */
    private static PublicKey getRsaPublicKey(String publicKey) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decryptBASE64(publicKey));
        PublicKey key = KeyFactory.getInstance("RSA","SunRsaSign").generatePublic(keySpec);
        return key;
    }

    /**
     * 返回私钥对象
     *
     * @param privateKey
     * @return
     * @throws Exception
     */
    private static PrivateKey getRsaPrivateKey(String privateKey) throws Exception {
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(decryptBASE64(privateKey));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA","SunRsaSign");
        PrivateKey key = keyFactory.generatePrivate(pkcs8KeySpec);
        return key;
    }

    /**
     * RSA加密
     *
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    private static byte[] encryptRSA(Key key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA","SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] resultBytes = cipher.doFinal(data);
        return resultBytes;
    }

    /**
     * RSA解密
     *
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    private static byte[] decryptRSA(Key key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA","SunJCE");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] resultBytes = cipher.doFinal(data);
        return resultBytes;
    }

    /**
     * 分段加密（因为RSA有长度限制，分段才能支持任意长度数据的RSA加密）
     *
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] fullEncryptRSA(Key key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA","SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] encryptedData = null;
        try {
            int offSet = 0;
            int i = 0;
            byte[] cache = null;
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > RSA_MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, RSA_MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * RSA_MAX_ENCRYPT_BLOCK;
            }
            encryptedData = out.toByteArray();
        } finally {
            out.close();
        }

        return encryptedData;
    }

    /**
     * 分段解密（因为RSA有长度限制，分段才能支持任意长度数据的RSA解密）
     *
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] fullDecryptRSA(Key key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA","SunJCE");
        cipher.init(Cipher.DECRYPT_MODE, key);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        int i = 0;
        byte[] cache = null;
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > RSA_MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, RSA_MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * RSA_MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * RSA公钥加密
     *
     * @param publicKey
     * @param data
     * @return
     * @throws Exception
     */
    public static String encryptRSAByPublicKey(String publicKey, String data) throws Exception {
        Key key = getRsaPublicKey(publicKey);
        byte[] dataBytes = data.getBytes("UTF-8");
//        byte[] resultBytes = encryptRSA(key, dataBytes);

        byte[] resultBytes = fullEncryptRSA(key, dataBytes);
        return encryptBASE64(resultBytes);
    }

    /**
     * RSA公钥解密
     *
     * @param publicKey
     * @param data
     * @return
     * @throws Exception
     */
    public static String decryptRSAByPublicKey(String publicKey, String data) throws Exception {
        Key key = getRsaPublicKey(publicKey);
        byte[] dataBytes = decryptBASE64(data);
//        byte[] resultBytes = decryptRSA(key, dataBytes);

        byte[] resultBytes = fullDecryptRSA(key, dataBytes);
        return new String(resultBytes, "UTF-8");
    }

    /**
     * RSA私钥加密
     *
     * @param privateKey
     * @param data
     * @return
     * @throws Exception
     */
    public static String encryptRSAByPrivateKey(String privateKey, String data) throws Exception {
        Key key = getRsaPrivateKey(privateKey);
        byte[] dataBytes = data.getBytes("UTF-8");
//        byte[] resultBytes = encryptRSA(key, dataBytes);

        byte[] resultBytes = fullEncryptRSA(key, dataBytes);
        return encryptBASE64(resultBytes);
    }

    /**
     * RSA私钥解密
     *
     * @param privateKey
     * @param data
     * @return
     * @throws Exception
     */
    public static String decryptRSAByPrivateKey(String privateKey, String data) throws Exception {
        Key key = getRsaPrivateKey(privateKey);
        byte[] dataBytes = decryptBASE64(data);
//        byte[] resultBytes = decryptRSA(key, dataBytes);

        byte[] resultBytes = fullDecryptRSA(key, dataBytes);
        return new String(resultBytes, "UTF-8");
    }

    /**
     * 用私钥对信息生成RSA数字签名
     *
     * @param data
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String rsaSignByPrivateKey(String data, String privateKey) throws Exception {
        PrivateKey key = getRsaPrivateKey(privateKey);
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initSign(key);
        signature.update(data.getBytes("UTF-8"));
        return encryptBASE64(signature.sign());
    }

    /**
     * 用公钥对信息进行RSA数字签名校验
     *
     * @param data
     * @param publicKey
     * @param sign
     * @return
     * @throws Exception
     */
    public static boolean rsaVerifyByPublicKey(String data, String publicKey, String sign) throws Exception {
        PublicKey key = getRsaPublicKey(publicKey);
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initVerify(key);
        signature.update(data.getBytes("UTF-8"));
        return signature.verify(decryptBASE64(sign));
    }
}
