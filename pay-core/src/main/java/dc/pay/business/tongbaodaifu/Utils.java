package dc.pay.business.tongbaodaifu;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;

public class Utils {
    
    public static Map<String,String> pairListToMap(List<NameValuePair> list){
        Map<String,String> result = new HashMap<String,String>();
        for(NameValuePair nameValuePair:list) {
            result.put(nameValuePair.getName(), nameValuePair.getValue());
        }
        return result;
    }
    
    /**
     * @desc beanתmap
     * @author nicholas
     * @date 2019��1��16��
     * @param obj
     * @return
     */
    public static Map<String, String> transBean2Map(Object obj) {

        if (obj == null) {
            return null;
        }
        Map<String, String> map = new HashMap<String, String>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();

                // ����class����
                if (!key.equals("class")) {
                    // �õ�property��Ӧ��getter����
                    Method getter = property.getReadMethod();
                    String value = (String) getter.invoke(obj);

                    map.put(key, value);
                }

            }
        } catch (Exception e) {
        }

        return map;

    }

}
