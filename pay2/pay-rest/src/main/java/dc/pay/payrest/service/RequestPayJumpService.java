package dc.pay.payrest.service;/**
 * Created by admin on 2017/6/25.
 */

import dc.pay.payrest.util.RestTemplateUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Component
@Qualifier("RequestPayJumpService")
public class RequestPayJumpService {



    private static final String RESPAY_SERV_PATH = "/reqPay/viewJson/";
    private static final String RESPONSE_PAY_MSG = "responsePayMsg";
    private static final String REQUEST_PAY_PATH = "/reqPay/";

    /**
     * 直链PayServIP地址,低优先级
     */
    @Value("${payProps.forweb.payServUrl:}")
    private String PAY_SERV_URL;


    /**
     * 非直链PayServIp地址，使用Eureka实例名，高优先级
     */
    @Value("${payProps.forweb.payServEurekaInstances:}")
    private String PAY_SERV_EUREKA_INSTANCES;


    @Autowired
    @LoadBalanced
    @Qualifier("payForWebLoadBalanced")
    RestTemplate payLoadBalanced;


    @Autowired
    @Qualifier("payForWebRestTemplate")
    RestTemplate payRestTemplate;


    /**
     * 转发内容至支付网关
     */
   public String getToPayServ(String orderId){
       String payServUrl = null;
       RestTemplate restTemplate = null;
       if(StringUtils.isNotBlank(PAY_SERV_EUREKA_INSTANCES) && PAY_SERV_EUREKA_INSTANCES.startsWith("http")){
           payServUrl = PAY_SERV_EUREKA_INSTANCES+RESPAY_SERV_PATH+orderId+"/";
           restTemplate=payLoadBalanced;
       }else{
           payServUrl = PAY_SERV_URL+RESPAY_SERV_PATH+orderId+"/";
           restTemplate=payRestTemplate;
       }
       return RestTemplateUtil.getToPayServ(restTemplate,payServUrl);
   }



    public String getRequestPayResult(String orderId,Map params){
        String payServUrl = null;
        RestTemplate restTemplate = null;
        if(StringUtils.isNotBlank(PAY_SERV_EUREKA_INSTANCES) && PAY_SERV_EUREKA_INSTANCES.startsWith("http")){
            payServUrl = PAY_SERV_EUREKA_INSTANCES+REQUEST_PAY_PATH+orderId;
            restTemplate=payLoadBalanced;
        }else{
            payServUrl = PAY_SERV_URL+REQUEST_PAY_PATH+orderId;
            restTemplate=payRestTemplate;
        }
        return RestTemplateUtil.postJson(restTemplate,payServUrl,params);
    }





    /**
     * 跳转
     * @param response
     * @param result
     */
    public  void writeResponse(HttpServletResponse response, String result){
         response.setContentType("text/html; charset=utf-8");
        PrintWriter out = null;

        try {
            out = response.getWriter();
            if(null!=result && !"".equalsIgnoreCase(result)){
                out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
                out.print(result);
                out.println("</html>");
                out.flush();
            }else{
                out.print("ERROR");
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



    public static String decryptOrderId(String str) {
        String sn = "helloworld"; // 密钥
        char[] snNum = new char[str.length() / 3];
        String result = "";
        for (int i = 0, j = 0; i < str.length() / 3; i++, j++) {
            if (j == sn.length())
                j = 0;
            int n = Integer.parseInt(str.substring(i * 3, i * 3 + 3));
            snNum[i] = (char) ((char) n ^ sn.charAt(j));
        }
        for (int k = 0; k < str.length() / 3; k++) {
            result += snNum[k];
        }
        return result;
    }

    /*判断字符串中是否仅包含字母数字和汉字
     *各种字符的unicode编码的范围：
     * 汉字：[0x4e00,0x9fa5]（或十进制[19968,40869]）
     * 数字：[0x30,0x39]（或十进制[48, 57]）
     *小写字母：[0x61,0x7a]（或十进制[97, 122]）
     * 大写字母：[0x41,0x5a]（或十进制[65, 90]）
     */
    public static  boolean valInput(String str){
            String regex = "^[a-z0-9A-Z\\-\\_\\&\\.\\:\\s\u4e00-\u9fa5]+$";
            return str.matches(regex);
    }


    /**
     * 元转分
     */
    public static  String getFen(String yuanStr)  {
        try{
            Double  fenDouble = Double.parseDouble(yuanStr) * 100.00;
            String  fenStr = new DecimalFormat("0").format(fenDouble.intValue());
            return fenStr;
        }catch (Exception ex){
            return "0.00";
        }
    }


    /**
     * 分转元，保留2位小数
     */
    public static String getYuan(String fenStr) {
        try{
            double  yuanDouble = Integer.parseInt(fenStr) / 100.00;
            String  yuanStr = new DecimalFormat("0.00").format(yuanDouble);
            return yuanStr;
        }catch (Exception ex){
            return "0.00";
        }
    }



}
