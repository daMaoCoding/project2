package dc.pay.business.wuxingdaifu;

public class ErrorResp extends MerchantSign {
    private static final long serialVersionUID = 1L;

    private Integer code = 200;

    private String msg = null;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
