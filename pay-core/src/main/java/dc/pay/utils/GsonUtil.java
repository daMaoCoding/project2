package dc.pay.utils;

import java.lang.reflect.Type;

import com.google.gson.Gson;

public class GsonUtil {
	
	/**
     * 将JSON字符串转换成JAVA对象
     * 适用处理一些简单的对象
     * <code>
     *     Person p = GsonUtils.fromJson(json, Person.class);
     * </code>
     *
     * @param json json 字符串
     * @param clazz 目标对象的Class类型
     * @param <T> 泛型对象
     * @return 目标对象
     */
	public static <T> T fromJson(String json, Class<T> clazz){
		Gson gson = new Gson();
		return gson.fromJson(json, clazz);
	}
    /**
     *
     * 将一些复杂的json字符串转成复杂的java集合对象时 {@link #fromJson(String, Class)} 无法处理
     * 可以自定义目标对象 {@link com.google.gson.reflect.TypeToken},得到对象类型
     * 例如：将json字符串转找成List对象
     * <code>
     *      Type type = new TypeToken<List<Person>>() {}.getType();
     *      List<Person> person = GsonUtils.formJson(json, type);
     * </code>
     *
     * @param json json 字符串
     * @param type 目标对象类型
     * @param <T> 泛型对象
     * @return 返回目标对象
     */
	public static <T> T formJson(String json, Type type){
		Gson gson = new Gson();
		return gson.fromJson(json, type);
	}
	

    /**
     * 将对象转换成json字符串
     * @param obj java 对象
     * @return json 字符串
     */
	public static String toJson(Object obj){
		Gson gson = new Gson();
		return gson.toJson(obj);
	}
}
