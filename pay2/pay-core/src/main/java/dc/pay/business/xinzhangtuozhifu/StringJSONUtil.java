package dc.pay.business.xinzhangtuozhifu;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author andrew
 * Aug 12, 2019
 */
public class StringJSONUtil {

    public static Map<String, Object> paramsParse(
            String params) {
        Map<String, Object> mapRequest = new HashMap<>(16);

        String[] arrSplit = null;

        if (params == null) {
            return mapRequest;
        }
        arrSplit = params.split("[&]");
        for (String strSplit : arrSplit) {
            String[] arrSplitEqual = null;
            arrSplitEqual = strSplit.split("[=]");
            // 解析出键值
            if (arrSplitEqual.length > 1) {// 正确解析
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);
            } else if (arrSplitEqual[0] != "") { // 只有参数没有值，不加入
                mapRequest.put(arrSplitEqual[0], "");
            }
        }
        return mapRequest;
    }

    public static Map<String, String> urlParse(
            String URL) {
        Map<String, String> mapRequest = new HashMap<>();

        String[] arrSplit = null;

        String strUrlParam = TruncateUrlPage(URL);
        if (strUrlParam == null) {
            return mapRequest;
        }
        arrSplit = strUrlParam.split("[&]");
        for (String strSplit : arrSplit) {
            String[] arrSplitEqual = null;
            arrSplitEqual = strSplit.split("[=]");
            // 解析出键值
            if (arrSplitEqual.length > 1) {// 正确解析
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);
            } else if (arrSplitEqual[0] != "") { // 只有参数没有值，不加入
                mapRequest.put(arrSplitEqual[0], "");
            }
        }
        return mapRequest;
    }

    /**
     * 去掉url中的路径，留下请求参数部分
     * 
     * @param strURL
     *            url地址
     * @return url请求参数部分
     */
    private static String TruncateUrlPage(
            String strURL) {
        String strAllParam = null;
        String[] arrSplit = null;

        arrSplit = strURL.split("[?]");
        for (String string : arrSplit) {
            System.out.println(string);
        }
        if (strURL.length() > 1 && arrSplit.length > 1 && arrSplit[1] != null) {
            strAllParam = arrSplit[1];
        }

        return strAllParam;
    }
}
