package dc.pay.config;/**
 * Created by admin on 2017/6/21.
 */

import com.google.common.collect.Maps;
import dc.pay.utils.ChannelParamConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ************************
 * @author tony 3556239829
 */

@Component
public class ChannelParamConfig {
    private static final Logger log =  LoggerFactory.getLogger(ChannelParamConfig.class);
    private static final String sp = "_BANK";
    private static Map<String,String> channelBankName          = Collections.synchronizedMap(Maps.newHashMap());
    private static Map<String,String> channelBankSignParamName = Collections.synchronizedMap(Maps.newHashMap());
    private static Map<String,String> channelBankRequestURL    = Collections.synchronizedMap(Maps.newHashMap());
    private static Map<String,String> channelBankDaifuQuerSignParamName    = Collections.synchronizedMap(Maps.newHashMap());
    private static Map<String,String> channelBankDaifuQuerRequestURL    = Collections.synchronizedMap(Maps.newHashMap());

    static {
        try {
            Properties props = ChannelParamConfigUtil.getChannelParamConfig();
            Map<String,String> param = new HashMap<String,String>((Map) props);
            for (Map.Entry<String, String> entry : param.entrySet()) {
                String key = entry.getKey().trim();
                String value = entry.getValue();
                String[] valueSplit = value.split("\\||\\#");
                if(null!=valueSplit && valueSplit.length==4){
                    channelBankName.put(key,valueSplit[0].trim());
                    channelBankRequestURL.put(key,valueSplit[1].trim());
                    channelBankSignParamName.put(key,valueSplit[2].trim());
                   if(key.contains(sp))   channelBankDaifuQuerSignParamName.put(key.substring(0,key.indexOf(sp, 0)),valueSplit[2].trim());
                    if(key.contains(sp))  channelBankDaifuQuerRequestURL.put(key.substring(0,key.indexOf(sp, 0)),valueSplit[1].trim());
                }else{
                   log.error("通道参数配置格式不正确，正确格式为：通道名称 = 参数|提交地址|签名名称 #备注信息。  key："+key+" ,value:"+value);
                   System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("读取[通道参数配置]成功，共："+channelBankName.size()+"个");
    }
    public  String getChannelBankName(String channelName){
        return channelBankName.get(channelName);
    }


    public  String getChannelBankSignParamName(String channelName){
        if(channelName.contains(sp))  return channelBankSignParamName.get(channelName.trim());
        return   channelBankDaifuQuerSignParamName.get(channelName.trim());
    }


    public  String getChannelBankRequestURL(String channelName){
        if(channelName.contains(sp))   return channelBankRequestURL.get(channelName.trim());
        return channelBankDaifuQuerRequestURL.get(channelName.trim());
    }




}
