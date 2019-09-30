package dc.pay.business.jinshunzhifu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

/**
 * @author cobby
 * Jan 15, 2019
 */
public class JinShunZhiFuXmlUtil {
	private static DocumentBuilderFactory factory = null;
	private static DocumentBuilder builder = null;
	private Document document;
	static {
		try {
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 订单请求转换成xml
	 * @param request 请求
	 * @return 请求的xml报文
	 */
	public String orderRequestToXml(Map<String,String> request) {

		document = builder.newDocument();
		Element root = document.createElement("message");
		root.setAttribute("application", request.get("application"));
		root.setAttribute("version", request.get("version"));
		root.setAttribute("timestamp", request.get("timestamp"));
		root.setAttribute("merchantId", request.get("merchantId"));
		root.setAttribute("merchantOrderId", request.get("merchantOrderId"));
		root.setAttribute("merchantOrderAmt", request.get("merchantOrderAmt"));
		if(request.get("merchantOrderDesc") != null && request.get("merchantOrderDesc").length() > 0){
			root.setAttribute("merchantOrderDesc", request.get("merchantOrderDesc"));
		}
		root.setAttribute("userName", request.get("userName"));
		if(request.get("merchantPayNotifyUrl") != null && request.get("merchantPayNotifyUrl").trim().length() > 0){
			root.setAttribute("merchantPayNotifyUrl", request.get("merchantPayNotifyUrl").trim());
		}
		if(request.get("accountType") != null && request.get("accountType").trim().length() > 0){
			root.setAttribute("accountType", request.get("accountType").trim());
		}
		if(request.get("orderTime") != null && request.get("orderTime").trim().length() > 0){
			root.setAttribute("orderTime", request.get("orderTime").trim());
		}
		if(request.get("rptType") != null && request.get("rptType").trim().length() > 0){
			root.setAttribute("rptType", request.get("rptType").trim());
		}
		if(request.get("payMode") != null && request.get("payMode").trim().length() > 0){
			root.setAttribute("payMode", request.get("payMode").trim());
		}
		document.appendChild(root);
		request.put("xml",createXml());
		return request.get("xml");
	}

	/**
	 * xml转换成支付通知响应
	 * @param xmlStr 响应报文
	 * @return 响应对象
	 */
	public Map<String ,String> xmlToPaymentNotifyResponse(String xmlStr) {
		Map<String ,String> response= new HashMap();
 		ByteArrayInputStream bais = null;
		DataInputStream dis = null;
		try {
			bais = new ByteArrayInputStream(xmlStr.getBytes("utf-8"));
			dis = new DataInputStream(bais);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.newDocument();
			document = builder.parse(dis);
			document.getDocumentElement().normalize();
			NamedNodeMap nnm = document.getFirstChild().getAttributes();
			//<message merchantId="19" merchantOrderId="20190177277" codeUrl="745570248100" respCode="000" respDesc="成功"/>
			for(int j = 0; j<nnm.getLength(); j++){
				Node nodeitm= nnm.item(j);
				String value = nodeitm.getNodeValue();
				if (nodeitm.getNodeName().equals("merchantId")) {
					response.put("merchantId",value);
				} else if (nodeitm.getNodeName().equals("merchantOrderId")) {
					response.put("merchantOrderId",value);
				} else if (nodeitm.getNodeName().equals("codeUrl")) {
					response.put("codeUrl",value);
				} else if (nodeitm.getNodeName().equals("respCode")) {
					response.put("respCode",value);
				} else if (nodeitm.getNodeName().equals("respDesc")) {
					response.put("respDesc",value);
				}
			}
			return response;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(dis != null)try {dis.close();} catch (Exception e2) {}
			if(bais != null)try {bais.close();} catch (Exception e2) {}
		}
		return null;
	}


	/**
	 * xml转换成支付通知响应
	 * @param xmlStr 响应报文
	 * @return 响应对象
	 */
	protected Map<String ,String> xmlToPaymentNotifyResponse1(String xmlStr) {
		ByteArrayInputStream bais = null;
		DataInputStream dis = null;
		try {
			bais = new ByteArrayInputStream(xmlStr.getBytes("utf-8"));
			dis = new DataInputStream(bais);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.newDocument();
			document = builder.parse(dis);
			document.getDocumentElement().normalize();
			NamedNodeMap nnm = document.getFirstChild().getAttributes();
			Map<String ,String> response = new HashMap<>();
			response.put("",xmlStr);
			for(int j = 0; j<nnm.getLength(); j++){
				Node nodeitm= nnm.item(j);
				String value = nodeitm.getNodeValue();
				if (nodeitm.getNodeName().equals("merchantId")) {
					response.put("merchantId",value);
				} else if (nodeitm.getNodeName().equals("merchantOrderId")) {
					response.put("merchantOrderId",value);
				} else if (nodeitm.getNodeName().equals("version")) {
					response.put("version",value);
				} else if (nodeitm.getNodeName().equals("application")) {
					response.put("application",value);
				}
			}
			NodeList messageChildList = document.getFirstChild().getChildNodes();
			for(int i = 0; i<messageChildList.getLength(); i++){
				Node node = messageChildList.item(i);
				NodeList nodeCList = node.getChildNodes();
				if(node.getNodeName().equals("deductList")){
					for(int j = 0; j<nodeCList.getLength();j++){
						nnm = nodeCList.item(j).getAttributes();
						if(nnm == null)continue;
						for(int k = 0; k<nnm.getLength(); k++){
							Node nodeitm= nnm.item(k);
							String value = nodeitm.getNodeValue();
							if (nodeitm.getNodeName().equals("payOrderId")) response.put("payOrderId",value);
							else if (nodeitm.getNodeName().equals("payAmt")) response.put("payAmt",value);
							else if (nodeitm.getNodeName().equals("payTime")) response.put("payTime",value);
							else if (nodeitm.getNodeName().equals("payStatus")) response.put("payStatus",value);
							else if (nodeitm.getNodeName().equals("payDesc")) response.put("payDesc",value);
						}
					}
				}
			}
			return response;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(dis != null)try {dis.close();} catch (Exception e2) {}
			if(bais != null)try {bais.close();} catch (Exception e2) {}
		}
		return null;
	}

	/**
	 * 生成sml
	 * @return
	 */
	private String createXml() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			DOMSource source = new DOMSource(document);
			transformer.setOutputProperty(OutputKeys.ENCODING,"utf-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			bos = new ByteArrayOutputStream();
			dos = new DataOutputStream(bos);
			StreamResult result = new StreamResult(dos);
			transformer.transform(source, result);
			return new String(bos.toByteArray(),"utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(dos != null){
				try {
					dos.close();
				} catch (Exception e2) {}
			}
			if(bos != null){
				try {
					bos.close();
				} catch (Exception e2) {}
			}
		}
		return "";
	}

	/**
	 * 通过r分解src
	 * @param src
	 * @return
	 */
	public  String [] split(String src,String r){
		if(src == null || r == null || src.length() == 0
				|| r.length() == 0 || src.indexOf(r) == -1){
			String [] strs = {src};
			return strs;
		}
		List list = new ArrayList();
		int site = src.indexOf(r);
		int rLen = r.length();
		while(site != -1){
			list.add(src.substring(0, site));
			src = src.substring(site + rLen);
			site = src.indexOf(r);
		}
		list.add(src);
		String [] strs = new String [list.size()];
		for(int i = 0; i<list.size(); i++){
			strs[i] = (String)list.get(i);
		}
		return strs;
	}
}
