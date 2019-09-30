package dc.pay.business.jinniuzhifu;

import java.util.HashMap;

import com.google.common.collect.Maps;

import dc.pay.utils.RestTemplateUtil;

public class Test {
    /**
     * 
     * 
     * @author andrew
     * Aug 17, 2019
     */
    @org.junit.Test
    public void test() {
        String url = "https://api.jzpay.vip/jzpay_exapi/v1/order/createOrder";
        HashMap<String , String> map = Maps.newHashMap();
        map.put("timestamp","1566200098649");
        map.put("signatureMethod","HmacSHA256");
        map.put("signatureVersion","1");
        map.put("signature","4b7c6fe2aca100401f8229cf99aa17e95bb4658e6b7bb4b8de63f7fa804e7bcf");
        map.put("merchantId","2d40827f8a0fc3c17a00ec21c852f1c2");
        //"merchantId":"2d40827f8a0fc3c17a00ec21c852f1c2"
       // {"timestamp":"1566199714417","signatureMethod":"HmacSHA256","signatureVersion":"1","signature":"dc141b2d1f8a110894d19671d5260e38af56df36a892ea7aebbd1ea9cfc0d670"}
        String s = RestTemplateUtil.postForm(url, map, null);
        System.out.println("========"+s);
    }
}
