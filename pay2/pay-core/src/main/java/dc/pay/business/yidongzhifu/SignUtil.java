package dc.pay.business.yidongzhifu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class SignUtil {

	public static String sign(Map dataMap, String key) {
		ArrayList<String> dataList = new ArrayList();
		Iterator iterator = dataMap.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, Object> entry = (Map.Entry) iterator.next();
			if (isNotEmpty(entry.getValue()) && !"sign".equals(entry.getKey())) {
				dataList.add(entry.getKey() + "=" + entry.getValue() + "&");
			}
		}

		int size = dataList.size();
		String[] arrayToSort = dataList.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < size; ++i) {
			sb.append(arrayToSort[i]);
		}

		String result = sb.toString();
		result = result + "key=" + key;
		result = MD5.md5ToHex(result).toUpperCase();
		return result;
	}

	public static boolean verify(Map dataMap, String key, String signValue) {
		return signValue == null ? false : signValue.equals(sign(dataMap, key));
	}

	private static boolean isNotEmpty(Object value) {
		if (value == null) {
			return false;
		}
		if (value.toString().trim().length() == 0) {
			return false;
		}
		return true;
	}
}
