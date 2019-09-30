package dc.pay.business.xinhongniuzhifu;

import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class RSAUtils {
    /**
     * 私钥进行签名
     *
     * @param singInfo
     * @param priKey
     * @return
     * @throws Exception
     */
    public static byte[] signMd5ByPriKey(String singInfo, String priKey) throws Exception {
        RSAPrivateKey pk        = loadPrivateKey(priKey);
        Signature     signature = Signature.getInstance("MD5withRSA");
        signature.initSign(pk);
        signature.update(singInfo.getBytes("utf-8"));
        byte[] signed = signature.sign();
        return signed;
    }

    public static boolean checkSignByPubkey(String oriInfo, byte[] sign, String pubKey) throws Exception {

        RSAPublicKey pk        = loadPublicKey(pubKey);
        Signature    signature = Signature.getInstance("MD5withRSA");
        signature.initVerify(pk);
        signature.update(oriInfo.getBytes("utf-8"));
        return signature.verify((sign));
    }

    public static byte[] encryptByPubKey(String content, String pubKey) throws Exception {
        RSAPublicKey pk     = loadPublicKey(pubKey);
        Cipher       cipher = Cipher.getInstance("RSA/NONE/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, pk);
        byte[] output = cipher.doFinal(content.getBytes("utf-8"));
        return output;
    }

    public static byte[] dencryptByPriKey(String content, String priKey) throws Exception {
        RSAPrivateKey pk     = loadPrivateKey(priKey);
        Cipher        cipher = Cipher.getInstance("RSA/NONE/OAEPPadding");
        cipher.init(Cipher.DECRYPT_MODE, pk);
        byte[] encryptedData = Base64.getDecoder().decode(content);
        return cipher.doFinal(encryptedData);
    }

    /**
     * 从字符串中加载公钥
     *
     * @param publicKeyStr 公钥数据字符串
     * @throws Exception 加载公钥时产生的异常
     */
    private static RSAPublicKey loadPublicKey(String publicKeyStr) throws Exception {

        BASE64Decoder      base64Decoder = new BASE64Decoder();
        byte[]             buffer        = base64Decoder.decodeBuffer(publicKeyStr);
        KeyFactory         keyFactory    = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec       = new X509EncodedKeySpec(buffer);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    private static RSAPrivateKey loadPrivateKey(String privateKeyStr) throws Exception {
        BASE64Decoder       base64Decoder = new BASE64Decoder();
        byte[]              buffer        = base64Decoder.decodeBuffer(privateKeyStr);
        PKCS8EncodedKeySpec keySpec       = new PKCS8EncodedKeySpec(buffer);
        KeyFactory          keyFactory    = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
