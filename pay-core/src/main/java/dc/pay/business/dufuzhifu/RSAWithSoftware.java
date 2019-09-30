package dc.pay.business.dufuzhifu;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

public class RSAWithSoftware {
	 public static final String KEY_ALGORITHM = "RSA";
	  public static final String SIGNATURE_ALGORITHM = "MD5withRSA";
	  private static final String PUBLIC_KEY = "RSAPublicKey";
	  private static final String PRIVATE_KEY = "RSAPrivateKey";
	  private static final int MAX_ENCRYPT_BLOCK = 116;
	  private static final int MAX_DECRYPT_BLOCK = 128;
	  
	  public static Map<String, Object> genKeyPair()
	    throws Exception
	  {
	    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
	    keyPairGen.initialize(1024);
	    KeyPair keyPair = keyPairGen.generateKeyPair();
	    RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();
	    RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
	    Map<String, Object> keyMap = new HashMap(2);
	    keyMap.put("RSAPublicKey", publicKey);
	    keyMap.put("RSAPrivateKey", privateKey);
	    return keyMap;
	  }
	  
	  public static String decryptByPrivateKey(String encryptedData, String privateKey)
	    throws Exception
	  {
	    byte[] keyBytes = Base64.decode(privateKey);
	    byte[] encryptedBytes = Base64.decode(encryptedData);
	    PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
	    Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
	    cipher.init(2, privateK);
	    int inputLen = encryptedBytes.length;
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    int offSet = 0;
	    
	    int i = 0;
	    while (inputLen - offSet > 0)
	    {
	      byte[] cache;
	     // byte[] cache;
	      if (inputLen - offSet > 128) {
	        cache = cipher.doFinal(encryptedBytes, offSet, 128);
	      } else {
	        cache = cipher.doFinal(encryptedBytes, offSet, inputLen - offSet);
	      }
	      out.write(cache, 0, cache.length);
	      i++;
	      offSet = i * 128;
	    }
	    byte[] decryptedData = out.toByteArray();
	    out.close();
	    return new String(decryptedData, "utf-8");
	  }
	  
	  public static String encryptByPublicKey(String data, String publicKey)
	    throws Exception
	  {
	    byte[] keyBytes = Base64.decode(publicKey);
	    byte[] dataBytes = data.getBytes("utf-8");
	    X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    Key publicK = keyFactory.generatePublic(x509KeySpec);
	    
	    Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
	    cipher.init(1, publicK);
	    int inputLen = dataBytes.length;
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    int offSet = 0;
	    
	    int i = 0;
	    while (inputLen - offSet > 0)
	    {
	      byte[] cache;
	     // byte[] cache;
	      if (inputLen - offSet > 116) {
	        cache = cipher.doFinal(dataBytes, offSet, 116);
	      } else {
	        cache = cipher.doFinal(dataBytes, offSet, inputLen - offSet);
	      }
	      out.write(cache, 0, cache.length);
	      i++;
	      offSet = i * 116;
	    }
	    byte[] encryptedData = out.toByteArray();
	    out.close();
	    return Base64.encode(encryptedData).replaceAll("\n", "").replaceAll("\r\n", "").replaceAll("\r", "");
	  }
	  
	  public static String signByPrivateKey(String data, String privateKey)
	    throws Exception
	  {
	    byte[] keyBytes = Base64.decode(privateKey);
	    PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
	    Signature signature = Signature.getInstance("MD5withRSA");
	    signature.initSign(privateK);
	    signature.update(data.getBytes("utf-8"));
	    return Base64.encode(signature.sign()).replaceAll("\n", "").replaceAll("\r\n", "").replaceAll("\r", "");
	  }
	  
	  public static boolean validateSignByPublicKey(String paramStr, String publicKey, String signedData)
	    throws Exception
	  {
	    byte[] keyBytes = Base64.decode(publicKey);
	    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    PublicKey publicK = keyFactory.generatePublic(keySpec);
	    Signature signature = Signature.getInstance("MD5withRSA");
	    signature.initVerify(publicK);
	    signature.update(paramStr.getBytes("utf-8"));
	    return signature.verify(Base64.decode(signedData));
	  }
	  
	  public static String getPrivateKey(Map<String, Object> keyMap)
	    throws Exception
	  {
	    Key key = (Key)keyMap.get("RSAPrivateKey");
	    return Base64.encode(key.getEncoded()).replaceAll("\r\n", "").replaceAll("\r", "").replaceAll("\n", "");
	  }
	  
	  public static String getPublicKey(Map<String, Object> keyMap)
	    throws Exception
	  {
	    Key key = (Key)keyMap.get("RSAPublicKey");
	    return Base64.encode(key.getEncoded()).replaceAll("\r\n", "").replaceAll("\r", "").replaceAll("\n", "");
	  }
}
