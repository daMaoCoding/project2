package dc.pay.business.yidao;

import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author andrew
 * May 1, 2018
 */
public class YiDao2Util {

    // 排序
    public static String sort(Map paramMap) throws Exception {
        String sort = "";
        YiDaoMapUtil signMap = new YiDaoMapUtil();
        if (paramMap != null) {
            String key;
            for (Iterator it = paramMap.keySet().iterator(); it.hasNext();) {
                key = (String) it.next();
                String value = ((paramMap.get(key) != null) && (!("".equals(paramMap.get(key).toString())))) ? paramMap.get(key).toString() : "";
                signMap.put(key, value);
            }
            signMap.sort();
            for (Iterator it = signMap.keySet().iterator(); it.hasNext();) {
                key = (String) it.next();
                sort = sort + key + "=" + signMap.get(key).toString() + "&";
            }
            if ((sort != null) && (!("".equals(sort)))) {
                sort = sort.substring(0, sort.length() - 1);
            }
        }
        return sort;
    }
}

