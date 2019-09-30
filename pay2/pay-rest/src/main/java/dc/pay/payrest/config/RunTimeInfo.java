package dc.pay.payrest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(value=1)

public class RunTimeInfo   implements CommandLineRunner {
    private static final Logger log =  LoggerFactory.getLogger(RunTimeInfo.class);
    private String  serverId;
    @Autowired
    StandardEnvironment environment;
    @Autowired
    GitProperties gitProperties;
    public static StartInfo startInfo;
    @PostConstruct
    public void init() {
        serverId = environment.getProperty("spring.cloud.client.ipAddress").concat(":").concat(environment.getProperty("server.port")).concat(":").concat(environment.getProperty("spring.profiles"));
    }

    public static String LOGLEVEL;

    @Override
    public void run(String... args) throws Exception {
        this.startInfo = new StartInfo(environment, gitProperties,getMemoryInfo(), args);
        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(  env -> (env.equalsIgnoreCase("dev") || env.equalsIgnoreCase("test") || env.equalsIgnoreCase("local")) ))
        { }
        this.LOGLEVEL = environment.getProperty("logging.pattern.level");
        RunTimeInfo.printMemoryInfo();
        log.info("-================================[Start Finish]================================-");
    }


     // 获取内存
    public static Map<String,Long> getMemoryInfo(){
        Map<String, Long> memoryInfo = new HashMap<String,Long>(){{
            put("maxMemory",Runtime.getRuntime().maxMemory()/1024/1024); //最大可用内存 -Xmx
            put("freeMemory", Runtime.getRuntime().freeMemory()/1024/1024);//最大可用内存 -Xmx
            put("totalMemory", Runtime.getRuntime().totalMemory()/1024/1024);
        }};
        return memoryInfo;
    }

    //打印内存
    public static void printMemoryInfo(){
        Map<String, Long> memoryInfo = getMemoryInfo();
        log.error("=========================================================");
        log.error(  "最大可用内存: "+memoryInfo.get("maxMemory")+" ,可用内存:"+memoryInfo.get("freeMemory") +" ,totalMemory:"+memoryInfo.get("totalMemory"));
        log.error(  "日志级别："+LOGLEVEL);
        log.error("=========================================================");
    }



    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }



    public   StartInfo getStartInfo() {
        return startInfo;
    }

    public   void setStartInfo(StartInfo startInfo) {
        RunTimeInfo.startInfo = startInfo;
    }


}
