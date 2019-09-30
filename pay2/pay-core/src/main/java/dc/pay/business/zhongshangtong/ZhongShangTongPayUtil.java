package dc.pay.business.zhongshangtong;/**
 * Created by admin on 2017/6/8.
 */

import dc.pay.base.processor.PayException;
import dc.pay.constant.SERVER_MSG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class ZhongShangTongPayUtil {
    private static final Logger log = LoggerFactory.getLogger(ZhongShangTongPayUtil.class);
    public static final String ENCODE = "UTF-8";

    public static String generatePayRequest(Map<String, String> paramsMap, String api_key) throws PayException {
        try {
            StringBuffer sb = new StringBuffer()
                    .append("userid=").append(paramsMap.get("userid").trim())
                    .append("&orderid=").append(paramsMap.get("orderid").trim())
                    .append("&bankid=").append(paramsMap.get("bankid").trim())
                    .append("&keyvalue=").append(api_key.trim());
            return sb.toString();
        } catch (Exception ex) {
            log.error("[中商通_HelibaoPayUtil]将由支付请求参数构成的map转换成支付串，并对参数做合法验证出错。" + ex.getMessage(), ex);
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
    }

    public static String disguiseMD5(String message) {
        if (null == message) {
            return null;
        }
        return disguiseMD5(message, ENCODE);
    }

    public static String disguiseMD5(String message, String encoding) {
        if (null == message || null == encoding) {
            return null;
        }
        message = message.trim();
        byte value[];
        try {
            value = message.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            value = message.getBytes();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return toHex(md.digest(value));
    }

    public static String disguise(String message, String encoding) {
        message = message.trim();
        byte value[];
        try {
            value = message.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            value = message.getBytes();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return toHex(md.digest(value));
    }

    public static String toHex(byte input[]) {
        if (input == null)
            return null;
        StringBuffer output = new StringBuffer(input.length * 2);
        for (int i = 0; i < input.length; i++) {
            int current = input[i] & 0xff;
            if (current < 16)
                output.append("0");
            output.append(Integer.toString(current, 16));
        }
        return output.toString();
    }

    public static enum ServerErrorMsg {
        E0000("-1", "系统忙"),
        E0002("1", "商户订单号无效"),
        E0003("2", "银行编码错误"),
        E0005("3", "商户不存在"),
        E8001("4", "验证签名失败"),
        E8101("5", "商户扫码通道被关闭"),
        E8102("6", "金额超出限额");
        String code;
        String msg;
        ServerErrorMsg(String code, String msg) {
            this.code = code;
            this.msg = msg;
        }
        public String getCode() {
            return code;
        }
        public String getMsg() {
            return msg;
        }
        public static String getMsgByCode(String code) {
            String msg = "无此错误代码：" + code;
            for (ServerErrorMsg c : ServerErrorMsg.values()) {
                if (c.getCode().equalsIgnoreCase(code)) {
                    return code + ":" + c.getMsg();
                }
            }
            return msg;
        }
    }
}