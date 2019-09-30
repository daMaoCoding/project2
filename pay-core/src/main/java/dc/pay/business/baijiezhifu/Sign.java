package dc.pay.business.baijiezhifu;

import java.util.Base64;

public class Sign {

    /**
     * rc4解密
     * @param data 需解密的数据
     * @param key 秘钥
     * @return 解密后的数据
     */
    public static byte[] decry_RC4(byte[] data, String key) {
        if (data == null || key == null) {
            return null;
        }
        return RC4Base(data, key);
    }
    /**
     * rc4 加密
     * @param data 需加密的数据
     * @param key 秘钥
     * @return 加密后的数据
     */
    public static byte[] encry_RC4_byte(String data, String key) {
        if (data == null || key == null) {
            return null;
        }
        byte b_data[] = data.getBytes();
        return RC4Base(b_data, key);
    }
    /**
     * rc4 加密
     * @param data 需加密的数据
     * @param key 秘钥
     * @return 加密后的数据
     */
    public static byte[] encry_RC4_byte(byte[] data, String key) {
        if (data == null || key == null) {
            return null;
        }
        return RC4Base(data, key);
    }
    /**
     * rc4 加密
     * @param data 需加密的数据
     * @param key 秘钥
     * @return 加密后的数据的base64形式
     */
    public static String encry_RC4_string(String data, String key) {
        if (data == null || key == null) {
            return null;
        }
        byte[] bytes = encry_RC4_byte(data, key);
        Base64.Encoder encoder = Base64.getEncoder();
        String s = encoder.encodeToString(bytes);
        return s;
    }
    private static byte[] RC4Base(byte[] input, String mKkey) {
        int x = 0;
        int y = 0;
        byte key[] = initKey(mKkey);
        int xorIndex;
        byte[] result = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            x = (x + 1) & 0xff;
            y = ((key[x] & 0xff) + y) & 0xff;
            byte tmp = key[x];
            key[x] = key[y];
            key[y] = tmp;
            xorIndex = ((key[x] & 0xff) + (key[y] & 0xff)) & 0xff;
            result[i] = (byte) (input[i] ^ key[xorIndex]);
        }
        return result;
    }
    private static byte[] initKey(String aKey) {
        byte[] b_key = aKey.getBytes();
        byte state[] = new byte[256];
        for (int i = 0; i < 256; i++) {
            state[i] = (byte) i;
        }
        int index1 = 0;
        int index2 = 0;
        if (b_key == null || b_key.length == 0) {
            return null;
        }
        for (int i = 0; i < 256; i++) {
            index2 = ((b_key[index1] & 0xff) + (state[i] & 0xff) + index2) & 0xff;
            byte tmp = state[i];
            state[i] = state[index2];
            state[index2] = tmp;
            index1 = (index1 + 1) % b_key.length;
        }
        return state;
    }

}
