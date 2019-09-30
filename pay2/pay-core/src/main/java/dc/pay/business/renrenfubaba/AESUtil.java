package dc.pay.business.renrenfubaba;


import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import dc.pay.base.processor.PayException;
import dc.pay.utils.HandlerUtil;

/**
 * AES加解密工具类
 */
public class AESUtil {


    /**
     * 加密
     * @param key
     * @param data
     * @return
     * @throws java.io.UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] encrypt(String key, String data)
            throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        SecretKeySpec newKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, newKey);
        return cipher.doFinal(data.getBytes());
    }

    /**
     * 解密
     * @param key
     * @param data
     * @return
     * @throws java.io.UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String decrypt(String key, String data)
            throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        SecretKeySpec newKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, newKey);
        byte [] bytes = decodeHex(data.toCharArray());
        byte[]  decVa = cipher.doFinal(bytes);
        return new String(decVa);
    }


    public static String bytes2String(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static byte[] string2Bytes(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return result;
    }


    /**
     * 用于建立十六进制字符的输出的小写字符数组
     */
    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f'};

    /**
     * 用于建立十六进制字符的输出的大写字符数组
     */
    private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'};

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data byte[]
     * @return 十六进制String
     */
    public static String encodeHexStr(byte[] data) {
        return encodeHexStr(data, true);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data        byte[]
     * @param toLowerCase <code>true</code> 传换成小写格式 ， <code>false</code> 传换成大写格式
     * @return 十六进制String
     */
    public static String encodeHexStr(byte[] data, boolean toLowerCase) {
        return encodeHexStr(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data     byte[]
     * @param toDigits 用于控制输出的char[]
     * @return 十六进制String
     */
    protected static String encodeHexStr(byte[] data, char[] toDigits) {
        return new String(encodeHex(data, toDigits));
    }

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data        byte[]
     * @param toLowerCase <code>true</code> 传换成小写格式 ， <code>false</code> 传换成大写格式
     * @return 十六进制char[]
     */
    public static char[] encodeHex(byte[] data, boolean toLowerCase) {
        return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * 将十六进制字符数组转换为字节数组
     *
     * @param data
     *            十六进制char[]
     * @return byte[]
     * @throws RuntimeException
     *             如果源十六进制字符数组是一个奇怪的长度，将抛出运行时异常
     */
    public static byte[] decodeHex(char[] data) {

        int len = data.length;

        if ((len & 0x01) != 0) {
            throw new RuntimeException("Odd number of characters.");
        }

        byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    /**
     * 将十六进制字符转换成一个整数
     *
     * @param ch
     *            十六进制char
     * @param index
     *            十六进制字符在字符数组中的位置
     * @return 一个整数
     * @throws RuntimeException
     *             当ch不是一个合法的十六进制字符时，抛出运行时异常
     */
    protected static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new RuntimeException("Illegal hexadecimal character " + ch
                    + " at index " + index);
        }
        return digit;
    }




    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data     byte[]
     * @param toDigits 用于控制输出的char[]
     * @return 十六进制char[]
     */
    protected static char[] encodeHex(byte[] data, char[] toDigits) {
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }

    /**
     * 验证签名
     *
     * @param
     */
    public static boolean verifySign(String code,String result,String SECRET,String other_sign) {

        //使用seckey对除sign以外的公共参数进行md5加密生成sign值
        String toSignStr = "Code=" + code + "&Result=" + result + "&Key=" + SECRET;
        System.out.println(("calculateSign Str:" + toSignStr));

        String sign = null;
        try {
            sign = HandlerUtil.getMD5UpperCase(toSignStr);
        } catch (PayException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("calculateSign.sign:" + sign);
        System.out.println("rrp res sign:"+other_sign);
        return sign.toUpperCase().equals(other_sign.toUpperCase());
    }


    public static void main(String[] args)
            throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
//        // 创建JSONObject对象
//        Map<String, Object> map = new LinkedHashMap<String, Object>();
//
//        // 向json中添加数据
//        map.put("RequestNo", "");
//        map.put("MerchantOrderNo", "IKLQM2SX9QP");
//        map.put("Channel", 2);
//        map.put("OrderPrice", "50.00");
//        map.put("CallBackUrl", "http://127.0.0.1:8889/callback/RRPAY");
//        map.put("Message", "IKLQM2SX9QA");
//        map.put("BankCardNo", "");
//        map.put("BankAccount", "");
//        map.put("OwnerPhone", "");
//        map.put("OwnerID", "");
//        map.put("PlatFormUserID", "");
//
//        System.out.println(JSON.toJSONString(map));
//        String str = encodeHexStr(encrypt("ImYOHmbLvfn5LpzD0ZBDXVs6pxcej2kk", JSON.toJSONString(map)), false);
//        System.out.print(str);
//
//        System.out.print("\n");
//        System.out.print(decrypt("9ce62c1836d128cfc875c9026db7564b", "98efc456143e68a8d8f65476603a5dc2"));

//     String  va  = AESUtil.decrypt(PublicParam.AES_KEY, "E700A7585ED6F0CF7A74A99CDF1D5F10AC50F37132AB3D84CC742CB1E1B72A8CD81BE34BFA166C4E8EB99C326A98D67D7FB6FD77221A3BFABC70FE7540783F5CF54474B549D29F2CB2C331A6D90C98E4E28A1A27318087E7EBF43F398564CCDB13BEB3DE9598396E70B5ED1BB15125F27C565FCD20BB32D53FD5328D2A1E29DD");
//        System.out.println("rrp res data :==>" + va );
    }
}