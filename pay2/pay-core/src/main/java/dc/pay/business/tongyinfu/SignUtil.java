package dc.pay.business.tongyinfu;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by xt on 2016/11/14.
 */
public class SignUtil {

    static final Logger logger = LoggerFactory.getLogger(SignUtil.class);

    public static String getSignData(Map map){
        StringBuilder retStr = new StringBuilder();
        //获取map集合中的所有键，存入到Set集合中，
        Set<Map.Entry<String,String>> entry = map.entrySet();
        //通过迭代器取出map中的键值关系，迭代器接收的泛型参数应和Set接收的一致
        Iterator<Map.Entry<String,String>> it = entry.iterator();
        while (it.hasNext())
        {
            //将键值关系取出存入Map.Entry这个映射关系集合接口中
            Map.Entry<String,String>  me = it.next();
            //使用Map.Entry中的方法获取键和值
            String key = me.getKey();
            String value = me.getValue();
            retStr.append(key + "=" + value+"&");
        }
        return retStr.toString().substring(0,retStr.length()-1);
    }


    public static Map<String,String> getMapData(String data){
        Map<String,String> returnMap = new TreeMap<>();
        String [] datas = data.split("&");
        for (int i =0;i<datas.length;i++){
            String [] temp = datas[i].split("=");
            StringBuffer tempBuff = new StringBuffer();
            for (int j=1;j<temp.length;j++){
                tempBuff.append(temp[j]+"=");
            }
            if (temp.length != 1) {
                returnMap.put(temp[0],tempBuff.toString().substring(0,tempBuff.length()-1));
            }else{
                returnMap.put(temp[0],"");
            }
        }
        return returnMap;
    }

    public static Map<String,String> jsonToMap(JSONObject jsonStr){
        Map<String,String> returnMap = new TreeMap<>();
        try {
            Set<String> set = jsonStr.keySet();
            Iterator i = set.iterator();
            while (i.hasNext()){
                String key = String.valueOf(i.next());
                String value = (String) jsonStr.get(key);
                returnMap.put(key, value);
            }
            return returnMap;
        } catch (Exception e) {
            logger.error("数据转换异常！");
        }
        return null;
    }

    public static void main(String [] args){
        Map a = getMapData("merchantId=801551059350132&signType=MD5&version=1.0.0&type=zxWxQueryOr=der&result=S&returnMsg=交易成功&amount=0.01&orderDate=");
        System.out.println(a.get("type"));
        System.out.println(System.getProperty("user.dir")+"//");
    }

}
