package dc.pay.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
 public class SortUtils<E> {
     private static final Logger log = LoggerFactory.getLogger(SortUtils.class);

     // 排序
     public static String sort(Map paramMap) throws Exception {
         String sort = "";
         TLinxMapUtil signMap = new TLinxMapUtil();
         if (paramMap != null) {
             String key;
             for (Iterator it = paramMap.keySet().iterator(); it.hasNext(); ) {
                 key = (String) it.next();
                 String value = ((paramMap.get(key) != null) && (!(""
                         .equals(paramMap.get(key).toString())))) ? paramMap
                         .get(key).toString() : "";
                 signMap.put(key, value);
             }
             signMap.sort();
             for (Iterator it = signMap.keySet().iterator(); it.hasNext(); ) {
                 key = (String) it.next();
                 sort = sort + key + "=" + signMap.get(key).toString() + "&";
             }
             if ((sort != null) && (!("".equals(sort)))) {
                 sort = sort.substring(0, sort.length() - 1);
             }
         }
         return sort;
     }


     public int compareByType(Class<?> type,Method m1,Method m2,E a, E b){
         int ret = 0;
         try {
             if (type == int.class) {
                 ret = ((Integer) m2.invoke(((E) b), null)).compareTo((Integer) m1.invoke(((E) a), null));
             } else if (type == double.class) {
                 ret = ((Double) m2.invoke(((E) b), null)).compareTo((Double) m1.invoke(((E) a), null));
             } else if (type == long.class) {
                 ret = ((Long) m2.invoke(((E) b), null)).compareTo((Long) m1.invoke(((E) a), null));
             } else if (type == float.class) {
                 ret = ((Float) m2.invoke(((E) b), null)).compareTo((Float) m1.invoke(((E) a), null));
             } else if (type == Date.class) {
                 ret = ((Date) m2.invoke(((E) b), null)).compareTo((Date) m1.invoke(((E) a), null));
             } else {
                 ret = m2.invoke(((E) a), null).toString().compareTo(m1.invoke(((E) b), null).toString());
             }
         }catch (Exception e){
             log.error(e.getMessage(),e);
         }

         return ret;
     }

     public void Sort(List<E> list, final String method, final String sort) {
         // 用内部类实现排序
         Collections.sort(list, new Comparator<E>() {
             public int compare(E a, E b) {
                 int ret = 0;
                 try {
                     Method m1 = a.getClass().getMethod(method, null); m1.setAccessible(true);Class<?> type = m1.getReturnType();
                     Method m2 = b.getClass().getMethod(method, null);
                     m2.setAccessible(true);
                     if (sort != null && "desc".equals(sort)) {
                         ret = compareByType(type,m1,m2, a,  b);
                     } else {
                         // 升序排序
                         ret = compareByType(type,m2,m1,b, a);
                     }
                 } catch (Exception e) {
                     log.error(e.getMessage(), e);
                 }
                 return ret;
             }
         });
     }
 }


