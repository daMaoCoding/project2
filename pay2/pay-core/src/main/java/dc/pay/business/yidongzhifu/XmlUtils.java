package dc.pay.business.yidongzhifu;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class XmlUtils {

	public static Map<String, String> xmlToMap(String res) {

		Document document = null;
		try {
			document = DocumentHelper.parseText(res);
		} catch (DocumentException e) {
			System.err.println("parse Xml error.");
			e.printStackTrace();
			return Collections.EMPTY_MAP;
		}

		Map<String, String> retMap = new HashMap();
		Element root = document.getRootElement();
		List elements = root.elements();

		for (int i = 0, length = elements.size(); i < length; i++) {
			Element e = (Element) elements.get(i);
			retMap.put(e.getName(), e.getTextTrim());
		}

		return retMap;
	}

	public static String mapToXml(Map<String, ?> map) {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("xml");
		Iterator iterator = map.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, Object> entry = (Map.Entry) iterator.next();
			root.addElement((String) entry.getKey()).addCDATA((String) entry.getValue());
		}
		return root.asXML().trim();
	}

	public static String beanToXml(Object target) {
		if (target == null) {
			return "<xml></xml>";
		} else if (target instanceof Map) {
			return mapToXml((Map<String, Object>) target);
		}
		try {
			Document document = DocumentHelper.createDocument();
			Element root = document.addElement("xml");
			Field[] fields = target.getClass().getDeclaredFields();
			for (Field field : fields) {
				if (!field.isAccessible()) {
					field.setAccessible(true);
				}
				Object val = field.get(target);
				root.addElement(field.getName()).addCDATA(String.valueOf(val == null ? "" : val));
			}
			return document.asXML();
		} catch (Exception e) {
			System.err.println("format Xml error.");
			e.printStackTrace();
		}
		return null;
	}
}
