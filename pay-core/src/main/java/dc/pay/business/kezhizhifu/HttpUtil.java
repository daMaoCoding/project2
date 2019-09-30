package dc.pay.business.kezhizhifu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class HttpUtil {
    //private static final Log logger = Logs.get();  
       private final static int CONNECT_TIMEOUT = 5000; // in milliseconds  
       private final static String DEFAULT_ENCODING = "UTF-8";  

       public static String postData(String urlStr, String data){  
           return postData(urlStr, data, null);  
       }  

       public static String postData(String urlStr, String data, String contentType){  
           BufferedReader reader = null;  
        //   System.out.println(data);
           try {  
               URL url = new URL(urlStr);  
               URLConnection conn = url.openConnection();  
               conn.setDoOutput(true);  
               conn.setConnectTimeout(CONNECT_TIMEOUT);  
               conn.setReadTimeout(CONNECT_TIMEOUT);  
             
               conn.setRequestProperty("content-type", "application/x-www-form-urlencoded;charset=utf8");  
               OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), DEFAULT_ENCODING);  
               if(data == null)  
                   data = "";  
               writer.write(data);   
               writer.flush();  
               writer.close();    

               reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), DEFAULT_ENCODING));  
               StringBuilder sb = new StringBuilder();  
               String line = null;  
               while ((line = reader.readLine()) != null) {  
                   sb.append(line);  
                   sb.append("\r\n");  
               }  
               return sb.toString();  
           } catch (IOException e) {  
               //logger.error("Error connecting to " + urlStr + ": " + e.getMessage());  
           } finally {  
               try {  
                   if (reader != null)  
                       reader.close();  
               } catch (IOException e) {  
               }  
           }  
           return null;  
       }  
       
}
