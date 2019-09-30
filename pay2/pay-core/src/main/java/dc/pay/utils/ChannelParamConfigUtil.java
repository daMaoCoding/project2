package dc.pay.utils;

import com.google.common.collect.Lists;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public   class ChannelParamConfigUtil {

    public  static   List<Resource>  channel_Param_Config_Resource_json_and_excel;
    public  static   List<Resource>  channel_Param_Config_All ;

    static {
        channel_Param_Config_All = Lists.newArrayList();
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-tree.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-kevin.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-beck.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-andrew.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-leo.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-tony.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-sunny.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-cobby.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-wilson.properties"));
        channel_Param_Config_All.add(new ClassPathResource("channelConfig/channel-mikey.properties"));
        channel_Param_Config_Resource_json_and_excel= new ArrayList<Resource>(){{
            add(new ClassPathResource("channelConfig/channel-excelJson.properties"));
        }};
    }

    // Properties props = PropertiesLoaderUtils.loadProperties(channelParamConfigResource);
    //File channelFile =new File(channelResource.getURI().getPath());
    //File channelFile =channelResource.getFile();


    public static List<Resource> getChannelParamConfigResourceAll(){  //classpath*:conf/appContext.xml  application-context-*.xml
        return  channel_Param_Config_All;
    }


    public static List<Resource> getChannelParamConfigResourceJsonAndExcel(){
        return channel_Param_Config_Resource_json_and_excel;
    }


    public static Properties getChannelParamConfig(){
        Properties mergedProperty = new Properties();
            channel_Param_Config_All.forEach(resource -> {
                try {
                    mergedProperty.putAll(PropertiesLoaderUtils.loadProperties(resource));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        return mergedProperty;
    }
}
