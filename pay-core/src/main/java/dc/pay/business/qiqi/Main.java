package dc.pay.business.qiqi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;
public class Main {
    private static final String realName = "胡迅";    //账户姓名
    private static final String cardNo = "6217681201348821";    //银行卡号
    private static final String idcardNo = "511623199211075053";    //身份证
    private static final String pkey = "b0a82b0216bd2d0b1ca0e9f7f4ffe937";
    private static final String code = "ZZ111701712822";     //商户code  个人中心查看
    private static final Long txAmount = 300L;  //提现金额  单位：分
    private static final String url = "http://219.234.6.70:8081/api/onepay/v2/tranAccount";
    public static String createSign(String Key,String characterEncoding,SortedMap<Object,Object> parameters){
        StringBuffer sb = new StringBuffer();
        Set es = parameters.entrySet();//所有参与传参的参数按照accsii排序（升序）
        Iterator it = es.iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String k = (String)entry.getKey();
            Object v = entry.getValue();
            if(null != v && !"".equals(v) 
                    && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k + "=" + v + "&");
            }
        }
        sb.append("key=" + Key);
        System.out.println("字符串拼接后是："+sb.toString());
        String sign = MD5Util.MD5Encode(sb.toString(), characterEncoding).toUpperCase();
        return sign;
    }
    
    public static void main(String[] args){
        //排序map
        SortedMap<Object,Object> sortedMap = new TreeMap<>();
        sortedMap.put("realname",realName);
        sortedMap.put("cardNo",cardNo);
        sortedMap.put("idcardNo",idcardNo);
        String newSign = createSign(pkey,"UTF-8",sortedMap);
        
        Map<String,Object> paramMap = new HashMap<String, Object>();
        paramMap.put("realname",realName);
        paramMap.put("cardNo",cardNo);
        paramMap.put("idcardNo",idcardNo);
        paramMap.put("txAmount",txAmount);
        paramMap.put("code",code);
        paramMap.put("notifyUrl", "http://www.baidu.com");
        paramMap.put("sign",newSign);
        
        String result = HttpPostUtils.doPost(url, paramMap);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(result);
        try {
            Map<String,Object> map = objectMapper.readValue(result,HashMap.class);
            
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
