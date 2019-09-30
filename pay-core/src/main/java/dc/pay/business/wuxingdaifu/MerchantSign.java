package dc.pay.business.wuxingdaifu;

import java.io.Serializable;

public class MerchantSign implements Serializable {
    //("签名时间, 格式yyyyMMddHHmmss")
    private String signTime;
    //("签名")
    private String sign;

    public String getSignTime() {
        return signTime;
    }

    public void setSignTime(String signTime) {
        this.signTime = signTime;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
