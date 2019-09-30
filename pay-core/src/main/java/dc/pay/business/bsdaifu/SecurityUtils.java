package dc.pay.business.bsdaifu;

import java.io.FileInputStream;  
import java.security.PrivateKey;  
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;


import com.alibaba.fastjson.JSONObject;

  
public class SecurityUtils {  
    
    
    public static String decrypt(String cipherText ,String priKey) throws Exception{  
        // 从文件中得到私钥  
       PrivateKey privateKey = RSAUtils.loadPrivateKey(priKey);
       byte[] decryptByte = RSAUtils.decryptData(Base64Utils.decode(cipherText), privateKey);  
       String decryptStr = new String(decryptByte,"utf-8");  
       return decryptStr;  
   }  
   /** 
    * 加密 
    * @param plainTest 明文 
    * @return  返回加密后的密文 
    * @throws Exception  
    */  
   public static String encrypt(String plainTest ,String pubKey) throws Exception{  
       PublicKey publicKey = RSAUtils.loadPublicKey(pubKey);
       // 加密  
       byte[] encryptByte = RSAUtils.encryptData(plainTest.getBytes("utf-8"), publicKey);  
       String afterencrypt = Base64Utils.encode(encryptByte);  
       //afterencrypt = URLEncoder.encode(afterencrypt, "utf-8");
       return afterencrypt;  
   }
    
   
   
   
   /** 
    * 解密 
    * @param cipherText 密文 
    * @return 返回解密后的字符串 
    * @throws Exception  
    */  
   public static String decrypt(String cipherText) throws Exception{  
        // 从文件中得到私钥  
   	
   	//System.out.println(SecurityUtils.class.getClassLoader().getResource("").getPath());
       FileInputStream inPrivate = new FileInputStream( "d:\\pri.key");  
       PrivateKey privateKey = RSAUtils.loadPrivateKey(inPrivate);
       //cipherText = URLDecoder.decode(cipherText, "utf8");
       byte[] decryptByte = RSAUtils.decryptData(Base64Utils.decode(cipherText), privateKey);  
       String decryptStr = new String(decryptByte,"utf-8");  
       return decryptStr;  
   }  
   /** 
    * 加密 
    * @param plainTest 明文 
    * @return  返回加密后的密文 
    * @throws Exception  
    */  
   public static String encrypt(String plainTest) throws Exception{  
   	//System.out.println(SecurityUtils.class.getClassLoader().getResource("").getPath());
       FileInputStream inPublic = new FileInputStream("d:\\pub.key");  
       PublicKey publicKey = RSAUtils.loadPublicKey(inPublic);  
       // 加密  
       byte[] encryptByte = RSAUtils.encryptData(plainTest.getBytes("utf-8"), publicKey);  
       String afterencrypt = Base64Utils.encode(encryptByte);  
       //afterencrypt = URLEncoder.encode(afterencrypt, "utf-8");
       return afterencrypt;  
   }  

   
   
    
} 
	
	
