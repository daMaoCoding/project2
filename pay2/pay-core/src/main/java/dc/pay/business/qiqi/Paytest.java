package dc.pay.business.qiqi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class Paytest {

    public Map<String, Object> Pay() {
        Map<String, Object> map = new HashMap<>();
        Long mon = 100L;
        Double tradeAmount = ((double) mon) / 100.00;
        String outOrderNo = ((Long) System.currentTimeMillis()).toString();
        String goodsClauses = "商品名称";
        String code = "Z111701712822";
        map.put("notifyUrl", "http://www.baidu.com");
        map.put("outOrderNo", outOrderNo);
        map.put("goodsClauses", goodsClauses);
        map.put("tradeAmount", "1.00");
        map.put("code", code);
        map.put("payCode", "weixinpay");
        SortedMap<Object, Object> sortMap = new TreeMap<>();
        sortMap.put("outOrderNo", outOrderNo);
        sortMap.put("goodsClauses", goodsClauses);
        sortMap.put("tradeAmount", "1.00");
        String sign = Main.createSign("b0a82b0216bd2d0b1ca0e9f7f4ffe937", "UTF-8", sortMap);
        map.put("sign", sign);
        return map;
    }

    public static String doPost(String url, Map<String, Object> map) {
        String result = null;
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Object> elem = (Entry<String, Object>) iterator.next();
            list.add(new BasicNameValuePair(elem.getKey(), elem.getValue() == null ? "" : elem.getValue().toString()));
        }
        try {

            if (list.size() > 0) {
                httpPost.setEntity(new UrlEncodedFormEntity(list, Charset.forName("utf-8")));
            }
            CloseableHttpResponse response = HttpClients.createDefault().execute(httpPost);

            try {
                if (response != null) {
                    HttpEntity resEntity = response.getEntity();
                    if (resEntity != null) {
                        result = EntityUtils.toString(resEntity);
                    }
                }
            } finally {
                if(response!=null) response.close();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void main(String[] args) {
        Paytest paytest = new Paytest();
        Map<String, Object> map = paytest.Pay();
        String result = paytest.doPost("http://219.234.6.70:8081/api/onepay/v2/pay", map);
        System.out.println(result);
    }
}
