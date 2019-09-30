package dc.pay.business.dangdangfu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Oct 27, 2018
 */
@RequestPayHandler("DANGDANGFU")
public final class DangDangFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DangDangFuPayRequestHandler.class);

    //字段名           字段说明          最大长度        是否必填        备注
    //merchno           商户号            15                是           商户签约时，本系统分配给商家的唯一标识。
    //amount            交易金额          12                是           以元为单位
    //traceno           商户流水号        32                是           商户网站唯一订单号，由商户系统生成，保证其唯一性。
    //payType           支付方式          1                 是           1-支付宝 2-微信 4-百度  8-QQ钱包  16-京东 32-银联钱包 
    //goodsName         商品名称          30                否           默认取商户名称
    //notifyUrl         通知地址          50                否           交易成功，则给商户发送异步通知。
    //cust1             自定义域1         100               否           商户网站自定义，系统原样返回给商户网站。
    //cust2             自定义域2         100               否           商户网站自定义，系统原样返回给商户网站。
    //cust3             自定义域3         100               否           商户网站自定义，系统原样返回给商户网站。
    //settleType        结算方式          1                 是           默认为T+1    1-T+1结算
    //signature         数据签名          32                是           对签名数据进行MD5加密的结果。
    private static final String merchno                         ="merchno";
    private static final String amount                          ="amount";
    private static final String traceno                         ="traceno";
    private static final String payType                         ="payType";
    private static final String goodsName                       ="goodsName";
    private static final String notifyUrl                       ="notifyUrl";
//    private static final String cust1                           ="cust1";
//    private static final String cust2                           ="cust2";
//    private static final String cust3                           ="cust3";
    private static final String settleType                      ="settleType";

    //2.4.WAP接口
    //ip                      客户端IP                15                     否              微信WAP支付必填,且必须上送终端用户手机上的公网IP
    private static final String ip                             ="ip";
    
    //网银
    //字段名          字段说明        最大长度        是否必填        备注
    //channel          连接方式         1                 是          2-直联银行
    //bankCode         银行代码         4                 否          当连接方式选择2的时候，该域必填。参见3.2银行代码。
    private static final String channel                       ="channel";
    private static final String bankCode                      ="bankCode";
    
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchno, channelWrapper.getAPI_MEMBERID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(traceno,channelWrapper.getAPI_ORDER_ID());
                put(goodsName,"name");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//                put(cust1,channelWrapper.getAPI_MEMBERID());
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("DANGDANGFU_BANK_WAP_WX_SM")) {
                    put(ip,channelWrapper.getAPI_Client_IP());
                    put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(settleType,"1");
                }else if (handlerUtil.isWY(channelWrapper)) {
                    put(channel,"2");
                    put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(settleType,"2");
                }else {
                    put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(settleType,"1");
                }
             }
        };
        log.debug("[当当付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[当当付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"GBK");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[当当付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        try {
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "GBK");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            log.error("[当当付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[当当付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[当当付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("respCode") && "00".equalsIgnoreCase(resJson.getString("respCode"))  && resJson.containsKey("barCode") && StringUtils.isNotBlank(resJson.getString("barCode"))) {
            String code_url = resJson.getString("barCode");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else{
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[当当付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[当当付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[当当付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    public static void main(String[] args) {
        String url = "http://66p.nsqmz6812.com:30000/respPayWeb/XXXX_BANK_WEBWAPAPP_WX_SM/";
        
//        try {
//            String sendPost = sendPost(url,"你好");
//            System.out.println("解码前sendPost===》"+sendPost);
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        
        String or_id = DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss");
        System.out.println("======>"+or_id);
        HashMap<String, String> payParam = Maps.newHashMap();
        payParam.put("a","你好");
        payParam.put(traceno,or_id);
      
        HttpHeaders headers = new HttpHeaders();
        
        
//      headers.add("Authorization","Bearer ".concat(channelWrapper.getAPI_KEY().split("&")[1]));
//        headers.add("Accept-Charset","UTF-8");
//      headers.add("Accept-Charset","ISO-8859-1");
//        headers.add("Accept-Charset","GBK");
//        headers.add("Accept-Charset","gb2312");
//        headers.add("Content-Type","text/html;charset=utf-8");
        
//        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
//        MediaType type = MediaType.parseMediaType("application/json; charset=GBK");
        MediaType type = MediaType.parseMediaType("application/x-www-form-urlencoded; charset=GBK");
        headers.setContentType(type);
        
//        headers.add("Content-Type","application/x-www-form-urlencoded;Charset=GBK");
//        headers.add("Content-Type","application/x-www-form-urlencoded; charset=GBK");
//        headers.add("Content-Type","application/x-www-form-urlencoded;Charset=gb2312");
//        headers.add("Content-Type","application/x-www-form-urlencoded;Charset=UTF-8");
//        headers.add("Accept-Encoding","gzip, deflate;Charset=UTF-8");
//      headers.add("Content-Encoding","application/x-www-form-urlencoded;Charset=UTF-8");
//      headers.add("accept", "*/*");
//      headers.add("connection", "Keep-Alive");
      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(url, payParam, String.class, HttpMethod.POST,headers);
//                         RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,headers);
      System.out.println("解码前===》"+resultStr);
      
////  System.out.println("解码前===》"+new String(result1.getBody().getBytes("GBK")));
////  System.out.println("解码前===》"+new String("������������Ƽ����޹�˾".getBytes("utf-8")));
//  
////  byte[] bytes = result1.getBody().getBytes("GBK");
////  System.out.println("解码前===》"+new String(result1.getBody().getBytes("utf-16be")));
//  
        try {
//            System.out.println("解码前gb2312===》"+new String(resultStr.getBytes("gb2312")));
//            System.out.println("解码前UTF-16BE===》"+new String(resultStr.getBytes("UTF-16BE")));
//            System.out.println("解码前utf-8===》"+new String(resultStr.getBytes("utf-8")));
//            System.out.println("解码前GBK===》"+new String(resultStr.getBytes("GBK")));
////            new String(resultStr,"GBK");
////            System.out.println("解码前GBK===》"+new String(resultStr,"GBK"));
//            System.out.println("解码前ISO-8859-1===》"+new String(resultStr.getBytes("ISO-8859-1")));
//            System.out.println("解码前()GBK===》"+new String(resultStr.getBytes(),"GBK"));
//            System.out.println("解码前()utf-8===》"+new String(resultStr.getBytes(),"utf-8"));
//            System.out.println("解码前GBK()utf-8===》"+new String(resultStr.getBytes("GBK"),"utf-8"));
            System.out.println("解码前utf-8()GBK===》"+new String(resultStr.getBytes("utf-8"),"GBK"));
//            System.out.println("解码前utf-8()utf-8===》"+new String(resultStr.getBytes("utf-8"),"utf-8"));
//            System.out.println("解码前GBK()GBK===》"+new String(resultStr.getBytes("GBK"),"GBK"));
//            System.out.println("解码前ISO-8859-1()GBK===》"+new String(resultStr.getBytes("ISO-8859-1"),"GBK"));
//            System.out.println("解码前ISO-8859-1()utf-8===》"+new String(resultStr.getBytes("ISO-8859-1"),"utf-8"));
//            System.out.println("解码前GBK()ISO-8859-1===》"+new String(resultStr.getBytes("GBK"),"ISO-8859-1"));
//            System.out.println("解码前ISO-8859-1()ISO-8859-1===》"+new String(resultStr.getBytes("ISO-8859-1"),"ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
      
    }
    
    public static String sendPost(String url, String param) throws Exception{
         
         BufferedReader in = null;
         String result = "";
        // try {
          URL realUrl = new URL(url);
          // 打开和URL之间的连接
          URLConnection conn = realUrl.openConnection();
          // 设置通用的请求属性
          conn.setRequestProperty("accept", "*/*");
          conn.setRequestProperty("connection", "Keep-Alive");
          conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
          // 发送POST请求必须设置如下两行
          conn.setDoOutput(true);
          conn.setDoInput(true);
          
//          headers.add("Content-Type","application/x-www-form-urlencoded;Charset=iso-8859-1");
          conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded;Charset=GBK");
          
          conn.setConnectTimeout(30000);
          conn.setReadTimeout(30000);
          // 获取URLConnection对象对应的输出流
          OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream()); 
          // 发送请求参数
          
          out.write(param);
          // flush输出流的缓冲
          out.flush();
          // 定义BufferedReader输入流来读取URL的响应
          in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
          String line;
          while ((line = in.readLine()) != null) {
          result += line;
          }
           
          if (out != null) {
          out.close();
          }
          if (in != null) {
          in.close();
          }
          
         return result;
         }
        
        

    public static char getRandomChar(){

        String str ="";

        int hightpos; 

        int lowpos;

        Random random = new Random();

        hightpos = (176+Math.abs((random.nextInt(39))));

        lowpos =(161+ Math.abs((random.nextInt(93))));

        byte b[] =new byte[2];

        b[0] = (Integer.valueOf(hightpos)).byteValue();

        b[1] = (Integer.valueOf(lowpos)).byteValue();

        try {

        str =new String(b,"GBK");

        } catch (UnsupportedEncodingException e) {

        // TODO Auto-generated catch block

        e.printStackTrace();

        }

        System.out.println("str===========》"+str);
        return str.charAt(0);

        }


}