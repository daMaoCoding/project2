package dc.pay.utils;

import dc.pay.constant.SERVER_MSG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ************************
 * Properties工具类
 * @author tony 3556239829
 */

//@Configuration
//@ConfigurationProperties()
//@Component
//@PropertySource("classpath:handler.properties")
public class PropertiesUtil {

    private static Properties props;
    private static final Logger log =  LoggerFactory.getLogger(PropertiesUtil.class);

    static {
        props = new Properties();
        //InputStream in = ClassLoader.getSystemResourceAsStream("handler.properties");
        InputStream in = PropertiesUtil.class.getResourceAsStream("/handler.properties");

        try {
            props.load(in);
        } catch (IOException e) {
            log.error("[错误：]"+ SERVER_MSG.PROPERROR+","+e.getMessage(),e);
        }
    }


    /**  
     * 获取某个属性  
     */    
    public static String getProperty(String key){
        return props.getProperty(key);
    }



    /**  
     * 获取所有属性，返回一个map,不常用  
     * 可以试试props.putAll(t)  
     */    
    public static  Map getAllProperty(){
        Map map=new HashMap();    
        Enumeration enu = props.propertyNames();    
        while (enu.hasMoreElements()) {    
            String key = (String) enu.nextElement();    
            String value = props.getProperty(key);    
            map.put(key, value);    
        }    
        return map;    
    }    


    /**
     * 在控制台上打印出所有属性，调试时用。  
     */    
    public void printProperties(){    
        props.list(System.out);    
    }    


}    