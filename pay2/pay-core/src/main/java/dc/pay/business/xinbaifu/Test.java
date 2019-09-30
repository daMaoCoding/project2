package dc.pay.business.xinbaifu;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import dc.pay.utils.CopUtils;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.SortUtils;
import dc.pay.utils.kspay.AESUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws Exception {
        String str="{'transData': '38E77C2D4266EC1DEA1BEB057B666115B5126D44B394645C52F7E4CCC4F8DCC90288F038E92C95582498CAA41634D0869AF159A98E8E59DF92C46482F6355AFB1CA351928B4EA86160B53DF0F2029D48E26FC39F5D8A2D2982BCD284926126F79E41CF07F6103BA68C585BB5E7DBEDEA43F02BC4C5B8747F29C11BA16CE64979262F0ADA9D6DAD8C603DB863FA60E7F41C1094B877F4B331501F6D6B6387FBAA4898B5F33306015D3F22864D538C241F3A54591CFAFD525359ABFBA5EAD31CBDC6464A2B30D2A401A345B884C7ECC8A9FE905170234E2DA0F37A8DB0061F23C2348B8723F522B3A46083E94996A30DBE74AC555C5808BA4E9EC1280F67ED95F40FB8C5C39C4E2C5844BE2FE68E8737505BEEEBCEF715B7E0F17316411231466547B704AEA663634F7EA10825536279CBCB6921AE7099298BE89DB2A856ED8366C91C1541C2840B5423A687249ED41223'\n" + "}";
        String transData = JSON.parseObject(str).getString("transData");

        String aesStr = AESUtil.decrypt(transData, "P253WWICRZPN3H1C"); //AES解密
        String desStr = new String(Base64.decodeBase64(aesStr.getBytes())); //base64_decode 解密
        Map<String, String> payParam = HandlerUtil.urlToMap(desStr);


        System.out.println("返回的签名："+payParam.get("sign"));



        payParam.remove("sign");
        String param = SortUtils.sort(payParam);
        System.out.println("待签名的字符串："+param);



        List<String> ignoreParamNames = new ArrayList<String>(){
            {
                add("sign");
            }
        };

        String pay_md5sign = CopUtils.sign(payParam, ignoreParamNames,"P253WWICRZPN3H1C"); //验签
        System.out.println(pay_md5sign);


    }
}
