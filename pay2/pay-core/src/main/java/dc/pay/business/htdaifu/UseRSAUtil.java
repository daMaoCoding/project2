package dc.pay.business.htdaifu;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

import org.apache.log4j.Logger;


/**
 * RSA加解密
 * @author jiafuwei
 * created at 2018/6/8
 */
public class UseRSAUtil {
	private static Logger logger = Logger.getLogger(UseRSAUtil.class);
    /**
     * RSA最大加密明文大小 
     */  
    private static final int MAX_ENCRYPT_BLOCK = 117;  
      
    /**
     * RSA最大解密密文大小 
     */  
    private static final int MAX_DECRYPT_BLOCK = 128; 

    public static final String KEY_ALGORITHM = "RSA";
    private static KeyFactory keyFactory = null;
    public static String privateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAK2fSa6fZFMteHNEY5MQr3DKBl8rl6w+3SB5SPJzGq2NLmL1IDssrJUAM2Gg5BKLC4DkCfwnGMoBuPw89ksTjFBLo9Z9MmeS8ypWw8fAKIcayAiOfqS0UwLR5IKx8TzGl29SD8NAyeFycd2lrfY03p4KhxU30QDLWhKfznwBzE9VAgMBAAECgYEAq1i2ePWpJ0WKyAdIVg1zd1aJ70awSzdHOBM6YLwkKf9ZNiRAiOXP9MTb8I++RmafFbb3+7gCCOqmS5lGYFWMdNN1ivGxxAUrhlunoCJKoReEZQYcneI0dzddAzKQtu5UWw+YmGNNKQwLfZm0+GtPak7oQL1A6NV1ZrStttLKG8ECQQD7dH4pF2SiInMWjZLLNyDykQ51K34z5XCLuSmTSVaJC1ARy367PGLUiUvUGuC5+MgD4MSeLwx1oH/JM46KB4SdAkEAsMKnpLJopL41stRfRNZoT0huDXGw3Vad617NAXoxju+dUNQeqsi6zGfe1HxE44Kd3hvg+gNUnAb2CwQqrm0MGQJAIPtCoPkkhe+m0Mp4+pach7RnBQ9TWlM509nRjjQMWaVWNz9NvBlLjT0E6SktWLc85OVSZL0fET6gBC/y/j/GMQJAKKdMaVvDJ4i7zvp7QtjROtLcxOjOFcoTHgw6uElDVq2TkACJYRwzokmNfW9rZKwo5Omij73uWshbFGOL8XwRGQJAClBRLzJ21mnUijqi6b+mELFTb9hAUrhFh5jkePyWnd72SXTzwd+9wNO6fy3NIN+cKFDMf1P9ml7fIDkPfMRv8A==";
    //public static String publicKey="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtn0mun2RTLXhzRGOTEK9wygZfK5esPt0geUjycxqtjS5i9SA7LKyVADNhoOQSiwuA5An8JxjKAbj8PPZLE4xQS6PWfTJnkvMqVsPHwCiHGsgIjn6ktFMC0eSCsfE8xpdvUg/DQMnhcnHdpa32NN6eCocVN9EAy1oSn858AcxPVQIDAQAB";
    public static String publicKey = "";
    static {
        try {
            keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,"SunRsaSign");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 解密方法
     * @param dataStr 要解密的数据
     * @return 解密后的原数据
     * @throws Exception
     */
    public static String decrypt(String dataStr) throws Exception{
        //要加密的数据
        System.out.println("要解密的数据:"+dataStr);
        //对私钥解密
        Key decodePrivateKey = getPrivateKeyFromBase64KeyEncodeStr(privateKey);
        //Log.i("机密",""+decodePrivateKey);
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM,"SunJCE");
        cipher.init(Cipher.DECRYPT_MODE, decodePrivateKey);
        byte[] encodedData = Base64.decode(dataStr);
        byte[] decodedData = cipher.doFinal(encodedData);
        String decodedDataStr = new String(decodedData,"utf-8");
        System.out.println("私钥解密后的数据:"+decodedDataStr);
        return decodedDataStr;
    }

    public  static Key getPrivateKeyFromBase64KeyEncodeStr(String keyStr) {
        byte[] keyBytes = Base64.decode(keyStr);
        // 取得私钥
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        Key privateKey=null;
        try {
            privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return privateKey;
    }

    /**
     * 获取base64加密后的字符串的原始公钥
     * @param keyStr
     * @return
     */
    public static Key getPublicKeyFromBase64KeyEncodeStr(String keyStr) {
        byte[] keyBytes = Base64.decode(keyStr);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        Key publicKey = null;
        try {
            publicKey = keyFactory.generatePublic(x509KeySpec);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return publicKey;
    }

    /**
     * 公钥加密方法
     * @param dataStr 要加密的数据
     * @param dataStr 公钥base64字符串
     * @return 加密后的base64字符串
     * @throws Exception
     */
    public static String encryptPublicKey(String dataStr) throws Exception{
        //要加密的数据
        System.out.println("要加密的数据:"+dataStr);
        byte[] data = dataStr.getBytes();
        // 对公钥解密
        Key decodePublicKey = getPublicKeyFromBase64KeyEncodeStr(publicKey);
        // 对数据加密
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM,"SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, decodePublicKey);
        byte[] encodedData = cipher.doFinal(data);
        String encodedDataStr = new String(Base64.encode(encodedData));
        System.out.println("公钥加密后的数据:"+encodedDataStr);
        return encodedDataStr;
    }
    
    /**
     * 使用公钥进行分段加密
     * @param dataStr 要加密的数据
     * @return 公钥base64字符串
     * @throws Exception
     */
    public static String encryptByPublicKey(String dataStr,String publicKey)  
            throws Exception {  
        //要加密的数据
        System.out.println("要加密的数据:"+dataStr);
        byte[] data = dataStr.getBytes();
        // 对公钥解密
        Key decodePublicKey = getPublicKeyFromBase64KeyEncodeStr(publicKey);
         
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,"SunRsaSign");  
        // 对数据加密  
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(),"SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, decodePublicKey);  
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
        String encodedDataStr = new String(Base64.encode(encryptedData));
        System.out.println("公钥加密后的数据:"+encodedDataStr);
        return encodedDataStr;  
    } 
    
    public static byte[] encryptByPublicKey(byte[] data,String publicKey)  
            throws Exception {  
        //要加密的数据
        System.out.println("要加密的数据:"+data);
       // byte[] data = dataStr.getBytes();
        // 对公钥解密
        Key decodePublicKey = getPublicKeyFromBase64KeyEncodeStr(publicKey);
         
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,"SunRsaSign");  
        // 对数据加密  
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(),"SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, decodePublicKey);  
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
    } 

    /**
     * 使用私钥进行分段解密
     * @param dataStr 使用base64处理过的密文
     * @return 解密后的数据
     * @throws Exception
     */
    public static String decryptByPrivateKey(String dataStr,String PrivateKey)  
            throws Exception {  
        
        byte[] encryptedData = Base64.decode(dataStr);
        
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,"SunRsaSign");  
        Key decodePrivateKey = getPrivateKeyFromBase64KeyEncodeStr(PrivateKey);
        
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(),"SunJCE");
        cipher.init(Cipher.DECRYPT_MODE, decodePrivateKey);  
        int inputLen = encryptedData.length;  
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
        int offSet = 0;  
        byte[] cache;  
        int i = 0;  
        // 对数据分段解密  
        while (inputLen - offSet > 0) {  
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {  
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);  
            } else {  
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);  
            }  
            out.write(cache, 0, cache.length);  
            i++;  
            offSet = i * MAX_DECRYPT_BLOCK;  
        }  
        byte[] decryptedData = out.toByteArray();  
        out.close();  
        String decodedDataStr = new String(decryptedData,"utf-8");
        //System.out.println("私钥解密后的数据:"+decodedDataStr);
        return decodedDataStr;  
    } 
   
    
    public static String sign(String content, String privateKey)  
    {  
        try  
        {  
            PKCS8EncodedKeySpec priPKCS8    = new PKCS8EncodedKeySpec( Base64.decode(privateKey) );   
            KeyFactory keyf = KeyFactory.getInstance("RSA","SunRsaSign");
            PrivateKey priKey = keyf.generatePrivate(priPKCS8);  
            java.security.Signature signature = java.security.Signature.getInstance("MD5WithRSA");  
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
    
    public static boolean doCheck(String content, String sign, String publicKey)  
    {  
        try  
        {  
            KeyFactory keyFactory = KeyFactory.getInstance("RSA","SunRsaSign");
            byte[] encodedKey = Base64.decode(publicKey);  
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));  
            java.security.Signature signature = java.security.Signature.getInstance("MD5WithRSA");
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
     * 生成签名摘要
     */
    private static byte[] sha1(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = null;
        md = MessageDigest.getInstance("MD5"); // 选择SHA-1，也可以选择MD5
        byte[] digest = md.digest(data); // 返回的是byet[]，要转化为String存储比较方便
        return digest;
    }
    
    
    public static void main(String[] args) throws Exception {
       String content = "acctname=6bada20e53bafaa29937f7046a10de0c&acctno=ff4c5bf94146befe1adab869803ddf80feb959b7d4642fcb&amount=1&appid=mx_test&apporderid=APP1544256125519&bankcode=CMB&bankname=招商银行&bankno=308584000013&banksettno=308584000013&certificatecode=300884c1ee82a67f84897a87247ee6068d0a36a13eab437a&city=北京&cmd=DPAYTOTAL&mobile=2760b498c62c36890f76a2eb21dfe30a&notifyurl=http://219.143.36.230:18000/GW/NotifyTest&orderbody=代付测试&ordertime=20181208160205&province=北京&userid=CUST_123456789&version=2.0";
      // logger.info(content);
       String miwen = "UQnzMI0V9LSI4+3QNjXUbVRDtfYLrBVujL6DbBs7zBaIZob1cALAzzt0URgKUiliqbHQKNrI2BG7QDWEQTvjdj2s+p4mqUlzf0eDgX0KwqXmXo31xtjFMnx3tmm64VLRcFHFYWGEpGpNodr3qs797fLCQ/BPPZro8mswBkNndsukVpt5qiZSiIsZfTyzzrPbhRqbh+NRbCSVjcwcgnxlJBbGaaDVUIhr1AAHUUjT+7dVDik6QQW8OEWwrm5kzqQM64GfWunRxGC7+06TAukVABNHiP5PU8A4JYWyYUTjw6r1LZ75DQ3wznC4UeO+pEh1NEsK4Wlh1GxOLSvp9T/LJZoJ8LOmhyLvFEO+I3CDYkI1u6u3IYJWMm485cJy9vrg9KJS0lECYsqysT0CqyuUfK1SWkTG4+cBVDKjEJb+JrybyWshyN0g8HrFFCBewThGOTOdmtRoOVMAEJp8Kml8sCEERanmLnn4XleJCmfDpH5nx20/lHxH8pcVqpU68iW5Z9p5JvgSf76Kg6J/dN+/FvOgqX/NqUdRw9JOD/OX3A0J+IjrvCXHEAEGeTiCCLXt3bHBdlZo0ZVd5x2V/2eNV19BPh6rfo/QU/QpY6KZ0L3HN5D6o8zA9FoeRYYrvzTvWEwNn6MoA3jlmmq7YiIlM55/C3MO+VU/gVmhEvNZ3Gw=";
       logger.info(UseRSAUtil.decryptByPrivateKey(miwen, privateKey));
//       UseRSAUtil.decryptByPrivateKey(UseRSAUtil.encryptByPublicKey(content,publicKey),privateKey); 
//       String sign = UseRSAUtil.sign("OID2013547545454545OID2013547545454545OID2013547545454545",privateKey);
//       logger.info(sign);
//       logger.info(doCheck("OID2013547545454545OID2013547545454545OID2013547545454545",sign,publicKey));
    }
}
