package dc.pay.business.xinzhinengyundaifu.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class WebUtils {
  
    
    /**
     * 获取所有请求参数
     * @param request
     * @return
     */
    public static Map<String,String> getParameterMap(HttpServletRequest request) {
    	Enumeration<String> names = request.getParameterNames();
    	Map<String,String> params = new HashMap<String,String>();
    	String key;
    	while(names.hasMoreElements()) {
    		 key = names.nextElement();
    		 params.put(key, request.getParameter(key));
    	}
    	return params;
    }
}
