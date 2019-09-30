package dc.pay.business.yinbang;

import dc.pay.utils.Base64Local;
import dc.pay.utils.SecurityRSAPay;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class YinBangUtil {



    /**
     * 验签和解密
     */
    public static  JsonResult verifyAndDecrypt(String encParam,String sign,String serverPublicKey,String privateKey){

        if (StringUtils.isEmpty(encParam) || StringUtils.isEmpty(sign)) {
            return new JsonResult("0001", "数据异常");
        }
        // 服务器公钥验签
        boolean flag;
        try {
            flag = SecurityRSAPay.verify(Base64Local.decode(encParam),Base64Local.decode(serverPublicKey),Base64Local.decode(sign));
            // 验签失败
            if (!flag) {
                // 商户出错处理
                System.out.println("验签失败");
                return new JsonResult("0001", "验签失败");
            }
            // 商户自己私钥解密
            String data = new String(SecurityRSAPay.decryptByPrivateKey(Base64Local.decode(encParam),Base64Local.decode(privateKey)), "utf-8");
            return new JsonResult("1000", "成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return new JsonResult("0001", "系统异常");
        }

    }
}
