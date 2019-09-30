package dc.pay.business.hujingzhifu;


import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtil {

    /**
     * 从字符串中加载公钥
     *
     * @param publicKeyStr 公钥数据字符串
     * @throws Exception 加载公钥时产生的异常
     */
    public static RSAPublicKey loadPublicKey(String publicKeyStr) throws Exception {
        try {
            BASE64Decoder      base64Decoder = new BASE64Decoder();
            byte[]             buffer        = base64Decoder.decodeBuffer(publicKeyStr);
            KeyFactory         keyFactory    = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec       = new X509EncodedKeySpec(buffer);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此算法");
        } catch (InvalidKeySpecException e) {
            throw new Exception("公钥非法");
        } catch (IOException e) {
            throw new Exception("公钥数据内容读取错误");
        } catch (NullPointerException e) {
            throw new Exception("公钥数据为空");
        }
    }

    public static RSAPrivateKey loadPrivateKey(String privateKeyStr) throws Exception {
        try {
            BASE64Decoder       base64Decoder = new BASE64Decoder();
            byte[]              buffer        = base64Decoder.decodeBuffer(privateKeyStr);
            PKCS8EncodedKeySpec keySpec       = new PKCS8EncodedKeySpec(buffer);
            KeyFactory          keyFactory    = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此算法");
        } catch (InvalidKeySpecException e) {
            throw new Exception("私钥非法");
        } catch (IOException e) {
            throw new Exception("私钥数据内容读取错误");
        } catch (NullPointerException e) {
            throw new Exception("私钥数据为空");
        }
    }

    /**
     * 签名
     *
     * @param privateKey 私钥
     * @param signStr
     * @return
     * @throws SignatureException
     */
    public static String rsaSign(String privateKey, String signStr) throws SignatureException {
        try {
            RSAPrivateKey rsaPrivateKey = loadPrivateKey(privateKey);
            if (privateKey == null) {
                throw new Exception("加密公钥为空, 请设置");
            }
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(rsaPrivateKey);
            signature.update(signStr.getBytes());

            byte[] signed = signature.sign();
            String output = new BASE64Encoder().encode(signed);
            return output;
        } catch (Exception e) {
            throw new SignatureException("RSAcontent = " + signStr);
        }
    }

    /**
     * 验签
     */
    public static boolean rsaVerify(String publicKey, String sign, String signStr) {
        try {
            RSAPublicKey rsaPublicKey = loadPublicKey(publicKey);
            if (publicKey == null) {
                throw new Exception("解密私钥为空, 请设置");
            }
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(rsaPublicKey);
            signature.update(signStr.getBytes());

            //把签名反解析，并验证
            byte[] decodeSign = new BASE64Decoder().decodeBuffer(sign);
            return signature.verify(decodeSign);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
