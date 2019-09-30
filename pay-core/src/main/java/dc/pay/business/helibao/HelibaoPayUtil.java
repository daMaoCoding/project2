package dc.pay.business.helibao;/**
 * Created by admin on 2017/6/8.
 */

import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.constant.PayEumeration;
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
public class HelibaoPayUtil {
    private static final Logger log = LoggerFactory.getLogger(HelibaoPayUtil.class);
    public static final String split = "&";
    public static final String ENCODE = "UTF-8";

    public static Map<String, String> parseBankFlag(String flag) {
        if (!flag.isEmpty() && flag.contains(PayEumeration.split)) {
            Map<String, String> bankFlags = Maps.newHashMap();
            String[] split = flag.split(PayEumeration.split);
            if (null != split && split.length == 3 && "AppPay".equalsIgnoreCase(split[0])) {
                bankFlags.put("P1_bizType", split[0]);
                bankFlags.put("P4_payType", split[1]);
                bankFlags.put("P8_appType", split[2]);
                return bankFlags;
            } else if (null != split && split.length == 3 && "OnlinePay".equalsIgnoreCase(split[0])) {
                bankFlags.put("P1_bizType", split[0]);
                bankFlags.put("P5_bankId", split[1]);
                bankFlags.put("P6_business", split[2]);
                return bankFlags;
            }
        }
        return null;
    }

    public static String generatePayRequest(Map<String, String> paramsMap) throws PayException {
        try {
            String P1_bizType = paramsMap.get("P1_bizType");
            if ("AppPay".equalsIgnoreCase(P1_bizType)) {
                StringBuffer sb = new StringBuffer()
                        .append(split).append(paramsMap.get("P1_bizType").trim())
                        .append(split).append(paramsMap.get("P2_orderId").trim())
                        .append(split).append(paramsMap.get("P3_customerNumber").trim())
                        .append(split).append(paramsMap.get("P4_payType").trim())
                        .append(split).append(paramsMap.get("P5_orderAmount").trim())
                        .append(split).append(paramsMap.get("P6_currency").trim())
                        .append(split).append(paramsMap.get("P7_authcode").trim())
                        .append(split).append(paramsMap.get("P8_appType").trim())
                        .append(split).append(paramsMap.get("P9_notifyUrl").trim())
                        .append(split).append(paramsMap.get("P10_successToUrl").trim())
                        .append(split).append(paramsMap.get("P11_orderIp").trim())
                        .append(split).append(paramsMap.get("P12_goodsName").trim())
                        .append(split).append(paramsMap.get("P13_goodsDetail").trim())
                        .append(split).append(paramsMap.get("P14_desc").trim());
                return sb.toString();
            } else if ("OnlinePay".equalsIgnoreCase(P1_bizType)) {
                StringBuffer sb = new StringBuffer()
                        .append(split).append(paramsMap.get("P1_bizType").trim())
                        .append(split).append(paramsMap.get("P2_orderId").trim())
                        .append(split).append(paramsMap.get("P3_customerNumber").trim())
                        .append(split).append(paramsMap.get("P4_orderAmount").trim())
                        .append(split).append(paramsMap.get("P5_bankId").trim())
                        .append(split).append(paramsMap.get("P6_business").trim())
                        .append(split).append(paramsMap.get("P7_timestamp").trim())
                        .append(split).append(paramsMap.get("P8_goodsName").trim())
                        .append(split).append(paramsMap.get("P9_period").trim())
                        .append(split).append(paramsMap.get("P10_periodUnit").trim())
                        .append(split).append(paramsMap.get("P11_callbackUrl").trim())
                        .append(split).append(paramsMap.get("P12_serverCallbackUrl").trim())
                        .append(split).append(paramsMap.get("P13_orderIp").trim())
                        .append(split).append(paramsMap.get("P14_onlineCardType").trim())
                        .append(split).append(paramsMap.get("P15_desc").trim());
                return sb.toString();
            }
        } catch (Exception ex) {
            log.error("[合利宝_HelibaoPayUtil]将由支付请求参数构成的map转换成支付串，并对参数做合法验证出错。" + ex.getMessage(), ex);
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        return null;
    }

    public static String signData(String sourceData, String key) throws PayException {
        sourceData = sourceData.concat(split).concat(key);
        String sign = disguiseMD5(sourceData);
        return sign;
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
        E0000("0000", "成功"),
        E0002("0002", "订单已存在"),
        E0003("0003", "失败"),
        E0005("0005", "单笔金额超过最大限额"),
        E8001("8001", "订单号不唯一"),
        E8101("8101", "订单金额不正确"),
        E8102("8102", "订单不存在"),
        E8103("8103", "订单状态异常"),
        E8104("8104", "订单对应的渠道未在系统中配置"),
        E8105("8105", "退款金额超过了订单实付金额"),
        E8106("8106", "渠道请求交互验签错误"),
        E8107("8107", "订单已过期"),
        E8108("8108", "订单已存在请更换订单号重新下单"),
        E8109("8109", "商户未开通此银行");
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