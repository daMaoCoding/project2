package dc.pay.business.kezhizhifu;

import java.net.MalformedURLException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
   

public class RSASignature{  
       
    
    public static final String SIGN_ALGORITHMS = "SHA1WithRSA";  
   
    
    public static String sign(String content, String privateKey, String encode)  
    {  
        try  
        {  
            PKCS8EncodedKeySpec priPKCS8    = new PKCS8EncodedKeySpec( Base64.decode(privateKey) );   
               
            KeyFactory keyf                 = KeyFactory.getInstance("RSA");  
            PrivateKey priKey               = keyf.generatePrivate(priPKCS8);  
   
            java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);  
   
            signature.initSign(priKey);  
            signature.update( content.getBytes(encode));  
   
            byte[] signed = signature.sign();  
               
            return Base64.encode(signed);  
        }  
        catch (Exception e)   
        {  
            e.printStackTrace();  
        }  
           
        return null;  
    }  
       
    public static String sign(String content, String privateKey)  
    {  
        try  
        {  
            PKCS8EncodedKeySpec priPKCS8    = new PKCS8EncodedKeySpec( Base64.decode(privateKey) );   
            KeyFactory keyf = KeyFactory.getInstance("RSA");  
            PrivateKey priKey = keyf.generatePrivate(priPKCS8);  
            java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);  
            signature.initSign(priKey);  
            signature.update( content.getBytes());  
            byte[] signed = signature.sign();  
            return Base64.encode(signed);  
        }  
        catch (Exception e)   
        {  
            e.printStackTrace();  
        }  
        return null;  
    }  
       
    /** 
    * RSA��ǩ����� 
    * @param content ��ǩ������ 
    * @param sign ǩ��ֵ 
    * @param publicKey ����������̹�Կ 
    * @param encode �ַ������� 
    * @return ����ֵ 
    */ 
    public static boolean doCheck(String content, String sign, String publicKey,String encode)  
    {  
        try  
        {  
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
            byte[] encodedKey = Base64.decode(publicKey);  
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));  
   
           
            java.security.Signature signature = java.security.Signature  
            .getInstance(SIGN_ALGORITHMS);  
           
            signature.initVerify(pubKey);  
            signature.update( content.getBytes(encode) );  
           
            boolean bverify = signature.verify( Base64.decode(sign) );  
            return bverify;  
               
        }   
        catch (Exception e)   
        {  
            e.printStackTrace();  
        }  
           
        return false;  
    }  
       
    public static boolean doCheck(String content, String sign, String publicKey)  
    {  
        try  
        {  
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
            byte[] encodedKey = Base64.decode(publicKey);  
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));  
   
            java.security.Signature signature = java.security.Signature  
            .getInstance(SIGN_ALGORITHMS);  
           
            signature.initVerify(pubKey);  
            signature.update( content.getBytes() );  
           
            boolean bverify = signature.verify( Base64.decode(sign) );  
            return bverify;  
               
        }   
        catch (Exception e)   
        {
            e.printStackTrace();  
        }
           
        return false;  
    }  
    /**
	 * ���ذ���ģ��modulus��ָ��exponent��haspMap
	 * @return
	 * @throws MalformedURLException
	 * @throws DocumentException
	 */
//	public static HashMap<String,String> rsaParameters(String xmlPublicKey) throws MalformedURLException, DocumentException{
//    	HashMap<String ,String> map = new HashMap<String, String>(); 
//		Document doc = DocumentHelper.parseText(xmlPublicKey);
//		String mudulus = (String) doc.getRootElement().element("Modulus").getData();
//		String exponent = (String) doc.getRootElement().element("Exponent").getData();
//		map.put("mudulus", mudulus);
//		map.put("exponent", exponent);
//		return map;
//	}
//	public static byte[] decodeBase64(String input) throws Exception{  
//        Class clazz=Class.forName("com.sun.org.apache.xerces.internal.impl.dv.util.Base64");  
//        Method mainMethod= clazz.getMethod("decode", String.class);  
//        mainMethod.setAccessible(true);  
//         Object retObj=mainMethod.invoke(null, input);  
//         return (byte[])retObj;  
//    }
//	
//	/**
//	 * ����RSA��Կ
//	 * @param modules
//	 * @param exponent
//	 * @return
//	 */
//	public static PublicKey getPublicKey(String modulus, String exponent){
//		try { 
//			byte[] m = decodeBase64(modulus);
//			byte[] e = decodeBase64(exponent);
//            BigInteger b1 = new BigInteger(1,m);  
//            BigInteger b2 = new BigInteger(1,e);  
//            KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
//            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(b1, b2);  
//            return (RSAPublicKey) keyFactory.generatePublic(keySpec);  
//        } catch (Exception e) {  
//            e.printStackTrace();  
//            return null;  
//        }	
//	}
	
}