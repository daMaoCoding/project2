package dc.pay.utils;

import org.apache.commons.lang.StringUtils;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;

import org.dom4j.Element;

public class XmlUtil {

    public static String parseRequst(HttpServletRequest request){
        String body = "";
        try {
            ServletInputStream inputStream = request.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            while(true){
                String info = br.readLine();
                if(info == null){
                    break;
                }
                if(body == null || "".equals(body)){
                    body = info;
                }else{
                    body += info;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return body;
    }

    public static Map<String, String> toMap(byte[] xmlBytes, String charset) throws Exception{
        SAXReader reader = new SAXReader(false);
        InputSource source = new InputSource(new ByteArrayInputStream(xmlBytes));
        source.setEncoding(charset);
        Document doc = reader.read(source);
        Map<String, String> params = toMap(doc.getRootElement());
        return params;
    }

    public static Map<String, String> toMap(Element element){
        Map<String, String> rest = new HashMap<String, String>();
        List<Element> els = element.elements();
        for(Element el : els){
            rest.put(el.getName(), el.getTextTrim());
        }
        return rest;
    }


    public static void parse(String xmlData, Map<String, String> resultMap) throws Exception {
        Document doc = DocumentHelper.parseText(xmlData);
        Element root = doc.getRootElement();
        parseNode(root, resultMap);
    }

    private static void parseNode(Element node, Map<String, String> resultMap) {
        List attList = node.attributes();
        List eleList = node.elements();

        for (int i=0; i<attList.size(); i++) {
            Attribute att = (Attribute) attList.get(i);
            resultMap.put(att.getPath(), att.getText().trim());
        }

        resultMap.put(node.getPath(), node.getTextTrim());
        for (int i=0; i<eleList.size(); i++) {
            parseNode((Element) eleList.get(i), resultMap);
        }
    }



    /**
     * xml 转 Map
     */
    public static Map<String,String> xml2Map(String xml)  {
        Map<String,String> map = new HashMap<String, String>();
        if(StringUtils.isBlank(xml)) return map;
        Document doc = null;
        try {
            doc = DocumentHelper.parseText(xml);
        } catch (DocumentException e) {
           e.printStackTrace();
        }
        if(doc ==null)
            return map;
        Element root = doc.getRootElement();
        for (Iterator iterator = root.elementIterator(); iterator.hasNext();) {
            Element e = (Element) iterator.next();
            map.put(e.getName(), e.getText());
        }
        return map;
    }


    /**
     * Map 转 XML
     */
    public static String map2Xml(Map map,boolean useXmlSchema,String rootSchema,boolean useCdata) {
        StringBuffer sb = new StringBuffer();
        if(useXmlSchema){
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        }
        if(StringUtils.isNotBlank(rootSchema)) sb.append("<"+rootSchema+">");
        mapToXML(map, sb,useCdata);
        if(StringUtils.isNotBlank(rootSchema)) sb.append("</"+rootSchema+">");
        return  sb.toString();
    }


    private static void mapToXML(Map map, StringBuffer sb,boolean useCdata) {
        Set set = map.keySet();
        for (Iterator it = set.iterator(); it.hasNext();) {
            String key = (String) it.next();
            Object value = map.get(key);
            if (null == value)
                value = "";
            if (value.getClass().getName().equals("java.util.ArrayList")) {
                ArrayList list = (ArrayList) map.get(key);
                sb.append("<" + key + ">");
                for (int i = 0; i < list.size(); i++) {
                    HashMap hm = (HashMap) list.get(i);
                    mapToXML(hm, sb,useCdata);
                }
                sb.append("</" + key + ">");

            } else {
                if (value instanceof HashMap) {
                    sb.append("<" + key + ">");
                    mapToXML((HashMap) value, sb,useCdata);
                    sb.append("</" + key + ">");
                } else {
                        if(useCdata){
                            sb.append("<" + key + ">" + "<![CDATA["+value+"]]>" + "</" + key + ">");
                        }else{
                          sb.append("<" + key + ">" + value + "</" + key + ">");
                        }

                }
            }
        }
    }




}
