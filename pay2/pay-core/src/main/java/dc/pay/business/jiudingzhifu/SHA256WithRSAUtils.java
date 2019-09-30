package dc.pay.business.jiudingzhifu;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;


@Slf4j
public class SHA256WithRSAUtils {

    public static final String CHARSET = "UTF-8";
    //密钥算法
    public static final String ALGORITHM_RSA = "RSA";
    //RSA 签名算法
    public static final String ALGORITHM_RSA_SIGN = "SHA256WithRSA";
    public static final int ALGORITHM_RSA_PRIVATE_KEY_LENGTH = 2048;

    private SHA256WithRSAUtils() {
    }

    /**
     * 初始化RSA算法密钥对
     *
     * @param keysize RSA1024已经不安全了,建议2048
     * @return 经过Base64编码后的公私钥Map, 键名分别为publicKey和privateKey
     */
    public static Map<String, String> initRSAKey(int keysize) {
        if (keysize != ALGORITHM_RSA_PRIVATE_KEY_LENGTH) {
            throw new IllegalArgumentException("RSA1024已经不安全了,请使用" + ALGORITHM_RSA_PRIVATE_KEY_LENGTH + "初始化RSA密钥对");
        }
        //为RSA算法创建一个KeyPairGenerator对象
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance(ALGORITHM_RSA);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No such algorithm-->[" + ALGORITHM_RSA + "]");
        }
        //初始化KeyPairGenerator对象,不要被initialize()源码表面上欺骗,其实这里声明的size是生效的
        kpg.initialize(ALGORITHM_RSA_PRIVATE_KEY_LENGTH);
        //生成密匙对
        KeyPair keyPair = kpg.generateKeyPair();
        //得到公钥
        Key publicKey = keyPair.getPublic();
        String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        //得到私钥
        Key privateKey = keyPair.getPrivate();
        String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        Map<String, String> keyPairMap = new HashMap<String, String>();
        keyPairMap.put("publicKey", publicKeyStr);
        keyPairMap.put("privateKey", privateKeyStr);
        return keyPairMap;
    }

    /**
     * RSA算法公钥加密数据
     *
     * @param data 待加密的明文字符串
     * @param key  RSA公钥字符串
     * @return RSA公钥加密后的经过Base64编码的密文字符串
     */
    public static String buildRSAEncryptByPublicKey(String data, String key) {
        try {
            //通过X509编码的Key指令获得公钥对象
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
            Key publicKey = keyFactory.generatePublic(x509KeySpec);
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, data.getBytes(CHARSET)));
        } catch (Exception e) {
            throw new RuntimeException("加密字符串[" + data + "]时遇到异常", e);
        }
    }
    /**
     * RSA算法公钥解密数据
     *
     * @param data 待解密的经过Base64编码的密文字符串
     * @param key  RSA公钥字符串
     * @return RSA公钥解密后的明文字符串
     */
    public static String buildRSADecryptByPublicKey(String data, String key) {
        try {
            //通过X509编码的Key指令获得公钥对象
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
            Key publicKey = keyFactory.generatePublic(x509KeySpec);
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return new String(rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, Base64.getDecoder().decode(data)), CHARSET);
        } catch (Exception e) {
            throw new RuntimeException("解密字符串[" + data + "]时遇到异常", e);
        }
    }
    /**
     * RSA算法私钥加密数据
     *
     * @param data 待加密的明文字符串
     * @param key  RSA私钥字符串
     * @return RSA私钥加密后的经过Base64编码的密文字符串
     */
    public static String buildRSAEncryptByPrivateKey(String data, String key) {
        try {
            //通过PKCS#8编码的Key指令获得私钥对象
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
            Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return Base64.getEncoder().encodeToString(rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, data.getBytes(CHARSET)));
        } catch (Exception e) {
            throw new RuntimeException("加密字符串[" + data + "]时遇到异常", e);
        }
    }
    /**
     * RSA算法私钥解密数据
     * @param data 待解密的经过Base64编码的密文字符串
     * @param key  RSA私钥字符串
     * @return RSA私钥解密后的明文字符串
     */
    public static String buildRSADecryptByPrivateKey(String data, String key) {
        try {
            //通过PKCS#8编码的Key指令获得私钥对象
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
            Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, Base64.getDecoder().decode(data)), CHARSET);
        } catch (Exception e) {
            throw new RuntimeException("解密字符串[" + data + "]时遇到异常", e);
        }
    }
    /**
     * RSA算法使用私钥对数据生成数字签名
     *
     * @param data 待签名的明文字符串
     * @param key  RSA私钥字符串
     * @return RSA私钥签名后的经过Base64编码的字符串
     */
    public static String buildRSASignByPrivateKey(String data, String key) {
        try {
            //通过PKCS#8编码的Key指令获得私钥对象
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
            Signature signature = Signature.getInstance(ALGORITHM_RSA_SIGN);
            signature.initSign(privateKey);
            signature.update(data.getBytes(CHARSET));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("签名字符串[" + data + "]时遇到异常", e);
        }
    }

    /**
     * RSA算法使用公钥校验数字签名
     *
     * @param data 参与签名的明文字符串
     * @param key  RSA公钥字符串
     * @param sign RSA签名得到的经过Base64编码的字符串
     * @return true--验签通过,false--验签未通过
     */
    public static boolean buildRSAverifyByPublicKey(String data, String key, String sign) {
        try {
            //通过X509编码的Key指令获得公钥对象
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
            PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);
            Signature signature = Signature.getInstance(ALGORITHM_RSA_SIGN);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(CHARSET));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            throw new RuntimeException("验签字符串[" + data + "]时遇到异常", e);
        }
    }

    /**
     * RSA算法分段加解密数据
     *
     * @param cipher 初始化了加解密工作模式后的javax.crypto.Cipher对象
     * @param opmode 加解密模式,值为javax.crypto.Cipher.ENCRYPT_MODE/DECRYPT_MODE
     * @return 加密或解密后得到的数据的字节数组
     */
    private static byte[] rsaSplitCodec(Cipher cipher, int opmode, byte[] datas) {
        int maxBlock = 0;
        if (opmode == Cipher.DECRYPT_MODE) {
            maxBlock = ALGORITHM_RSA_PRIVATE_KEY_LENGTH / 8;
        } else {
            maxBlock = ALGORITHM_RSA_PRIVATE_KEY_LENGTH / 8 - 11;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] buff;
        int i = 0;
        try {
            while (datas.length > offSet) {
                if (datas.length - offSet > maxBlock) {
                    buff = cipher.doFinal(datas, offSet, maxBlock);
                } else {
                    buff = cipher.doFinal(datas, offSet, datas.length - offSet);
                }
                out.write(buff, 0, buff.length);
                i++;
                offSet = i * maxBlock;
            }
        } catch (Exception e) {
            throw new RuntimeException("加解密阀值为[" + maxBlock + "]的数据时发生异常", e);
        }
        byte[] resultDatas = out.toByteArray();
        IOUtils.closeQuietly(out);
        return resultDatas;
    }

//    public static void main(String[] args) {
//        String privateKey="MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCKqgxszb4ObuoJ5U8Ddz2ytOFFc81fmniRFo+jvNtrOegMUpd9gBPvMEaBzdeJC74Cg/HsAE7f9u24APLi0oLYZCwthhs01xTIhAis8h7IYCSdWrcbn8gpB7apSe4Ud0dS6zkuRcDTVGsLNrzNOMKHfT8S13dtsqDyDBBhSpkBwe9TkeLQB8K5ZjDYJ8uTtsrcey95eNFe22r8qAhv562yUBTG0PjZpQmiJWD0T6URLLzNYEOXEzsVVZgPKBNWSVZvMjLg32eFv/2Mw+x83n44w39FLx6E347A5hVQYWbBLuHDyFUzW/endOmMSj1YJmGDhctgEK+UIA1bNbexYo33AgMBAAECggEAZlZ6NRLjYeOZ9xO17OjkMDAu0gNVX2mx8eKkwENx7QEfsXiDNayBCdanMsWofQydf13B/lt72u9zIooQuDaFOw8zS6XeDnFudU582KcY8OmEHF4HJewW3bFDrk1R2OjvStMvsGbqmQ2EsxIC5bMuXrChDFbZXayn+/vLWwKjShetqPkN2cRHcKWaASqOnWOAnpgHm5VuGu2ttaR5K14pmMq7a0TOaj7lDYyHelWejCfqFFiWfYLefNj3oFVAfiNxwsxj8q42xWwPZ/Xzhn8p0cInja//1AMuNLIadyC4r6VR7cOIKm4F7XwCTCRCSmPbhDu5pOEA//pERFTTNtE7gQKBgQDdzbujAJRqkn0WwPtbKE7ZxR2KFjc4fM1LyPyODz4tbXhtXtZeMcjjsKn8pTpzbgj+Cfmhz9X8sKAqdxe1WJTtkgg5zbvPQ8A+Q0Su19LZMfFCuC0RCp1SX/asl4XeQe6fQZCft3AG7RgA5HjHET0/7Mpwb3C7A/xBwMfn51T0+wKBgQCgCuy9NmpG2bG/MEz1gDojYe08yKOGgTLp6v/UZcn+U6Oit37/sFe0vU7n9NMtkCLdhf2mqF1cNCUv+rzHkvtgG8FaNlsuozOMXuTNCJ6nj/IypMOnU8vV9DL9zUq5cUnny7HKwCTuS8FYZTjI75GfDDwrxIhhzOIkh2leQD+iNQKBgDV1xOgA18ToEeZOFUdfa8HpVLlXqW+gBQtjIhxLaD0iyYfy99A0R6s5hX8zg+cWemxgkx6BLZ5+I9yYX8qB00N/kyP7hmzqc4eORxutQVDATNo78gDNgiW8o4Pt8YIkehNAhk84s3O36bUtXD7+1Lh3pkN7WLx6tW5TvNsUUtHJAoGBAIYOoJ8dpYgTccAkRVKfRhO9Q2tW5SMVtgAayJCxcrGGfdseuVKT8+OBb0b83KedxJaqVf3zqcBCLaQy80543/dxSFS4k0hNjDBYjG7yeXMCMG4bdYgDuQpOsyfFfoI3UyDGjva2XDj/W8UfhKFLiz8ekIhY56SEaikPBEPerW7BAoGBAIW+5xD7BH4Z/w+GNrA5WFWNNH02+32AD/k6W59GQ+ejrFzCa9/SPa/7WEbBjKNWnzYl9pcdA0lP3LGEbKzrm6Zy+6lCHI6Hx/o4PbHaKTQg2jAIJdEUrAOKR44rjIY41a8wtgilfZA4I4zDSvJMPkMYOItIXjFCwHTxLLfw0CJp";
//        String publicKey="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiqoMbM2+Dm7qCeVPA3c9srThRXPNX5p4kRaPo7zbaznoDFKXfYAT7zBGgc3XiQu+AoPx7ABO3/btuADy4tKC2GQsLYYbNNcUyIQIrPIeyGAknVq3G5/IKQe2qUnuFHdHUus5LkXA01RrCza8zTjCh30/Etd3bbKg8gwQYUqZAcHvU5Hi0AfCuWYw2CfLk7bK3HsveXjRXttq/KgIb+etslAUxtD42aUJoiVg9E+lESy8zWBDlxM7FVWYDygTVklWbzIy4N9nhb/9jMPsfN5+OMN/RS8ehN+OwOYVUGFmwS7hw8hVM1v3p3TpjEo9WCZhg4XLYBCvlCANWzW3sWKN9wIDAQAB";
//        System.out.println("签名:"+buildRSASignByPrivateKey("ABCabc123测试", privateKey));
//        System.out.println("校验:"+buildRSAverifyByPublicKey("ABCabc123测试",publicKey,buildRSASignByPrivateKey("ABCabc123测试", privateKey)));
//
//    }

}
