package dc.pay.utils.ipUtil.qqwry.entry;

import java.util.*;


public class CityDirectly {
    public final static Map<String, String> CITY_DIRECTLY_MAP = new HashMap<String, String>();
    static {
        CITY_DIRECTLY_MAP.put("北京", "直辖市");
        CITY_DIRECTLY_MAP.put("天津", "直辖市");
        CITY_DIRECTLY_MAP.put("重庆", "直辖市");
        CITY_DIRECTLY_MAP.put("上海", "直辖市");
    }
    public static Set<String> getCitySet(){
        return CITY_DIRECTLY_MAP.keySet();
    }
}
