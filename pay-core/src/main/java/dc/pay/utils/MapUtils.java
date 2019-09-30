package dc.pay.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MapUtils{
    private static Logger log = LoggerFactory.getLogger(MapUtils.class);

    /**
	 * 按参数名称排序获取key=value&key=value
	 * @param param 参数map
	 * @param ignoreEmpty 是否忽略掉为空的参数
	 * @return
	 */
	public static String getSignStrByTreeMap(Map<String,? extends Object> param,boolean ignoreEmpty){
		Map<String,Object> paramMap = new TreeMap<String, Object>();
		paramMap.putAll(param);
		return getSignStrByMap(paramMap,ignoreEmpty);
	}

	/**
	 * key=value&key=value
	 * @param param 参数map
	 * @param ignoreEmpty 是否忽略掉为空的参数
	 * @return
	 */
	public static String getSignStrByMap(Map<String,? extends Object> param,boolean ignoreEmpty){
		
		if(param == null  || param.isEmpty()){
			return "";
		}
		StringBuffer str = new StringBuffer("");
		int i = 0;
		Object obj = null;
		for (String key : param.keySet()) {
			obj = param.get(key);
			//忽略空值
			if(ignoreEmpty){
				if(obj == null || StringUtils.isEmpty(obj.toString())){
					continue;
				}
			}
			
			if(i > 0){
				str.append("&");
			}
			str.append(key).append("=").append(param.get(key));
			i++;
		}
		return str.toString();
	}

	/**
	 * @param parameters
	 * @param keys
	 *            需要重组map的key数组
	 * @return LinkedHashMap 按keys数组中的顺序排列
	 */
	public static Map<String, Object> recombinationMap(Map<String, Object> parameters, String... keys) {
		Map<String, Object> results = new LinkedHashMap<String, Object>();
		for (String key : keys) {
			if (parameters.containsKey(key)) {
				results.put(key, parameters.get(key));
			}
		}
		return results;
	}



	/**
	 * [公用] -按map(key)升序生成list(keys)
	 * @param map
	 * @return
	 */
	public static List<String> sortMapByKeyAsc(Map map) {
//		Object[] key = map.keySet().toArray();
//		Arrays.sort(key);
//      List<String> keys = new ArrayList(map.keySet());
//      Collections.sort(keys);
		ArrayList<String> mapKeyList = new ArrayList<>();
		List<Map.Entry<String,String>> list = new ArrayList<Map.Entry<String,String>>(map.entrySet());
		if(!list.isEmpty()){
			Collections.sort(list,new Comparator<Map.Entry<String,String>>() {
				//升序排序
				public int compare(Map.Entry<String, String> o1,Map.Entry<String, String> o2) {
					//return o1.getValue().compareTo(o2.getValue());
					return o1.getKey().compareTo(o2.getKey());
				}
				
			});
			for(Map.Entry<String,String> mapping:list){
				//System.out.println(mapping.getKey()+":"+mapping.getValue());
				mapKeyList.add(mapping.getKey());
			}
		}
		return mapKeyList;
	}
	
	/**
	 * 
	 * @param map
	 * @return
	 * @author andrew
	 * Apr 19, 2018
	 */
	@Deprecated
	public static Map sortMapByKeyAsc2(Map map) {
		List<Map.Entry<String,String>> list = new ArrayList<Map.Entry<String,String>>(map.entrySet());
		if(!list.isEmpty()){
			Collections.sort(list,new Comparator<Map.Entry<String,String>>() {
				//升序排序
				public int compare(Map.Entry<String, String> o1,Map.Entry<String, String> o2) {
					//return o1.getValue().compareTo(o2.getValue());
					return o1.getKey().compareTo(o2.getKey());
				}
			});
		}
		return map;
	}



	/**
	 * [公用] -创建倒序Map
	 * @return
	 */
	public static Map<String,String> getDescMap(){
		Map<String, String> map = new TreeMap<String, String>(
				new Comparator<String>() {
					public int compare(String obj1, String obj2) {
						// 降序排序
						return obj2.compareTo(obj1);
					}
				});
		return map;
	}





}
