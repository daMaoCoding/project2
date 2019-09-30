package dc.pay.payrest;

import com.alibaba.fastjson.JSON;
import dc.pay.utils.ipUtil.qqwry.qqwry3.IpHelperCZ;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GeneralHandler {
    private static final Logger log =  LoggerFactory.getLogger(GeneralHandler.class);
   @ExceptionHandler
   public ModelAndView handleException (Exception ex, HttpServletRequest request,ContentCachingRequestWrapper requestWrapper) throws Exception {
        ModelAndView mav = new ModelAndView();
       String dateTime = DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss");

       //  ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper((HttpServletRequest) request);
       log.info("=============================="+dateTime+"====================================");
       log.info("[ERROR:第三方返回RequestURL：]，{}",requestWrapper.getRequestURL());
       log.info("[ERROR:第三方返回RequestURI：]，{}",requestWrapper.getRequestURI());
       log.info("[ERROR:第三方返回Header：]，{}",JSON.toJSONString(getHeadersInfo(request)));
       log.info("[ERROR:第三方返回UserAgent：]，{}",JSON.toJSONString(getUserAgent(request)));
       log.error("[ERROR:第三方返回请求IP：，{}，位置：{}]",getIpAdrress(request),  IpHelperCZ.findStrAddress(getIpAdrress(request)));
       log.info("[ERROR:第三方返回Body：]，{}",IOUtils.toString(requestWrapper.getBody(),request.getCharacterEncoding()));
       log.info(ex.getMessage());
       log.info("=============================="+dateTime+"====================================");
        return mav;
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


    private String   getRequestBody(HttpServletRequest request) throws Exception {
        ServletInputStream mServletInputStream = request.getInputStream();
        byte[] httpInData = new byte[request.getContentLength()];
        int retVal = -1;
        StringBuilder stringBuilder = new StringBuilder();

        while ((retVal = mServletInputStream.read(httpInData)) != -1) {
            for (int i = 0; i < retVal; i++) {
                stringBuilder.append(Character.toString((char) httpInData[i]));
            }
        }
        return stringBuilder.toString();
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



}