package dc.pay.business.bsdaifu;
import java.io.BufferedReader;  
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;  
import java.security.spec.InvalidKeySpecException;  
import java.security.spec.PKCS8EncodedKeySpec;  
import java.security.spec.RSAPublicKeySpec;  
import java.security.spec.X509EncodedKeySpec;  
  
import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;  
  
/** 
 * Created by wk on 2017/2/14. 
 */  
  
public class RSAUtils {  
    private static String RSA = "RSA";  
    /** *//**  
     * RSA最大加密明文大小  
     */    
    private static final int MAX_ENCRYPT_BLOCK = 117;   
    /** *//**  
    /** *//** 
     * RSA最大解密密文大小 
     */  
    private static final int MAX_DECRYPT_BLOCK = 128; 
    
    /** 
     * 随机生成RSA密钥对(默认密钥长度为1024) 
     * 
     * @return 
     */  
    public static KeyPair generateRSAKeyPair()  
    {  
        return generateRSAKeyPair(1024);  
    }  
  
    /** 
     * 随机生成RSA密钥对 
     * 
     * @param keyLength 
     *            密钥长度，范围：512～2048<br> 
     *            一般1024 
     * @return 
     */  
    public static KeyPair generateRSAKeyPair(int keyLength)  
    {  
        try  
        {  
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA);  
            kpg.initialize(keyLength);  
            return kpg.genKeyPair();  
        } catch (NoSuchAlgorithmException e)  
        {  
            e.printStackTrace();  
            return null;  
        }  
    }  
    
    /** *//** 
     * 加密算法RSA 
     */  
    public static final String KEY_ALGORITHM = "RSA";  
  
    /** *//** 
     * 签名算法 
     */  
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";  
  
    /** *//** 
     * 获取公钥的key 
     */  
    private static final String PUBLIC_KEY = "RSAPublicKey";  
      
    /** *//** 
     * 获取私钥的key 
     */  
    private static final String PRIVATE_KEY = "RSAPrivateKey";  
     
   
    /** 
     * 用公钥加密 <br> 
     * 每次加密的字节数，不能超过密钥的长度值减去11 
     * 
     * @param data 
     *            需加密数据的byte数据 
     * @param publicKey 公钥 
     * @return 加密后的byte型数据 
     */  
    public static byte[] encryptData(byte[] data, PublicKey publicKey)  
    {  
          
        try  
        {  
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding","SunJCE");
            // 编码前设定编码方式及密钥  
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);  
            // 传入编码数据并返回编码结果  
            int inputLen = data.length;    
            ByteArrayOutputStream out = new ByteArrayOutputStream();    
            int offSet = 0;    
            byte[] cache;    
            int i = 0;    
            // 对数据分段加密    
            while (inputLen - offSet > 0) {    
                if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {    
                    cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);    
                } else {    
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);    
                }    
                out.write(cache, 0, cache.length);    
                i++;    
                offSet = i * MAX_ENCRYPT_BLOCK;    
            }    
            byte[] encryptedData = out.toByteArray();    
            out.close();    
            return encryptedData;    
        } catch (Exception e)  
        {  
            e.printStackTrace();  
            return null;  
        }  
    }  
    
    
    
    
  
    /** 
     * 用私钥解密 
     * 
     * @param encryptedData 
     *            经过encryptedData()加密返回的byte数据 
     * @param privateKey 
     *            私钥 
     * @return 
     */  
    public static byte[] decryptData1(byte[] encryptedData, PrivateKey privateKey)  
    {  
        try  
        {  
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding","SunJCE");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);  
            return cipher.doFinal(encryptedData);  
        } catch (Exception e)  
        {     
            e.printStackTrace();  
            return null;  
        }  
    }  
  
    
    
    public static byte[] decryptData( byte[] encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding","SunJCE");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;

        for(int i = 0; inputLen - offSet > 0; offSet = i * MAX_DECRYPT_BLOCK) {
            byte[] cache;
            if(inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }

            out.write(cache, 0, cache.length);
            ++i;
        }

        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }
    
    /** 
     * 通过公钥byte[](publicKey.getEncoded())将公钥还原，适用于RSA算法 
     * 
     * @param keyBytes 
     * @return 
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeySpecException 
     */  
    public static PublicKey getPublicKey(byte[] keyBytes) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);  
        KeyFactory keyFactory = KeyFactory.getInstance(RSA,"SunRsaSign");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    /**
     * 通过私钥byte[]将公钥还原，适用于RSA算法
     *
     * @param keyBytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PrivateKey getPrivateKey(byte[] keyBytes) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchProviderException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA,"SunRsaSign");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    /**
     * 使用N、e值还原公钥
     *
     * @param modulus
     * @param publicExponent
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PublicKey getPublicKey(String modulus, String publicExponent)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        BigInteger bigIntModulus = new BigInteger(modulus);
        BigInteger bigIntPrivateExponent = new BigInteger(publicExponent);
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(bigIntModulus, bigIntPrivateExponent);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA,"SunRsaSign");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    /**
     * 使用N、d值还原私钥
     *
     * @param modulus
     * @param privateExponent
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PrivateKey getPrivateKey(String modulus, String privateExponent)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        BigInteger bigIntModulus = new BigInteger(modulus);
        BigInteger bigIntPrivateExponent = new BigInteger(privateExponent);
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(bigIntModulus, bigIntPrivateExponent);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA,"SunRsaSign");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);  
        return privateKey;  
    }  
  
    /** 
     * 从字符串中加载公钥 
     * 
     * @param publicKeyStr 
     *            公钥数据字符串 
     * @throws Exception 
     *             加载公钥时产生的异常 
     */  
    public static PublicKey loadPublicKey(String publicKeyStr) throws Exception  
    {  
        try  
        {  
        	//publicKeyStr = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDY/2Sapo/QPZftX/EaWAokEiuWqAv1ybrdxtQ569gBM4G5MxI4+Za+qIYSrOn1enRLJGVOUYM1qCzkrczW+9tLMontI1o6UaeapaTK4/Yz8psNjzTEsOtGP+mnerD2UDC7MU+V2Ot3bdWFSwZm5uod6fzw5iryKV72kB76RmXqeQIDAQAB";
            byte[] buffer = Base64Utils.decode(publicKeyStr);  
            KeyFactory keyFactory = KeyFactory.getInstance(RSA,"SunRsaSign");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);  
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);  
        } catch (NoSuchAlgorithmException e)  
        {  
            throw new Exception("无此算法");  
        } catch (InvalidKeySpecException e)  
        {  
            throw new Exception("公钥非法");  
        } catch (NullPointerException e)  
        {  
            throw new Exception("公钥数据为空");  
        }  
    }  
  
    /** 
     * 从字符串中加载私钥<br> 
     * 加载时使用的是PKCS8EncodedKeySpec（PKCS#8编码的Key指令）。 
     * 
     * @param privateKeyStr 
     * @return 
     * @throws Exception 
     */  
    public static PrivateKey loadPrivateKey(String privateKeyStr) throws Exception  
    {  
        try  
        {  
        	//privateKeyStr ="MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBANj/ZJqmj9A9l+1f8RpYCiQSK5aoC/XJut3G1Dnr2AEzgbkzEjj5lr6ohhKs6fV6dEskZU5RgzWoLOStzNb720syie0jWjpRp5qlpMrj9jPymw2PNMSw60Y/6ad6sPZQMLsxT5XY63dt1YVLBmbm6h3p/PDmKvIpXvaQHvpGZep5AgMBAAECgYEAjB8GiLKLZQ2Q4FA2sSsuiTJOgT9MUc+M4g61Fh3L4qKu3rcyyiWpCL5rrP2JKeGD3M3IuPT7xBcvvg7Yme4SIOCu11p31xnSCiqH6nnax/FAVKWPRW2E15F21x5kij2xsqx4WXS3KiUBXieL+HEbU9JjcRY2dGAt4FGxIazwE7ECQQD6pZ/w3S5M1tCv+N+ias67dv8Ye88BqQmeET11fZSNl9o8i/clLAXJ3oEsthpTMbLFpJBDrIrhnaEhrAnbsPqNAkEA3aHLoCSPfXlSgDZCm/Snf06n/taPGI6gdK3LGRMfiw2OJXX3MmRd2itlXH5bx0encvdeP1MOg+PDZekj3A3KnQJAGAeG5OWfibhSe3xlnEGXHjvTSvqbpvIYvPG0La5jbouvXXyhrguhZnARfELdFTq/g9k6B3LkQasGBp9itpAqBQJBANUSnY8iVwkMQHKet77zoKxV1FC9ueikBkLmaqF6rxKiP4xoMvUxZMFAgzw/BsE5dBSlGOjMUuIdcFdjomQGpkkCQEgEWwcgysEtmAhA7LfCiFO+nIzFR5G2KICFeiHKJIPSttg8aUKGs1Wg+/xGSoKohoGWFEjNLqL+p5YZEYC7BqM=";
            byte[] buffer = Base64Utils.decode(privateKeyStr);  
            // X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);  
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);  
            KeyFactory keyFactory = KeyFactory.getInstance(RSA,"SunRsaSign");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);  
        } catch (NoSuchAlgorithmException e)  
        {  
            throw new Exception("无此算法");  
        } catch (InvalidKeySpecException e)  
        {  
            throw new Exception("私钥非法");  
        } catch (NullPointerException e)  
        {  
            throw new Exception("私钥数据为空");  
        }  
    }  
  
    /** 
     * 从文件中输入流中加载公钥 
     * 
     * @param in 
     *            公钥输入流 
     * @throws Exception 
     *             加载公钥时产生的异常 
     */  
    public static PublicKey loadPublicKey(InputStream in) throws Exception  
    {  
        try  
        {  
            return loadPublicKey(readKey(in));  
        } catch (IOException e)  
        {  
            throw new Exception("公钥数据流读取错误");  
        } catch (NullPointerException e)  
        {  
            throw new Exception("公钥输入流为空");  
        }  
    }  
  
    /** 
     * 从文件中加载私钥 
     * 
     * @param in 
     *            私钥文件名 
     * @return 是否成功 
     * @throws Exception 
     */  
    public static PrivateKey loadPrivateKey(InputStream in) throws Exception  
    {  
        try  
        {  
            return loadPrivateKey(readKey(in));  
        } catch (IOException e)  
        {  
            throw new Exception("私钥数据读取错误");  
        } catch (NullPointerException e)  
        {  
            throw new Exception("私钥输入流为空");  
        }  
    }  
  
    /** 
     * 读取密钥信息 
     * 
     * @param in 
     * @return 
     * @throws IOException 
     */  
    private static String readKey(InputStream in) throws IOException  
    {  
        BufferedReader br = new BufferedReader(new InputStreamReader(in,"utf-8"));  
        
        String readLine = null;  
        StringBuilder sb = new StringBuilder();  
        while ((readLine = br.readLine()) != null)  
        {  
            if (readLine.charAt(0) == '-')  
            {  
                continue;  
            } else  
            {  
                sb.append(readLine);  
                sb.append('\r');  
            }  
        }  
        System.out.println("readKey: "+sb);
        return sb.toString();  
    }  
    
    

	public static String readTxt(String filePath) {
	 
	  try {
	    File file = new File(filePath);
	    if(file.isFile() && file.exists()) {
	      InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "utf-8");
	      BufferedReader br = new BufferedReader(isr);
	      
	      String readd="";  
	      StringBuffer sb=new StringBuffer();  
	      while ((readd=br.readLine())!=null) {  
	          sb.append(readd);  
	      }  
	      br.close();  
	      String keystr=sb.toString();  
	      System.out.println(keystr+" -----> key读取成功");  //读取出来的key是编码之后的 要进行转码
	      return keystr;
	    } else {
	      System.out.println("文件不存在!");
	    }
	  } catch (Exception e) {
	    System.out.println("文件读取错误!");
	  }
	  return null;
	  }
    
  
    /** 
     * 打印公钥信息 
     * 
     * @param publicKey 
     */  
    public static void printPublicKeyInfo(PublicKey publicKey)  
    {  
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;  
        System.out.println("----------RSAPublicKey----------");  
        System.out.println("Modulus.length=" + rsaPublicKey.getModulus().bitLength());  
        System.out.println("Modulus=" + rsaPublicKey.getModulus().toString());  
        System.out.println("PublicExponent.length=" + rsaPublicKey.getPublicExponent().bitLength());  
        System.out.println("PublicExponent=" + rsaPublicKey.getPublicExponent().toString());  
    }  
  
    public static void printPrivateKeyInfo(PrivateKey privateKey)  
    {  
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;  
        System.out.println("----------RSAPrivateKey ----------");  
        System.out.println("Modulus.length=" + rsaPrivateKey.getModulus().bitLength());  
        System.out.println("Modulus=" + rsaPrivateKey.getModulus().toString());  
        System.out.println("PrivateExponent.length=" + rsaPrivateKey.getPrivateExponent().bitLength());  
        System.out.println("PrivatecExponent=" + rsaPrivateKey.getPrivateExponent().toString());  
  
    }  
}  
	
	
