package dc.pay.utils;/**
 * Created by admin on 2017/6/1.
 */

import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.constant.SERVER_MSG;
import sun.misc.BASE64Decoder;

/**
 * ************************
 * RAS 加解密工具
 *    说明：由于使用了 BouncyCastleProvider-->替换默认的 SunRsaSign，将导致同样的代码，放入本系统就是不能成功(RSA-Pay奇妙问题)，可以使用如下详细指定。(java默认的)
 *     keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,"SunRsaSign");
 *     Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(),"SunJCE");
 *     Provider[] providers = Security.getProviders(); //可观察出不同点，3Q.
 * @author tony 3556239829
 */





public class RsaUtil {

    private static Map<String,String> encryptMap = Collections.synchronizedMap(Maps.newConcurrentMap()); //临时API_KEY解密存储,ConcurrentHashMap
    private static Map<String,String> decryptMap = Collections.synchronizedMap(Maps.newConcurrentMap()); //临时API_KEY加密存储,ConcurrentHashMap

    private static String publicKeyString  = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCyAtnwFOfwARgOjIJPah1/r6kO49qNQF04Ca8oW7P4mBzc5oiO3FSU/0Zj8O8LJ3Pd5TJiQS4RT6PrywPqJgIUK1xz+xbpBHKl+f3DMLbxYVdGhy+0v4/0oeTcARAWocZKkcJucmU0S3zsFrkqnLzTnik1RzZrT2ddi58DImuyQQIDAQAB";
    private static String privateKeyString = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBALIC2fAU5/ABGA6Mgk9qHX+vqQ7j2o1AXTgJryhbs/iYHNzmiI7cVJT/RmPw7wsnc93lMmJBLhFPo+vLA+omAhQrXHP7FukEcqX5/cMwtvFhV0aHL7S/j/Sh5NwBEBahxkqRwm5yZTRLfOwWuSqcvNOeKTVHNmtPZ12LnwMia7JBAgMBAAECgYEAr0zliGteGVlKcpFmhoROtn6rctFqWa3n3xaSWqEsM4TA771K/7z0wqI9lJAhKA/bJ8rc+iuMDVoraDpOCZkSc1e26a5S2DK8PfZyRy0/FNkUg8/oFWGoENuQTz9eGuk5sLJ7jlIXnOQCQyZVHCSkNuFlScgg5ZGiB1I5USRiSRECQQD11MR1OIt45G/nsc1ZoNlST4WUFnUpy2HhX+7rjpNm70utU/m140JRh56HhAVqthpF/X8LbcC4n4wTTL6uvyeNAkEAuV/nMXEJPKJpChrQ0mSOikx2plBPvfOfeW/tfmzFK0/r6143sR6pVGlso6eEJAroHLLtqTs8FOBN4JOnvag+hQJBAKEBYEZR6W8dVnmORytboNo7AQbTipr7/LhqZ4XZ1IrHUW5NILBTDr1tMJQbEJ0qdZy/gXTPxjhlLo35Zq32voUCQHzQA7wYXZ3DxHGXSI7AQfEANssYO/irde6v2/pJhh/5eeFJ2Lma6Wv6Z7lw6tnRtiLMnpRZW8S6mfThtglOpHkCQQDsZR5CgvU/TG+ld5EPN/nO4tpjgao7VuXH8sKMGXXaBciC73B5uGNJtw7aiGYaflkHAtPxSQVI41gZl/m+/Fds";
    public static final String  KEYPAIRGENERATORPROVIDER = "SunRsaSign";
    public static final String  CIPHERPROVIDER = "SunJCE";
    public static final String  KEYFACTORYPROVIDER = "SunRsaSign";
    public static final String  SIGNATUREPROVIDER = "SunRsaSign";

    private static String modulusString;
    private static String publicExponentString;
    private static String privateExponentString;

    static {
        try {
            //获取公钥
            RSAPublicKey publicKey=(RSAPublicKey) getPublicKey(publicKeyString);
            modulusString = publicKey.getModulus().toString();
            publicExponentString=publicKey.getPublicExponent().toString();

            //获取私钥
            RSAPrivateKey privateKey=(RSAPrivateKey)getPrivateKey(privateKeyString);
            privateExponentString = privateKey.getPrivateExponent().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**********************************************************************************************************************/


    /**
     * 生成RAS 密钥公钥
     */
    public static void generateRSA(){
        KeyPairGenerator keyPairGenerator= null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA",KEYPAIRGENERATORPROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null!=keyPairGenerator){
            keyPairGenerator.initialize(1024);
            KeyPair keyPair=keyPairGenerator.generateKeyPair();
            //获取公钥，并以base64格式打印出来
            PublicKey publicKey=keyPair.getPublic();
            System.out.println("公钥： 　"+new String(Base64.getEncoder().encode(publicKey.getEncoded())));
            System.out.println();
            //获取私钥，并以base64格式打印出来
            PrivateKey privateKey=keyPair.getPrivate();
            System.out.println("私钥： 　"+new String(Base64.getEncoder().encode(privateKey.getEncoded())));
        }
    }



    /**
     * 加密字符串
     * @param data
     * @return
     * @throws Exception
     */
    public static String encrypt(String data) throws Exception {
        //由n和e获取公钥
        PublicKey publicKey=getPublicKey(modulusString, publicExponentString);
        //公钥加密
        String encrypted=encrypt(data, publicKey);
        return encrypted;
    }



    /**
     * 加密并缓存商户秘钥
     * @param decryptedApiKey
     * @return
     * @throws PayException
     */
    public static String encryptAndCache(String decryptedApiKey) throws PayException {
        try {
            String decryptApiKeyInMap = decryptMap.get(decryptedApiKey);
            if(StringUtils.isBlank(decryptApiKeyInMap)){
                decryptApiKeyInMap = RsaUtil.encrypt(decryptedApiKey);
                decryptMap.put(decryptedApiKey,decryptApiKeyInMap);
                return decryptApiKeyInMap;
            }
            return decryptApiKeyInMap;
        } catch (Exception ex) {
            throw new PayException(SERVER_MSG.DECRYPT_API_KEY_ERROR, ex);
        }

    }


    /**
     * 解密字符串
     * @param encryptedata
     * @return
     * @throws Exception
     */
    public static String decrypt(String encryptedata) throws Exception {
        //由n和d获取私钥
        PrivateKey privateKey=getPrivateKey(modulusString, privateExponentString);
        //私钥解密
        String decrypted=decrypt(encryptedata,  privateKey);
        return decrypted;
    }


    /**
     * 解密并缓存商户秘钥
     * @param encryptedApiKey
     * @return
     * @throws PayException
     */
    public static String decryptAndCache(String encryptedApiKey) throws PayException {
        try {
            String decryptApiKeyInMap = null; encryptMap.get(encryptedApiKey);
            if(StringUtils.isBlank(decryptApiKeyInMap)){
                decryptApiKeyInMap = RsaUtil.decrypt(encryptedApiKey);
                encryptMap.put(encryptedApiKey,decryptApiKeyInMap);
                return decryptApiKeyInMap;
            }
            return decryptApiKeyInMap;
        } catch (Exception ex) {
            throw new PayException(SERVER_MSG.DECRYPT_API_KEY_ERROR, ex);
        }

    }



/********************************************************************************************************************************************/

    //公钥加密，并转换成十六进制字符串打印出来
    private static String encrypt(String content, PublicKey publicKey) throws Exception{
        Cipher cipher= Cipher.getInstance("RSA",CIPHERPROVIDER);    //java默认"RSA"="RSA/ECB/PKCS1Padding"  RSA/None/PKCS1Padding ，"BC
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        int splitLength=((RSAPublicKey)publicKey).getModulus().bitLength()/8-11;
        byte[][] arrays=splitBytes(content.getBytes(), splitLength);
        StringBuffer sb=new StringBuffer();
        for(byte[] array : arrays){
            sb.append(bytesToHexString(cipher.doFinal(array)));
            //sb.append(cipher.doFinal(array));
        }
        return sb.toString();
    }




    /**
     * 公钥加密-转Base64
     * @param data
     * @param publicKeyString
     * @return
     * @throws Exception
     */
    public static String encryptToBase64(String data,String publicKeyString) throws Exception {
        PublicKey publicKey = RsaUtil.getPublicKey(publicKeyString);
        byte[] encript = encript(data.getBytes(), publicKey);
        return new String(java.util.Base64.getEncoder().encode(encript));
    }


    public static  byte[] encript(byte[] ptext,Key ppkey) throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding",CIPHERPROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, ppkey);
        //一次性对超过117个字节的数据加密
        byte[] enBytes = null;
        int i = 0;
        for (i = 0; i < ptext.length; i += 64) {
            // 注意要使用2的倍数，否则会出现加密后的内容再解密时为乱码
            byte[] subarray = ArrayUtils.subarray(ptext, i, i + 64);
            byte[] doFinal = cipher.doFinal(subarray);
            enBytes = ArrayUtils.addAll(enBytes, doFinal);
        }
        return enBytes;
    }





    //私钥解密，并转换成十六进制字符串打印出来
    private static String decrypt(String content, PrivateKey privateKey) throws Exception{
        Cipher cipher=Cipher.getInstance("RSA",CIPHERPROVIDER);    // Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        int splitLength=((RSAPrivateKey)privateKey).getModulus().bitLength()/8;
        byte[] contentBytes=hexString2Bytes(content);
        byte[][] arrays=splitBytes(contentBytes, splitLength);
        StringBuffer sb=new StringBuffer();
        for(byte[] array : arrays){
            sb.append(new String(cipher.doFinal(array)));
        }
        return sb.toString();
    }





    //将base64编码后的公钥字符串转成PublicKey实例
    public  static PublicKey getPublicKey(String publicKey) throws Exception{
        byte[ ] keyBytes= Base64.getDecoder().decode(publicKey.getBytes());
        KeyFactory keyFactory= KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER);
        X509EncodedKeySpec keySpec=new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(keySpec);
    }


    //将base64编码后的公钥字符串转成PublicKey实例
    public static PublicKey getPublicKey(String modulusStr, String exponentStr) throws Exception{
        BigInteger modulus=new BigInteger(modulusStr);
        BigInteger exponent=new BigInteger(exponentStr);
        RSAPublicKeySpec publicKeySpec=new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory= KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER);
        return keyFactory.generatePublic(publicKeySpec);
    }




    //将base64编码后的私钥字符串转成PrivateKey实例
    public static PrivateKey getPrivateKey(String privateKey) throws Exception{
        byte[ ] keyBytes=Base64.getDecoder().decode(privateKey.getBytes());
        PKCS8EncodedKeySpec keySpec=new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory=KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER);
        return keyFactory.generatePrivate(keySpec);
    }



    //将base64编码后的私钥字符串转成PrivateKey实例
    private static PrivateKey getPrivateKey(String modulusStr, String exponentStr) throws Exception{
        BigInteger modulus=new BigInteger(modulusStr);
        BigInteger exponent=new BigInteger(exponentStr);
        RSAPrivateKeySpec privateKeySpec=new RSAPrivateKeySpec(modulus, exponent);
       // System.out.println(Security.getProviders());
        KeyFactory keyFactory=KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER); //, "Sun"
        return keyFactory.generatePrivate(privateKeySpec);
    }



    //拆分byte数组
    private static byte[][] splitBytes(byte[] bytes, int splitLength){
        int x; //商，数据拆分的组数，余数不为0时+1
        int y; //余数
        y=bytes.length%splitLength;
        if(y!=0){
            x=bytes.length/splitLength+1;
        }else{
            x=bytes.length/splitLength;
        }
        byte[][] arrays=new byte[x][];
        byte[] array;
        for(int i=0; i<x; i++){

            if(i==x-1 && bytes.length%splitLength!=0){
                array=new byte[bytes.length%splitLength];
                System.arraycopy(bytes, i*splitLength, array, 0, bytes.length%splitLength);
            }else{
                array=new byte[splitLength];
                System.arraycopy(bytes, i*splitLength, array, 0, splitLength);
            }
            arrays[i]=array;
        }
        return arrays;
    }

    //byte数组转十六进制字符串
    private static String bytesToHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length);
        String sTemp;
        for (int i = 0; i < bytes.length; i++) {
            sTemp = Integer.toHexString(0xFF & bytes[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    //十六进制字符串转byte数组
    private static byte[] hexString2Bytes(String hex) {
        int len = (hex.length() / 2);
        hex=hex.toUpperCase();
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | (toByte(achar[pos + 1]) &0xff) );  //&0xff int 符号位
        }
        return result;
    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }
    
    public static String signByPrivateKey(String data, String privateKey)throws Exception{
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER);
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = Signature.getInstance("MD5withRSA",SIGNATUREPROVIDER);
        signature.initSign(privateK);
        signature.update(data.getBytes("utf-8"));
        return new String(Base64.getEncoder().encode(signature.sign())).replaceAll("\n", "").replaceAll("\r\n", "").replaceAll("\r", "");
    }


    //SHA256withRSA  MD5withRSA  SHA1WithRSA
    public static String signByPrivateKey(String data, String privateKey,String sigType)throws Exception{
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER);
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = Signature.getInstance(sigType,SIGNATUREPROVIDER);
        signature.initSign(privateK);
        signature.update(data.getBytes("utf-8"));
        return new String(Base64.getEncoder().encode(signature.sign())).replaceAll("\n", "").replaceAll("\r\n", "").replaceAll("\r", "");
    }


    /**
     * RSA验签名
     * @param info
     * @param pubkey
     * @param signatureData
     * @return
     */
    public static boolean validateSignByPublicKey(String info,String pubkey,String signatureData,String sigType) {
        try{
            PublicKey publicK = getPublicKey2(pubkey);
            Signature signature = Signature.getInstance(sigType,SIGNATUREPROVIDER);
            signature.initVerify(publicK);
            signature.update(info.getBytes("UTF-8"));
            return signature.verify(Base64.getDecoder().decode(signatureData));
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }




    public static boolean validateSignByPublicKey(String signInfo, String publicKey, String signedData) {
        try{
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER);
            PublicKey publicK = keyFactory.generatePublic(keySpec);
            Signature signature = Signature.getInstance("MD5withRSA",SIGNATUREPROVIDER);
            signature.initVerify(publicK);
            signature.update(signInfo.getBytes("utf-8"));
            return signature.verify(Base64.getDecoder().decode(signedData));
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

  }

//********************************************************************************************************************//

    public static  PublicKey getPublicKey2(String publicKeyStr) {
        PublicKey publicKey = null;
        try {
            java.security.spec.X509EncodedKeySpec bobPubKeySpec = new java.security.spec.X509EncodedKeySpec( new BASE64Decoder().decodeBuffer(publicKeyStr));
            java.security.KeyFactory keyFactory; // RSA对称加密算法
            keyFactory = java.security.KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER);
            publicKey = keyFactory.generatePublic(bobPubKeySpec); // 取公钥匙对象
        } catch (Exception e) {
            e.printStackTrace();
        }
        return publicKey;
    }


    public static PrivateKey getPrivateKey2(String privateKeyStr) {
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec priPKCS8;
        try {
            priPKCS8 = new PKCS8EncodedKeySpec( new BASE64Decoder().decodeBuffer(privateKeyStr));
            KeyFactory keyf = KeyFactory.getInstance("RSA",KEYFACTORYPROVIDER);
            privateKey = keyf.generatePrivate(priPKCS8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return privateKey;
    }


    /**
     * 数字签名函数入口(For 新百付通)
     * @param plainBytes 待签名明文字节数组
     * @param privateKey 签名使用私钥
     * @param signAlgorithm 签名算法
     * @return 签名后的字节数组
     * @throws Exception
     */
    public static byte[] digitalSign(byte[] plainBytes, PrivateKey privateKey, String signAlgorithm) throws PayException {
        try {
            Signature signature = Signature.getInstance(signAlgorithm,SIGNATUREPROVIDER);
            signature.initSign(privateKey);
            signature.update(plainBytes);
            byte[] signBytes = signature.sign();
            return signBytes;
        } catch (NoSuchAlgorithmException e) {
            throw new PayException(String.format("数字签名时没有[%s]此类算法", signAlgorithm));
        } catch (InvalidKeyException e) {
            throw new PayException("数字签名时私钥无效");
        } catch (SignatureException e) {
            throw new PayException("数字签名时出现异常");
        }catch (NoSuchProviderException e) {
            throw new PayException("没有这个Provider");
        }
    }

    public static final byte[] hexStrToBytes(String s) {
        byte[] bytes;
        bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * 		i + 2), 16);
        }
        return bytes;
    }


    /**
     * RSA数字签名 (For 新百付通)
     * @param data
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String signByPrivateKey2(String data, String privateKey)throws PayException{
        try {
            PrivateKey hzfPriKey =  RsaUtil.getPrivateKey(privateKey);
            byte[] base64SingDataBytes = org.apache.commons.codec.binary.Base64.encodeBase64(RsaUtil.digitalSign(data.getBytes("UTF-8"), hzfPriKey, "SHA1WithRSA"));
           return new String(base64SingDataBytes, "UTF-8");
        } catch (Exception e) {
            throw new PayException("RSA数字签名出错:"+e.getMessage());
        }
    }


    //For 商银信支付(奇葩进行2次base64编码)
    public static String signByPrivateKey2(String data, String privateKey,  String input_charset)throws PayException{
        try  {
            PrivateKey priKey =  RsaUtil.getPrivateKey(privateKey);
            java.security.Signature signature = java.security.Signature.getInstance("SHA1WithRSA");
            signature.initSign(priKey);
            signature.update(data.getBytes(input_charset));
            byte[] signed = signature.sign();
            String str1 = new String(org.apache.commons.codec.binary.Base64.encodeBase64(signed));
            byte []  base64SingDataBytes = org.apache.commons.codec.binary.Base64.encodeBase64(str1.getBytes());
            String str2 = new String(base64SingDataBytes, "UTF-8");
            return str2;
        }
        catch (Exception e) { throw new PayException("RSA数字签名出错:"+e.getMessage()); }


    }


    /**
     * RSA验签名 (For 新百付通)
     * @param info
     * @param pubkey
     * @param signatureData
     * @return
     */
    public static boolean validateSignByPublicKey2(String info,String pubkey,String signatureData) {
        try{
            PublicKey publicK = getPublicKey2(pubkey);
            Signature signature = Signature.getInstance("SHA1WithRSA",SIGNATUREPROVIDER);
            signature.initVerify(publicK);
            signature.update(info.getBytes("UTF-8"));
            return signature.verify(Base64.getDecoder().decode(signatureData));
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static boolean validateSignByPublicKey2(String info,  String pubkey, String sign, String input_charset){
        try{
            sign = URLDecoder.decode(sign);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.getDecoder().decode(pubkey);
            PublicKey pubKey = keyFactory .generatePublic(new X509EncodedKeySpec(encodedKey));
            java.security.Signature signature = java.security.Signature.getInstance("SHA1WithRSA");
            signature.initVerify(pubKey);
            signature.update(info.getBytes(input_charset));
            sign = sign.replaceAll("\\*", "+");
            sign = sign.replaceAll("-", "/");
            boolean bverify = signature.verify( Base64.getDecoder().decode(sign));
            return bverify;
        }
        catch (Exception e) {e.printStackTrace();}
        return false;
    }
    
    
    public static boolean validateSignByPublicKey3(String info,  String pubkey, String sign){
        try{
            //sign = URLDecoder.decode(sign);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA","SunRsaSign");
            byte[] encodedKey = org.apache.commons.codec.binary.Base64.decodeBase64(pubkey);
            PublicKey pubKey = keyFactory .generatePublic(new X509EncodedKeySpec(encodedKey));
            java.security.Signature signature = java.security.Signature.getInstance("SHA1WithRSA");
            signature.initVerify(pubKey);
            signature.update(info.getBytes());
            boolean bverify = signature.verify(org.apache.commons.codec.binary.Base64.decodeBase64(sign));
            return bverify;
        }
        catch (Exception e) {e.printStackTrace();}
        return false;
    }
    
    





    public static String signByPublicKey(String data, String publicKey)throws Exception{
        Cipher cipher = Cipher.getInstance("RSA",CIPHERPROVIDER);
        RSAPublicKey pubKey = (RSAPublicKey)getPublicKey(publicKey);

        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] cipherText = cipher.doFinal(data.getBytes());

        // return new String(cipherText);  //加密后的东西
        return new String(Base64.getEncoder().encode(cipherText)).replaceAll("\n", "").replaceAll("\r\n", "").replaceAll("\r", "");
    }


    //------------------------------------------------------
    private static  int enSegmentSize=117;//加密长度
    private static int deSegmentSize=128;//解密长度

    public static String dencipher(String mer_private_key, String data){
        RsaHelper rsa = new RsaHelper();
        String deTxt = rsa.decipher(data, mer_private_key,deSegmentSize);
        return deTxt;
    }

    public static String merencipher(String mer_public_key,String data,int type){
        RsaHelper rsa = new RsaHelper();
        String ciphertext = rsa.encipher(data, mer_public_key,enSegmentSize,type);
        return  ciphertext;
    }

}
