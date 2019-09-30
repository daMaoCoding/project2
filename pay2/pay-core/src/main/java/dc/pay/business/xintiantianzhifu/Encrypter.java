package dc.pay.business.xintiantianzhifu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;


/**
 */
public class Encrypter {
    /**
     * 编码方式统一为utf8
     **/
    private static String CHARSET = "utf-8";
    /**
     * 秘钥算法
     **/
    private static String KEY_ALGORITHM = "AES";
    /**
     * AES加密算法
     **/
    private static String AES_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    /**
     * RSA签名算法
     **/
    private static String RSA_SIGNALGORITHM = "SHA1WithRSA";
    /**
     * RSA加密算法
     **/
    private static String RSA_CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
    /****
     * keyLength 密钥bit长度
     **/
    private static int KEY_LENGTH = 2048;
    /**
     * reserveSize 填充字节数，预留11字节
     **/
    private static int RESEVE_SIZE = 11;
    /**
     * 私钥对象
     ***/
    private static PrivateKey privateKey = null;
    /**
     * 公钥对象
     ***/
    private static PublicKey publicKey = null;

    private static String RSA_NAME = "RSA";

    public Encrypter(final String key, final boolean encrypt) {
        try {
            if (!encrypt) {
                PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(key));
                KeyFactory keyFactoryPrivate = KeyFactory.getInstance(RSA_NAME,"SunRsaSign");
                this.privateKey = keyFactoryPrivate.generatePrivate(priPKCS8);
            } else {
                X509EncodedKeySpec pubX509 = new X509EncodedKeySpec(Base64.decodeBase64(key));
                KeyFactory keyFactoryPublic = KeyFactory.getInstance(RSA_NAME,"SunRsaSign");
                this.publicKey = keyFactoryPublic.generatePublic(pubX509);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Encrypter(final String publicKey,final String privateKey) {
    	
    	
        try {
        	
        	PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
            KeyFactory keyFactoryPrivate = KeyFactory.getInstance(RSA_NAME,"SunRsaSign");
            this.privateKey = keyFactoryPrivate.generatePrivate(priPKCS8);
        	
        	
            X509EncodedKeySpec pubX509 = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
            KeyFactory keyFactoryPublic = KeyFactory.getInstance(RSA_NAME,"SunRsaSign");
            this.publicKey = keyFactoryPublic.generatePublic(pubX509);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 数字签名函数入口
     *
     * @param plainBytes 待签名明文字节数组
     * @return 签名后的字节数组
     * @throws RuntimeException
     */
    public byte[] digitalSign(byte[] plainBytes) throws RuntimeException {
        try {
            Signature signature = Signature.getInstance(RSA_SIGNALGORITHM);
            signature.initSign(privateKey);
            signature.update(plainBytes);
            byte[] signBytes = signature.sign();
            return signBytes;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(String.format("数字签名时没有[%s]此类算法", RSA_SIGNALGORITHM));
        } catch (InvalidKeyException e) {
            throw new RuntimeException("数字签名时私钥无效");
        } catch (SignatureException e) {
            throw new RuntimeException("数字签名时出现异常");
        }
    }

    /**
     * 验证数字签名函数入口
     *
     * @param plainBytes 待验签明文字节数组
     * @param signBytes  待验签签名后字节数组
     * @return 验签是否通过
     * @throws RuntimeException
     */
    public boolean verifyDigitalSign(byte[] plainBytes, byte[] signBytes) throws RuntimeException {
        boolean isValid = false;
        try {
            Signature signature = Signature.getInstance(RSA_SIGNALGORITHM);
            signature.initVerify(publicKey);
            signature.update(plainBytes);
            isValid = signature.verify(signBytes);
            return isValid;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(String.format("验证数字签名时没有[%s]此类算法", RSA_SIGNALGORITHM));
        } catch (InvalidKeyException e) {
            throw new RuntimeException("验证数字签名时公钥无效");
        } catch (SignatureException e) {
            throw new RuntimeException("验证数字签名时出现异常");
        }
    }

    /**
     * 验证数字签名函数入口
     *
     * @param plainBytes 待验签明文字节数组
     * @param signBytes  待验签签名后字节数组
     * @return 验签是否通过
     * @throws RuntimeException
     */
    public boolean verifyDigitalSign(byte[] plainBytes, byte[] signBytes, X509Certificate cert) throws RuntimeException {
        boolean isValid = false;
        try {
            Signature signature = Signature.getInstance(RSA_SIGNALGORITHM);
            signature.initVerify(cert);
            signature.update(plainBytes);
            isValid = signature.verify(signBytes);
            return isValid;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(String.format("验证数字签名时没有[%s]此类算法", RSA_SIGNALGORITHM));
        } catch (InvalidKeyException e) {
            throw new RuntimeException("验证数字签名时公钥无效");
        } catch (SignatureException e) {
            throw new RuntimeException("验证数字签名时出现异常");
        }
    }

    /**
     * AES加密
     *
     * @return 加密后字节数组，不经base64编码
     * @throws RuntimeException
     */
    public byte[] AESEncrypt(String plainText, String keyPlain) {
        try {
            byte[] plainBytes = plainText.getBytes(CHARSET);
            byte[] keyBytes = keyPlain.getBytes(CHARSET);
            // AES密钥长度为128bit、192bit、256bit，默认为128bit
            if (keyBytes.length % 8 != 0 || keyBytes.length < 16 || keyBytes.length > 32) {
                throw new RuntimeException("AES密钥长度不合法");
            }

            Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
            /*if (StringUtils.trimToNull(IV) != null) {
                IvParameterSpec ivspec = new IvParameterSpec(IV.getBytes());
				cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
			} else {*/
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            //	}

            byte[] encryptedBytes = cipher.doFinal(plainBytes);

            return encryptedBytes;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有此类加密算法");
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("没有此类填充模式");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("无效密钥");
        } catch (BadPaddingException e) {
            throw new RuntimeException("错误填充模式");
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("解密块大小不合法");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * AES解密
     *
     * @param encryptedBytes 密文字节数组，不经base64编码
     * @param keyBytes       密钥字节数组
     * @return 解密后字节数组
     * @throws RuntimeException
     */
    public byte[] AESDecrypt(byte[] encryptedBytes, byte[] keyBytes)
            throws RuntimeException {
        try {
            // AES密钥长度为128bit、192bit、256bit，默认为128bit
            if (keyBytes.length % 8 != 0 || keyBytes.length < 16 || keyBytes.length > 32) {
                throw new RuntimeException("AES密钥长度不合法");
            }

            Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM,"SunJCE");
            SecretKey secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
			/*if (IV != null && StringUtils.trimToNull(IV) != null) {
				IvParameterSpec ivspec = new IvParameterSpec(IV.getBytes());
				cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
			} else {*/
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            //}

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return decryptedBytes;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有此类加密算法");
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("没有此类填充模式");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("无效密钥");
        } catch (Exception e) {
            throw new RuntimeException("错误填充模式");
        }
    }

    /**
     * RSA加密
     *
     * @return 加密后字节数组，不经base64编码
     * @throws RuntimeException
     */
    public byte[] RSAEncrypt(String plainTxt)
            throws RuntimeException {

        try {
            byte[] plainBytes = plainTxt.getBytes(CHARSET);
            int keyByteSize = KEY_LENGTH / 8; // 密钥字节数
            int encryptBlockSize = keyByteSize - RESEVE_SIZE; // 加密块大小=密钥字节数-padding填充字节数
            int nBlock = plainBytes.length / encryptBlockSize;// 计算分段加密的block数，向上取整
            if ((plainBytes.length % encryptBlockSize) != 0) { // 余数非0，block数再加1
                nBlock += 1;
            }
            Cipher cipher = Cipher.getInstance(RSA_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            // 输出buffer，大小为nBlock个keyByteSize
            ByteArrayOutputStream outbuf = new ByteArrayOutputStream(nBlock * keyByteSize);
            // 分段加密
            for (int offset = 0; offset < plainBytes.length; offset += encryptBlockSize) {
                int inputLen = plainBytes.length - offset;
                if (inputLen > encryptBlockSize) {
                    inputLen = encryptBlockSize;
                }

                // 得到分段加密结果
                byte[] encryptedBlock = cipher.doFinal(plainBytes, offset, inputLen);
                // 追加结果到输出buffer中
                outbuf.write(encryptedBlock);
            }

            outbuf.flush();
            outbuf.close();
            return outbuf.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有此类加密算法");
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("没有此类加密算法");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("无效密钥");
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("加密块大小不合法");
        } catch (BadPaddingException e) {
            throw new RuntimeException("错误填充模式");
        } catch (IOException e) {
            throw new RuntimeException("字节输出流异常");
        }
    }

    /**
     * RSA解密
     *
     * @param encryptedBytes 加密后字节数组
     * @return 解密后字节数组，不经base64编码
     * @throws RuntimeException
     */
    public byte[] RSADecrypt(byte[] encryptedBytes)
            throws RuntimeException {
        int keyByteSize = KEY_LENGTH / 8; // 密钥字节数
        int decryptBlockSize = keyByteSize - RESEVE_SIZE; // 解密块大小=密钥字节数-padding填充字节数
        int nBlock = encryptedBytes.length / keyByteSize;// 计算分段解密的block数，理论上能整除

        try {
            Cipher cipher = null;
            try {
                cipher = Cipher.getInstance(RSA_CIPHER_ALGORITHM,"SunJCE");
            } catch (NoSuchProviderException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // 输出buffer，大小为nBlock个decryptBlockSize
            ByteArrayOutputStream outbuf = new ByteArrayOutputStream(nBlock * decryptBlockSize);
            // 分段解密
            for (int offset = 0; offset < encryptedBytes.length; offset += keyByteSize) {
                // block大小: decryptBlock 或 剩余字节数
                int inputLen = encryptedBytes.length - offset;
                if (inputLen > keyByteSize) {
                    inputLen = keyByteSize;
                }

                // 得到分段解密结果
                byte[] decryptedBlock = cipher.doFinal(encryptedBytes, offset, inputLen);
                // 追加结果到输出buffer中
                outbuf.write(decryptedBlock);
            }

            outbuf.flush();
            outbuf.close();
            return outbuf.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有此类解密算法");
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("没有此类填充模式");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("无效密钥");
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("解密块大小不合法");
        } catch (BadPaddingException e) {
            throw new RuntimeException("错误填充模式");
        } catch (IOException e) {
            throw new RuntimeException("字节输出流异常");
        }
    }

    public static String getRandomString(int length) { //length表示生成字符串的长度
        String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
}