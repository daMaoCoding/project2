package dc.pay.utils.kspay;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;


public class BASEUtil {
    static Logger loger = Logger.getLogger(BASEUtil.class);


    public static String encode(String data) {
        try {
            return new BASE64Encoder().encode(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            loger.error("base64 utf8 编码错误", e);
        }
        return null;
    }


    public static String decode(String str) {
        byte[] bt = (byte[]) null;
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            bt = decoder.decodeBuffer(str);
            return new String(bt, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

