package dc.pay.utils;

import org.apache.shiro.codec.Base64;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.Security;

/**
 * ************************
 * 快活城堡,解决默认jdk  jce_policy
 * 具体参考：https://www.ctolib.com/topics-107955.html
 * @author tony 3556239829
 */
public class BouncyCastleAES {
    private static final Charset utf8Charset = Charset.forName("UTF-8");

    public static final String UTF_8 = "UTF-8";
    public static final String AES = "AES";
    public static final String AES_ECB_PKCS5Padding = "AES/ECB/PKCS5Padding";
    public static final String RSA_ECB_PKCS1Padding = "RSA/ECB/PKCS1Padding";

    public static final String AES_CBC_PKCS7Padding = "AES/CBC/PKCS7Padding";
    public static final String BC = "BC";


    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    /**
     * 加密
     * @throws UnsupportedEncodingException
     */
    public static String encrypt(String content, String password) throws Exception {
       Key key = new SecretKeySpec(password.getBytes(UTF_8), AES);
        // Cipher in = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        //in.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        Cipher in = Cipher.getInstance(AES_ECB_PKCS5Padding);
        in.init(Cipher.ENCRYPT_MODE, key);//
        byte[] enc = in.doFinal(content.getBytes());
        return new String(Base64.encode(enc));
    }


    /**
     * 解密
     *
     * @param content  待解密内容
     * @param password 解密密钥
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String decrypt(String content, String password) throws Exception {
        Key key = new SecretKeySpec(password.getBytes(UTF_8), AES);
        Cipher out = Cipher.getInstance(AES_ECB_PKCS5Padding);// 创建密码器
        out.init(Cipher.DECRYPT_MODE, key);// 初始化
        //Cipher out = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        //out.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] dec = out.doFinal( Base64.decode(content.getBytes()));
        return new String(dec, UTF_8);
    }


    //--------------------------------------------------------------

    // placeholder, real IV should be somehow better
    public static byte[] loadIvBytes() {
        byte[] data = new byte[16];
        for (int i = 0; i < data.length; ++i) {
            data[i] = (byte)i;
        }
        return data;
    }

    public static PaddedBufferedBlockCipher getBcCipher(boolean forEncryption, String keyStr) {
        try {
            Key key = new SecretKeySpec(keyStr.getBytes(), "AES");
            byte[] iv = loadIvBytes();
            CipherParameters params = new ParametersWithIV(new KeyParameter(key.getEncoded()), iv);
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
            cipher.init(forEncryption, params);
            return cipher;
        } catch (Exception ex) {
            throw new RuntimeException("Cannot intialize Bouncy Castle cipher. " + ex.getMessage(), ex);
        }
    }

    public static String encode2(String value,String keyStr) {
        try {
            PaddedBufferedBlockCipher cipher = getBcCipher(true,keyStr);
            byte[] input = value.getBytes(utf8Charset);
            byte[] output = new byte[cipher.getOutputSize(input.length)];
            int len = cipher.processBytes(input, 0, input.length, output, 0);
            len += cipher.doFinal(output, len);
            return net.iharder.Base64.encodeBytes(output, 0, len);
        } catch (Exception e) {
            throw new RuntimeException("Data encryption failed. " + e.getMessage(), e);
        }
    }

    public static String decode2(String encodedStr,String keyStr) {
        try {
            PaddedBufferedBlockCipher cipher = getBcCipher(false,keyStr);
            byte[] encoded = net.iharder.Base64.decode(encodedStr);
            byte[] output = new byte[cipher.getOutputSize(encoded.length)];
            int len = cipher.processBytes(encoded, 0, encoded.length, output, 0);
            len += cipher.doFinal(output, len);
            return new String(output, 0, len, utf8Charset);
        } catch (Exception e) {
            throw new RuntimeException("Data decryption failed. " + e.getMessage(), e);
        }
    }

    //--------------------------------------------------------------

    public static String encode3(String value,String keyStr){    //默认：AES/ECB/PKCS5Padding ，AES_256
        try {
            byte[] key = keyStr.getBytes();
            PaddedBufferedBlockCipher encryptCipher = new PaddedBufferedBlockCipher(new AESEngine());
            encryptCipher.init(true, new KeyParameter(key));
            byte[] input = value.getBytes(utf8Charset);
            byte[] output = new byte[encryptCipher.getOutputSize(input.length)];
            int len = encryptCipher.processBytes(input, 0, input.length, output, 0);
            len += encryptCipher.doFinal(output, len);
            return  net.iharder.Base64.encodeBytes(output, 0, len);
        } catch (Exception e) {
            throw new RuntimeException("Data decryption failed. " + e.getMessage(), e);
        }

    }

    public static String decode3(String encodedStr,String keyStr){    //默认：AES/ECB/PKCS5Padding ，AES_256
        try {
            byte[] key = keyStr.getBytes();
            PaddedBufferedBlockCipher decryptCipher = new PaddedBufferedBlockCipher(new AESEngine());
            decryptCipher.init(false, new KeyParameter(key));

            byte[] encoded = net.iharder.Base64.decode(encodedStr);
            byte[] output = new byte[decryptCipher.getOutputSize(encoded.length)];
            int len = decryptCipher.processBytes(encoded, 0, encoded.length, output, 0);
            len += decryptCipher.doFinal(output, len);
            return new String(output, 0, len, utf8Charset);
        } catch (Exception e) {
            throw new RuntimeException("Data decryption failed. " + e.getMessage(), e);
        }

    }




    //----------------------------------------------

    public  void test() throws  Exception{
        Security.addProvider(new BouncyCastleProvider());

        final Cipher encryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "BC");
        encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec("7c54367a45b37a192abc2cd7f4520304".getBytes(), "AES"));
        final byte[] encrypt = encryptCipher.doFinal("This is my text".getBytes());
        System.out.println(new String(Base64.encode(encrypt)));

        final Cipher decryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "BC");
        decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec("7c54367a45b37a192abc2cd7f4520304".getBytes(), "AES"));
        final byte[] decrypt = decryptCipher.doFinal(encrypt);
        System.out.println(new String(decrypt));
    }




    public static void main(String[] args) throws Exception {
        String jsonStr ="{\"amount\":\"30000\",\"product\":\"20180911101810\",\"merAccount\":\"091fde3be727466ab36bb4eb22697bc4\",\"orderId\":\"20180911101810\",\"sign\":\"bb914f12769e0520f52743f5713bed077d2a0f57\",\"payWay\":\"ALIPAY\",\"productDesc\":\"20180911101810\",\"payType\":\"ALIPAY_H5\",\"merNo\":\"10001378\",\"userIp\":\"123.123.123.123\",\"notifyUrl\":\"http://66p.nsqmz6812.com:30000/respPayWeb/KZHIFU_BANK_WAP_ZFB_SM/\",\"time\":\"1536632290\",\"userType\":\"0\",\"productType\":\"01\"}";
        String merKey = "50b6d57f8d004e92a2a803dd099ae75b";

        String encrypt = encrypt(jsonStr, merKey);
        System.out.println(encrypt);

        String decrypt = decrypt(encrypt, merKey);
        System.out.println(decrypt);

        //----------------------------------

//        String encoded2 = encode2(jsonStr,merKey);
//        System.out.println(encoded2);
//
//        String decoded2 = decode2(encoded2,merKey);
//        System.out.println(decoded2);


        //----------------------------------

        String encoded3 = encode3(jsonStr,merKey);
        System.out.println(encoded3);

        String decoded3 = decode3(encoded3,merKey);
        System.out.println(decoded3);


    }

}
