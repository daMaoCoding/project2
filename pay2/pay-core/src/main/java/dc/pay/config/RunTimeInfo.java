package dc.pay.config;

import com.google.common.collect.Maps;
import dc.pay.entity.runtime.StartInfo;
import dc.pay.service.runtime.StartInfoService;
import dc.pay.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(value=1)

public class RunTimeInfo   implements CommandLineRunner {
    private static final Logger log =  LoggerFactory.getLogger(RunTimeInfo.class);
    private final static String LoggerTag="logging.level.";
    private static final String ipKey="spring.cloud.client.ipAddress";
    private static final String portKey="server.port";
    private static final String profilesKey="spring.profiles";
    private static final String serverIdFormat="%s:%s:%s";
    private static final String serverUrlFormat="http://%s:%s";
    @Autowired DataSourceProperties properties;
    @Autowired StandardEnvironment environment;
    @Autowired GitProperties gitProperties;
    @Autowired StartInfoService startInfoService;
    public static String  serverId;
    public static String ip;
    public static String port;
    public static String profiles;
    public static StartInfo startInfo;
    public static String LOGLEVEL;
    public static String serverUrl;

    @PostConstruct
    public void init() {
        ip = environment.getProperty(ipKey);
        port = environment.getProperty(portKey);
        profiles = environment.getProperty(profilesKey);
        serverId = String.format(serverIdFormat,ip,port,profiles);
        serverUrl = String.format(serverUrlFormat,ip,port);
    }


    @Override
    public void run(String... args) throws Exception {
        try{
            this.startInfo = new StartInfo(environment, gitProperties,getMemoryInfo(), getDbInfo(), args);
            startInfoService.save(startInfo);
            if(Arrays.stream(environment.getActiveProfiles()).anyMatch(env -> (env.equalsIgnoreCase("dev") || env.equalsIgnoreCase("test") || env.equalsIgnoreCase("local")) ))
            {
            }
            this.LOGLEVEL = environment.getProperty("logging.pattern.level");

        }catch (Exception e){}//保存启动信息，重要gitCommitID
        RunTimeInfo.printMemoryInfo();
        DateUtil.printNow();
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




     // 获取本机的IP
    public static String getLocalHostIP() {
        String ip;
        try {
           //返回本地主机
            InetAddress addr = InetAddress.getLocalHost();
            /**返回 IP 地址字符串（以文本表现形式）*/
            ip = addr.getHostAddress();
        } catch(Exception ex) {
            ip = "";
        }
        return ip;
    }


    // 主机名：
    public static String getLocalHostName() {
        String hostName;
        try {
            InetAddress  addr = InetAddress.getLocalHost();
            hostName =addr.toString(); // addr.getHostName();
        }catch(Exception ex){
           hostName = "ERROR InetAddress";
        }
        return  hostName;
    }


    //数据库配置信息
    public HashMap<String, String> getDbInfo() {
        HashMap<String, String> maps = Maps.newHashMap();
        maps.put("url",properties.getUrl());
        maps.put("username",properties.getUsername());
        maps.put("password",properties.getPassword());
        return maps;
    }


    //备份数据库
    public static void backup () throws Exception {
        String savePath = "/opt/log/sqlBack/" + Instant.now().getNano() + ".sql";
        String[] execCMD = new String[] {"mysqldump", "-u" + "root", "-p" + "12369", "dbname", "-r" + savePath, "--skip-lock-tables"};
        Process process = Runtime.getRuntime().exec(execCMD);
        int processComplete = process.waitFor();
        if (processComplete == 0) {
            System.out.println("备份成功.");
        } else {
            throw new RuntimeException("备份数据库失败.");
        }
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
    public static String getIp() {
        return ip;
    }
    public static void setIp(String ip) {
        RunTimeInfo.ip = ip;
    }
    public static String getPort() {
        return port;
    }
    public static void setPort(String port) {
        RunTimeInfo.port = port;
    }

    public static String getProfiles() {
        return profiles;
    }

    public static void setProfiles(String profiles) {
        RunTimeInfo.profiles = profiles;
    }

    public static String getServerUrl() {
        return serverUrl;
    }

    public static void setServerUrl(String serverUrl) {
        RunTimeInfo.serverUrl = serverUrl;
    }

    public DataSourceProperties getProperties() {
        return properties;
    }

    public void setProperties(DataSourceProperties properties) {
        this.properties = properties;
    }

    public StandardEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(StandardEnvironment environment) {
        this.environment = environment;
    }

    public GitProperties getGitProperties() {
        return gitProperties;
    }

    public void setGitProperties(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    public StartInfoService getStartInfoService() {
        return startInfoService;
    }

    public void setStartInfoService(StartInfoService startInfoService) {
        this.startInfoService = startInfoService;
    }
}
