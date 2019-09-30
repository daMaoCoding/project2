package dc.pay.business.eboopay;
/**
 * ************************
 *
 * @author tony 3556239829
 */
public class EbooPayUtil {
    public enum ServerErrorMsg {
        E000000("-1", "系统临时维护中..."),
        E00000("00000", "成功"),
        E30001("30001", "金额错误"),
        E30002("30002", "订单号已存在"),
        E30003("30003", "签名验证失败"),
        E30004("30004", "银行编码错误"),
        E30005("30005", "系统银行编码错误"),
        E30006("30006", "系统错误"),
        E30007("30007", "通道账号获取失败"),
        E90002("90002", "商户编号不存在"),
        E90003("90003", "订单编号格式错误"),
        E90004("90004", "通道不可用"),
        E90005("90005", "域名错误"),
        E99999("99999", "无可用通道"),
        E99998("99998", "通道异常"),
        E99997("99997", "无可用水池");
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