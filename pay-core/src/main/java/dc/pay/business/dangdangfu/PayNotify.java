package dc.pay.business.dangdangfu;

import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by paul on 2017/8/25.
 */
@WebServlet(name = "PayNotify")
public class PayNotify extends HttpServlet {
    
    final static String CHARSET = "UTF-8";
    final static String MER_SEC = "UTF-8";
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 将请求、响应的编码均设置为容器编码（防止中文乱码）
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("GBK");
        String string;
        System.out.println("当前时间:" + new Date());
        StringBuffer stringBuffer = new StringBuffer();
        System.out.println("==============接收到交易通知===================");
        System.out.println("============================================");
        Map map = new HashMap();
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues.length == 1) {
                String paramValue = paramValues[0];
                if (paramValue.length() != 0) {
                    map.put(paramName, paramValue);
                }
            }
        }
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry entry : set) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
            stringBuffer.append(entry.getKey() + ":" + entry.getValue()
                    + "\r\n");
        }
        try {
            map.remove("signature");
            string = signature(map, MER_SEC);
            stringBuffer.append("计算签名:" + string + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("===============END====================");
        System.out.println("======================================");
        // 响应消息
        PrintWriter out = response.getWriter();
        out.print("success");
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected static String signature(Map<String, String> param, String keyValue)
            throws Exception {
        Set<String> set = param.keySet();
        List<String> keys = new ArrayList<String>(set);
        Collections.sort(keys);
        boolean start = true;
        StringBuffer sb = new StringBuffer();
        for (String key : keys) {
            String value = param.get(key);
            if (value != null && !value.trim().equals("")
                    && !"signature".equalsIgnoreCase(key)) {
                if (!start) {
                    sb.append("&");
                }
                sb.append(key + "=" + value);
                start = false;
            }
        }
        sb.append("&" + keyValue);
        String src = sb.toString();
        System.out.println("签名数据:" + src);
        String result = DigestUtils.md5Hex(src.getBytes(CHARSET)).toUpperCase();
        System.out.println("签名结果:" + result);
        return result;
    }
}
