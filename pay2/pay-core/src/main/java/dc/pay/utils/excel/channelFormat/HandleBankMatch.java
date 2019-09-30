package dc.pay.utils.excel.channelFormat;
import com.google.common.collect.Sets;

import java.util.*;


public class HandleBankMatch {
    public static Map<String, String> bankMap;


    public HandleBankMatch(Map<String, String> bankMap) {
        this.bankMap = bankMap;
    }

    public  HashSet<String> handleMatch(String matchStr ) {
        List<String> resultList = new ArrayList<String>();
        HashSet<String> resultSet = Sets.newHashSet();
        String code, name;
        String[] nameArray;
        String findResult;
        for (Map.Entry<String, String> entry : bankMap.entrySet()) {
            code = entry.getKey();
            name = entry.getValue();
            nameArray = name.split(",");
            findResult = code + "," + nameArray[0];
            List<String> arrangeList = new ArrayList<String>();
            arrageArray(new String[nameArray.length],arrangeList, nameArray); // 如果有省份城市,重排其顺序以保证匹配的准确性
            for (String oneArrangeStr : arrangeList) {
                name = oneArrangeStr.replaceAll(",", "");
                // 处理BMP全字匹配的情况
                if ((KMPMatchString.kmpMatch(name, matchStr) || KMPMatchString.kmpMatch(matchStr, name)) && !resultList.contains(findResult)) {
                    resultList.add(findResult);
                }
            }
        }
        // Levenshtein 模糊算法
        if (resultList.size() > 0) {
            // 根据Levenshtein 模糊算法排序
            Collections.sort(resultList, new Comparator<String>() {
                public int compare(String s1, String s2) {
                    return LevenshteinMacthString.levenshteinMacth(s1.split(",")[1], matchStr) - LevenshteinMacthString.levenshteinMacth(s2.split(",")[1], matchStr);
                }
            });
        }
        resultList.forEach(list->{  resultSet.add(list.split(",")[1]); });
        return resultSet;
    }

    private static void arrageArray(String[] resultStr,List<String> arrangeList, String[] array) {
        int length = array.length;
        if (length == 1) {
            resultStr[resultStr.length - length] = array[0];
            // System.out.println(getStr(resultStr) + "--" + (++num));
            arrangeList.add(getStr(resultStr));
        } else {
            String[] newStrArray = new String[length - 1];
            for (int i = 0; i < length; i++) {
                int m = 0;
                resultStr[resultStr.length - length] = array[i];
                for (int k = 0; k < length - 1; k++) {
                    newStrArray[k] = array[k >= i ? i + (++m) : k];
                }
                // System.out.println("---"+new String(newCharArray));
                arrageArray(resultStr,arrangeList, newStrArray);
            }
        }
    }

    private static String getStr(String[] resultStr2) {
        String s = "";
        for (String string : resultStr2) {
            s += "," + string;
        }
        return "".equals(s) ? "" : s.substring(1);
    }



}
