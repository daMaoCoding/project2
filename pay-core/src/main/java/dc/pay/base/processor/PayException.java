package dc.pay.base.processor;

import dc.pay.constant.SERVER_MSG;
import org.apache.commons.lang.StringUtils;

/**
 * ************************
 * @author tony 3556239829
 */
public class PayException extends Exception {

    private static final long serialVersionUID = 1L;

    private Exception exception;
    private SERVER_MSG server_msg;


    public PayException(SERVER_MSG server_msg,Exception exception) {
        super(server_msg.getMsg());
        this.server_msg = server_msg;
        this.exception = exception;
    }

    public PayException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public String getMessage() {
        if(null!=this.getException() && !(this.getException() instanceof  PayException)) return this.getException().toString();
        if(null!=this.getException() &&  (this.getException() instanceof  PayException)) return this.getException().getMessage();

        if(StringUtils.isNotBlank(super.getMessage())) return super.getMessage();
        if(null!=getServer_msg())  return this.server_msg.getMsg();
        return "XC:未知支付异常,可能第三方接口返回[空]";
    }

    public PayException(SERVER_MSG server_msg) {
        super(server_msg.getMsg());
        this.server_msg = server_msg;
    }

    public PayException(String server_msg,Exception exception) {
        super(server_msg);
        this.exception = exception;
    }


    public PayException(String server_msg) {
        super(server_msg);
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public SERVER_MSG getServer_msg() {
        return this.server_msg;
    }

    public void setServer_msg(SERVER_MSG server_msg) {
        this.server_msg = server_msg;
    }


    public Exception getException() {
        return this.exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
