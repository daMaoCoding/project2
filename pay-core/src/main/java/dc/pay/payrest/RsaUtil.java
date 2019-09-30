package dc.pay.payrest;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * ************************
 * RAS 加密工具 - For WEB
 * @author tony 3556239829
 */
public class RsaUtil {
    private static String publicKeyString  = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCyAtnwFOfwARgOjIJPah1/r6kO49qNQF04Ca8oW7P4mBzc5oiO3FSU/0Zj8O8LJ3Pd5TJiQS4RT6PrywPqJgIUK1xz+xbpBHKl+f3DMLbxYVdGhy+0v4/0oeTcARAWocZKkcJucmU0S3zsFrkqnLzTnik1RzZrT2ddi58DImuyQQIDAQAB";
    private static String modulusString;
    private static String publicExponentString;

    static {
        try {
            //获取公钥
            RSAPublicKey publicKey=(RSAPublicKey) getPublicKey(publicKeyString);
            modulusString = publicKey.getModulus().toString();
            publicExponentString=publicKey.getPublicExponent().toString();
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
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyPairGenerator.initialize(1024);
        KeyPair keyPair=keyPairGenerator.generateKeyPair();

        //获取公钥，并以base64格式打印出来
        PublicKey publicKey=keyPair.getPublic();
        System.out.println("公钥："+new String(Base64.getEncoder().encode(publicKey.getEncoded())));

        //获取私钥，并以base64格式打印出来
        PrivateKey privateKey=keyPair.getPrivate();
        System.out.println("私钥："+new String(Base64.getEncoder().encode(privateKey.getEncoded())));
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


    /********************************************************************************************************************************************/

    //公钥加密，并转换成十六进制字符串打印出来
    private static String encrypt(String content, PublicKey publicKey) throws Exception{
        Cipher cipher= Cipher.getInstance("RSA");//睿捷通"RSA"="RSA/ECB/PKCS1Padding"  RSA/None/PKCS1Padding ，"BC
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        int splitLength=((RSAPublicKey)publicKey).getModulus().bitLength()/8-11;
        byte[][] arrays=splitBytes(content.getBytes(), splitLength);
        StringBuffer sb=new StringBuffer();
        for(byte[] array : arrays){
            sb.append(bytesToHexString(cipher.doFinal(array)));
        }
        return sb.toString();
    }




    //将base64编码后的公钥字符串转成PublicKey实例
    private static PublicKey getPublicKey(String publicKey) throws Exception{
        byte[ ] keyBytes= Base64.getDecoder().decode(publicKey.getBytes());
        KeyFactory keyFactory= KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec=new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(keySpec);
    }


    //将base64编码后的公钥字符串转成PublicKey实例
    private static PublicKey getPublicKey(String modulusStr, String exponentStr) throws Exception{
        BigInteger modulus=new BigInteger(modulusStr);
        BigInteger exponent=new BigInteger(exponentStr);
        RSAPublicKeySpec publicKeySpec=new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory= KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(publicKeySpec);
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
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }



}
