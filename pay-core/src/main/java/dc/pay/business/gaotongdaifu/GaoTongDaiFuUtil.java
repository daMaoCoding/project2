package dc.pay.business.gaotongdaifu;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class GaoTongDaiFuUtil {

    private static final String Algorithm = "DESede"; // 定义 加密算法,可用
   // private static final String Transformation = "DESede/ECB/NoPadding";
      private static final String Transformation = "DESede/ECB/PKCS5Padding";

    static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7','8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    // / <summary>
    // / 3des加密
    // / </summary>
    // / <param name="value">待加密字符串</param>
    // / <param name="strKey">原始密钥字符串</param>
    // / <returns></returns>
    public static String Encrypt3DES(String value, String key) throws Exception {
        String str = byte2Base64(encryptMode(key.getBytes(), value.getBytes("UTF-8")));
        return str;
    }


    public static String Decrypt3DES(String value, String key) throws Exception {
        String str = decryptMode(key.getBytes(), base64Tobyte(value.getBytes("UTF-8")));
        return str;
    }

    // keybyte为加密密钥，长度为24字节
    // src为被加密的数据缓冲区（源）
    public static byte[] encryptMode(byte[] keybyte, byte[] src) {
        try {
            // 生成密钥
            SecretKey deskey = new SecretKeySpec(keybyte, Algorithm); // 加密
            Cipher c1 = Cipher.getInstance(Algorithm);
            c1.init(Cipher.ENCRYPT_MODE, deskey);
            return c1.doFinal(src);
        } catch (java.security.NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (javax.crypto.NoSuchPaddingException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        return null;
    }




    public static String decryptMode(byte[] keybyte, byte[] src) {
        try {
            SecretKey deskey = new SecretKeySpec(keybyte, Algorithm );
            Cipher c1 = Cipher.getInstance(Algorithm);
            c1.init(Cipher.DECRYPT_MODE, deskey);
            byte[] data = c1.doFinal(src);
            return new String(data);
        } catch (java.security.NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (javax.crypto.NoSuchPaddingException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        return null;
    }





    // 转换成base64编码
    public static String byte2Base64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    // 转换成base64编码
    public static byte[]  base64Tobyte(byte[] b) {
        return Base64.getDecoder().decode(b);
    }




    /**
     * http请求方法
     * @param url
     * @param params
     * @return
     */
    public static String request(String url, String params) {
        try {
            System.out.println("请求报文:" + params);
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj
                    .openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(1000 * 5);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Content-Length", String.valueOf(params.length()));
            OutputStream outStream = conn.getOutputStream();
            outStream.write(params.toString().getBytes("UTF-8"));
            outStream.flush();
            outStream.close();
            return getResponseBodyAsString(conn.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getResponseBodyAsString(InputStream in) {
        try {
            BufferedInputStream buf = new BufferedInputStream(in);
            byte[] buffer = new byte[1024];
            StringBuffer data = new StringBuffer();
            int readDataLen;
            while ((readDataLen = buf.read(buffer)) != -1) {
                data.append(new String(buffer, 0, readDataLen, "UTF-8"));
            }
            System.out.println("响应报文=" + data);
            return data.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * md5加密方法
     * @param s
     * @param encoding
     * @return
     */
    public final static String MD5(String s, String encoding) {
        try {
            byte[] btInput = s.getBytes(encoding);
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String mapToParamByKeysSort(Map<String, String> params) {
        List<String> keys = new ArrayList<String>(params.keySet());
        Collections.sort(keys);
        String prestr = "";
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
                prestr = prestr + key + "=" + value;
            } else {
                prestr = prestr + key + "=" + value + "&";
            }
        }
        return prestr;
    }
    /**
     * map转换成json
     * @param map
     * @return
     */
    public static String mapToJson(Map<String, String> map) {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        StringBuffer json = new StringBuffer();
        json.append("{");
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            String key = entry.getKey();
            String value = entry.getValue();
            json.append("\"").append(key).append("\"");
            json.append(":");
            json.append("\"").append(value).append("\"");
            if (it.hasNext()) {
                json.append(",");
            }
        }
        json.append("}");
        System.out.println("mapToJson=" + json.toString());
        return json.toString();
    }

    /**
     * 生成随机数 字母加数字
     * @param num
     * @return
     */
    public static String randomStr(int num) {
        char[] randomMetaData = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g',
                'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
                't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E',
                'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
                'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9' };
        Random random = new Random();
        String tNonceStr = "";
        for (int i = 0; i < num; i++) {
            tNonceStr += (randomMetaData[random
                    .nextInt(randomMetaData.length - 1)]);
        }
        return tNonceStr;
    }





}
