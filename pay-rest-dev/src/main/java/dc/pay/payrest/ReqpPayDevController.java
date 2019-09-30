package dc.pay.payrest;
/**
 * Created by admin on 2017/5/29.
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.utils.ipUtil.qqwry.qqwry3.IpHelperCZ;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;


import org.dom4j.Element;
import org.xml.sax.InputSource;

/**
 * ************************
 * 接收&响应 第三方支付平台 web 接口
 * @author tony 3556239829
 */

@RestController
@RequestMapping(path = {"/respPayWeb","/respDaifuWeb"})
public class ReqpPayDevController {
    private static final Logger log =  LoggerFactory.getLogger(ReqpPayDevController.class);

    /**
     * 接受第三方支付通知 & 转发
     * @param channelName
     * @param mapBodys
     * @return
     */
    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET},consumes={"application/json"})
    public void  receiveJSON(@PathVariable(value = "channelName",required = true) String channelName, @RequestBody(required = false) Map<String,String> mapBodys, HttpServletResponse response,HttpServletRequest request) {
        if(!PayRestDevConFig.getInstance().isBlackIp(getIpAdrress(request))){
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log.error("请求Header:，{}",JSON.toJSONString(getHeadersInfo(request)));
            log.error("请求UserAgent：，{}",JSON.toJSONString(getUserAgent(request)));
            log.error("请求IP：，{}，位置：{}",getIpAdrress(request),  IpHelperCZ.findStrAddress(getIpAdrress(request)));
            log.error("通道名称："+channelName+" ,application/json");
            log.error("响应内容："+JSON.toJSONString(mapBodys));
            writeResponse(response,JSON.toJSONString(mapBodys));
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }
    }


    /**
     * 接受第三方支付通知 & 转发
     * @param channelName
     * @param mapParams
     * @return
     */
    
    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET},consumes={"application/x-www-form-urlencoded"} )
    public void receiveForm(@PathVariable(value = "channelName",required = true) String channelName, @RequestParam(required = false) Map<String,String> mapParams, HttpServletResponse response,HttpServletRequest request) {
        if(!PayRestDevConFig.getInstance().isBlackIp(getIpAdrress(request))) {
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log.error("请求Header:，{}", JSON.toJSONString(getHeadersInfo(request)));
            log.error("请求UserAgent：，{}", JSON.toJSONString(getUserAgent(request)));
            log.error("请求IP：，{}，位置：{}", getIpAdrress(request), IpHelperCZ.findStrAddress(getIpAdrress(request)));
            log.error("通道名称：" + channelName + " ,application/x-www-form-urlencoded");
            log.error("响应内容：" + JSON.toJSONString(mapParams));
            writeResponse(response, JSON.toJSONString(mapParams));
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }
    }


    /**
     * 接受第三方支付通知 & 转发
     * @param channelName
     * @return
     */
    
    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET},consumes={"text/xml","application/xml"})
    public void  receiveXML(@PathVariable(value = "channelName",required = true) String channelName, HttpServletRequest request,HttpServletResponse response) throws Exception {
        if(!PayRestDevConFig.getInstance().isBlackIp(getIpAdrress(request))) {
            String requestInputStreamString = getRequestInputStreamString(request, null);
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log.error("请求Header:，{}", JSON.toJSONString(getHeadersInfo(request)));
            log.error("请求UserAgent：，{}", JSON.toJSONString(getUserAgent(request)));
            log.error("请求IP：，{}，位置：{}", getIpAdrress(request), IpHelperCZ.findStrAddress(getIpAdrress(request)));
            log.error("通道名称：" + channelName + " ,text/xml，application/xml");
            log.error("原始xml响应内容："+   requestInputStreamString);  //1.重复读，2，避免开发人员用错第三方返回内容
            log.error("响应内容：" + JSON.toJSONString(toMap(requestInputStreamString.getBytes(), "utf-8")));
            writeResponse(response, JSON.toJSONString(toMap(requestInputStreamString.getBytes(), "utf-8")));
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }
    }


    /**
     * 接受第三方支付通知 & 转发
     * @param channelName
     * @param request
     * @return
     */
    
    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET} )
    public void receive(@PathVariable(value = "channelName",required = true) String channelName, ContentCachingRequestWrapper request,HttpServletResponse response) {
        if(!PayRestDevConFig.getInstance().isBlackIp(getIpAdrress(request))) {
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log.error("请求Header:，{}", JSON.toJSONString(getHeadersInfo(request)));
            log.error("请求UserAgent：，{}", JSON.toJSONString(getUserAgent(request)));
            log.error("请求IP：，{}，位置：{}", getIpAdrress(request), IpHelperCZ.findStrAddress(getIpAdrress(request)));
            log.error("通道名称：" + channelName + " ,other");
            log.error("响应内容：" + JSON.toJSONString(new String(request.getBody()))); //1.重复读，2，避免开发人员用错第三方返回内容
            Map<String, String> mapParams = processRequestParam(request);
            if (null == mapParams || mapParams.isEmpty()) {
                mapParams = getMapUseJsonStr(request);
            }
            log.error("响应内容In do req:" + JSON.toJSONString(mapParams));
            writeResponse(response, JSON.toJSONString(mapParams));
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }
    }



    /**********************************************************************************************************************/

    /**
     * 处理request参数
     * @param request
     * @return
     */
    public static Map<String, String> processRequestParam(HttpServletRequest request) {
        Enumeration enu=request.getParameterNames();
        Map<String,String> paramMap = new TreeMap<String,String>();
        while(enu.hasMoreElements()){
            String paraName=(String)enu.nextElement();
            paramMap.put(paraName,request.getParameter(paraName));
        }
        if(null!=paramMap && !paramMap.isEmpty())
            return paramMap;
        return null;
    }


    /**
     * JSON转Map
     * @param str  {"responsePayCode":"SUCCESS","responseOrderID":"456_1496025545611_1496108539616","responseOrderState":"SUCCESS","responsePayErrorMsg":null,"responsePayTotalTime":68,"responsePayMsg":"000000"}
     * @return
     */
    public static  Map<String,String>  jsonToMap(String str) {
        String sb = str.substring(2, str.length() - 2);
        String[] name = sb.split("\\\",\\\"");
        String[] nn = null;
        Map<String, String> map = new HashMap<>();
        for (String aName : name) {
            nn = aName.split("\\\":\\\"");
            map.put(nn[0], nn.length==2?nn[1]:null);
        }
        return map;
    }

    /**********************************************************************************************************************/


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


    public static Map<String, String> toMap(Element element){
        Map<String, String> rest = new HashMap<String, String>();
        List<Element> els = element.elements();
        for(Element el : els){
            rest.put(el.getName(), el.getTextTrim());  //
        }
        return rest;
    }




    /**********************************************************************************************************************/


    /**
     * 转发回复内容至第三方支付(无论验证是否成功，总返回第三方支付验证成功,目的：使第三方不再继续发送通知，具体错误由人工处理)
     * @param response
     */
    public  void writeResponse(HttpServletResponse response,String responseDataMsg){
         response.setContentType("text/html; charset=utf-8");
        PrintWriter out = null;
        String responseMsg="回调收到！ 这是开发环境,接收回调数据用于开发。(开发结束后,生成环境会正常返回success|ok|等). The Time: ".concat(formatDate(new Date(), "yyyy-MM-dd HH:mm:ss.SSS"));
        try {
            out = response.getWriter();
            if(null!=responseMsg && !"".equalsIgnoreCase(responseMsg)){
                out.println(responseMsg);
                out.println(" | 收到回调数据："+responseDataMsg);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null!=out){
                out.close();
            }
        }
    }


    public static String formatDate(Date date, String pattern) {
        String formatDate = null;
        if (StringUtils.isNotBlank(pattern)) {
            formatDate = DateFormatUtils.format(date, pattern);
        } else {
            formatDate = DateFormatUtils.format(date, "yyyy-MM-dd");
        }
        return formatDate;
    }




    private  Map<String, String> getHeadersInfo(HttpServletRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        return map;
    }


    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("user-agent");
    }



    public static String getIpAdrress(HttpServletRequest request) {
        String Xip = request.getHeader("X-Real-IP");
        String XFor = request.getHeader("X-Forwarded-For");
        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = XFor.indexOf(",");
            if(index != -1){
                return XFor.substring(0,index);
            }else{
                return XFor;
            }
        }
        XFor = Xip;
        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
            return XFor;
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.getRemoteAddr();
        }
        // XFor.concat(":").concat(request.getRemotePort()+"");
        return XFor;
    }

    public static Map<String, String> toMap(byte[] xmlBytes, String charset) throws Exception{
        SAXReader reader = new SAXReader(false);
        InputSource source = new InputSource(new ByteArrayInputStream(xmlBytes));
        source.setEncoding(charset);
        Document doc = reader.read(source);
        Map<String, String> params = toMap(doc.getRootElement());
        return params;
    }

    public static String  getRequestInputStreamString(HttpServletRequest request, String charset){
        StringBuilder sb = new StringBuilder();
        BufferedReader br;
        try {
            if(StringUtils.isNotBlank(charset)){
                br = new BufferedReader(new InputStreamReader((ServletInputStream) request.getInputStream(),charset));
            }else{
                br = new BufferedReader(new InputStreamReader((ServletInputStream) request.getInputStream()));
            }
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return  sb.toString();
        } catch (IOException e) {
            return null;
        }
    }


    //目前只能处理,request.getInputStream内容是  {"A":"B"},不能处理request.getInputStream内容是  data= {"A":"B"}
    private Map<String, String> getMapUseJsonStr(HttpServletRequest request) {
        Map<String, String> mapParams= new HashMap<>();
        String requstStr = parseRequst(request);
        if(StringUtils.isNotBlank(requstStr) && requstStr.contains("{") && requstStr.contains("}")){
            mapParams =  JSONObject.toJavaObject(JSON.parseObject(requstStr), Map.class);
        }else{
            mapParams.put("pay-core-respData",requstStr);
        }
        return mapParams;
    }


}
