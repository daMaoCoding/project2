package dc.pay.business.onegopayzhifu;

import java.util.Map;

/**
 * @author Cobby
 * May 18, 2019
 */
public class Hasher {

    public static String encode(Map<String, String> data) {
        StringBuilder json = new StringBuilder();

        json.append("{");
        for (Object key : data.keySet()) {
            json.append(getJSONValue((String) key) + ":");
            json.append(getJSONValue(data.get(key)));
            json.append(",");
        }
        json.deleteCharAt(json.length() - 1);
        json.append("}");

        return json.toString();
    }

    private static String getJSONValue(String s) {
        return "\"" + utf8ToUnicode(addSlashes(s)) + "\"";
    }
    public static String utf8ToUnicode(String inStr) {
        char[] myBuffer = inStr.toCharArray();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < inStr.length(); i++) {
            Character.UnicodeBlock ub = Character.UnicodeBlock.of(myBuffer[i]);
            if (ub == Character.UnicodeBlock.BASIC_LATIN) {
                sb.append(myBuffer[i]);
            } else if (ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
                int j = (int) myBuffer[i] - 65248;
                sb.append((char) j);
            } else {
                short s = (short) myBuffer[i];
                String hexS = Integer.toHexString(s);
                String unicode = "\\u" + hexS.substring(hexS.length() - 4, hexS.length());
                sb.append(unicode.toLowerCase());
            }
        }
        return sb.toString();
    }


    private static String addSlashes(String s) {
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replaceAll("\\n", "\\\\n");
        s = s.replaceAll("\\r", "\\\\r");
        s = s.replaceAll("\\00", "\\\\0");
        s = s.replaceAll("'", "\\\\'");

        return s;
    }

}
