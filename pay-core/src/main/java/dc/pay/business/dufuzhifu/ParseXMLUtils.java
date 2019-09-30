package dc.pay.business.dufuzhifu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class ParseXMLUtils {

	 /**
     * xml  To  map
     * @throws Exception
     */
    public static Map<String, Object> xmlToMap(String responseXmlTemp) throws DocumentException {
        Document doc = DocumentHelper.parseText(responseXmlTemp);
        Element rootElement = doc.getRootElement();
        Map<String, Object> mapXml = new HashMap<>();
        elementToMap(mapXml, rootElement);
        return mapXml;
    }
 
    /**
     * 使用递归调用将多层级xml转为map
     *
     * @param map
     * @param rootElement
     */
    private static void elementToMap(Map<String, Object> map, Element rootElement) {
        // 获得当前节点的子节点
        List<Element> childElements = rootElement.elements();
        if (childElements.size() > 0) {
            Map<String, Object> tempMap = new HashMap<>();
            for (Element e : childElements) {
                elementToMap(tempMap, e);
                map.put(rootElement.getName(), tempMap);
            }
        } else {
            map.put(rootElement.getName(), rootElement.getText());
        }
    }
}
