package dc.pay.business.zhihuimingfuzhifu;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * @description: 接口请求专用加密DES类
 * @author: Cobby
 * @create: 2019-04-10 11:03
 **/
public class ZhiHuiMingFuDesHelper {

    /**
     * 加密
     *
     * @param message 加密内容
     * @param key     加密密钥(明文)
     * @return
     */
    public static String encrypt(String message, String key) {
        try {
            return byteArr2HexStr(_encrypt(message,  key ));
        } catch (Exception e) {
        }
        return message;
    }

    /**
     * 加密
     *
     * @param message 加密内容
     * @param key     加密密钥(md5 0-8)
     * @return
     * @throws Exception
     */
    private static byte[] _encrypt(String message, String key)
            throws Exception {
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return cipher.doFinal(message.getBytes("UTF-8"));
    }

    private static String byteArr2HexStr(byte[] arrB) throws Exception {
        int iLen = arrB.length;
        // 每个byte用两个字符才能表示，所以字符串的长度是数组长度的两倍
        StringBuffer sb = new StringBuffer(iLen * 2);
        for (int i = 0; i < iLen; i++) {
            int intTmp = arrB[i];
            // 把负数转换为正数
            while (intTmp < 0) {
                intTmp = intTmp + 256;
            }
            // 小于0F的数需要在前面补0
            if (intTmp < 16) {
                sb.append("0");
            }
            sb.append(Integer.toString(intTmp, 16));
        }
        return sb.toString();
    }


    /**
     * 解密
     *
     * @param message 密文内容
     * @param key     解密密钥(明文)
     * @return
     * @throws Exception
     */
    public static String decrypt(String message, String key) {
        try {
            return _decrypt(message, key );
        } catch (Exception e) {
        }
        return message;
    }

    /**
     * @param message 密文内容
     * @param key     解密密钥(md5 0-8)
     * @return
     * @throws Exception
     */
    private static String _decrypt(String message, String key) throws Exception {
        byte[] bytesrc = hexStr2ByteArr(message);
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        byte[] retByte = cipher.doFinal(bytesrc);
        return new String(retByte).trim().replace("\n", "");
    }

    private static byte[] hexStr2ByteArr(String strIn) throws Exception {
        byte[] arrB = strIn.getBytes();
        int iLen = arrB.length;
        // 两个字符表示一个字节，所以字节数组长度是字符串长度除以2
        byte[] arrOut = new byte[iLen / 2];
        for (int i = 0; i < iLen; i = i + 2) {
            String strTmp = new String(arrB, i, 2);
            arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
        }
        return arrOut;
    }

}
