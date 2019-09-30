package dc.pay.entity.runtime;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import dc.pay.base.BaseEntity;
import dc.pay.utils.RsaUtil;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;

import javax.persistence.Column;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * ************************
 * 启动信息
 * @author tony 3556239829
 */
public class StartInfo  extends BaseEntity{

    @Column(name = "serverID")
    String serverID;
    @Column(name = "profiles")
    String profiles;
    @Column(name = "appname")
    String appname;
    @Column(name = "ipAddress")
    String ipAddress;
    @Column(name = "port")
    String port;
    @Column(name = "startDateTime")
    Date startDateTime;
    @Column(name = "dbInfo")
    String dbInfo;
    @Column(name = "gitBranch")
    String gitBranch;
    @Column(name = "gitCommitId")
    String gitCommitId;
    @Column(name = "gitShortCommitId")
    String gitShortCommitId;
    @Column(name = "gitCommitTime")
    String gitCommitTime;
    @Column(name = "commondLine")
    String commondLine;
    @Column(name = "memoryInfo")
    String memoryInfo;

    public StartInfo() {
    }

    public StartInfo(StandardEnvironment environment, GitProperties gitProperties,Map<String,Long> memoryInfo, Map<String, String> dbInfo, String... args)  {
        List<String> inputArguments = new ArrayList(ManagementFactory.getRuntimeMXBean().getInputArguments());
        if(CollectionUtils.isEmpty(inputArguments))inputArguments= Lists.newArrayList();
        inputArguments.addAll(Arrays.asList(args));
        this.serverID  =  environment.getProperty("spring.cloud.client.ipAddress").concat(":").concat(environment.getProperty("server.port"));
        this.profiles = environment.getProperty("spring.profiles");
        this.appname = environment.getProperty("eureka.instance.appname");
        this.ipAddress = environment.getProperty("spring.cloud.client.ipAddress");
        this.port = environment.getProperty("server.port");
        try {
            this.dbInfo =  RsaUtil.encrypt(JSON.toJSONString(dbInfo));
        } catch (Exception e) {}
        this.gitBranch = gitProperties.getBranch();
        this.gitCommitId = gitProperties.getCommitId();
        this.gitShortCommitId = gitProperties.getShortCommitId();
        this.memoryInfo = JSON.toJSONString(memoryInfo);
        this.commondLine = JSON.toJSONString(inputArguments);
        this.gitCommitTime = gitProperties.get("commit.time");
        this.startDateTime=new Date();
    }


    public String getServerID() {
        return serverID;
    }

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getDbInfo() {
        return dbInfo;
    }

    public void setDbInfo(String dbInfo) {
        this.dbInfo = dbInfo;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public String getGitCommitId() {
        return gitCommitId;
    }

    public void setGitCommitId(String gitCommitId) {
        this.gitCommitId = gitCommitId;
    }

    public String getGitShortCommitId() {
        return gitShortCommitId;
    }

    public void setGitShortCommitId(String gitShortCommitId) {
        this.gitShortCommitId = gitShortCommitId;
    }

    public String getMemoryInfo() {
        return memoryInfo;
    }

    public void setMemoryInfo(String memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    public String getCommondLine() {
        return commondLine;
    }

    public void setCommondLine(String commondLine) {
        this.commondLine = commondLine;
    }

    public String getGitCommitTime() {
        return gitCommitTime;
    }

    public void setGitCommitTime(String gitCommitTime) {
        this.gitCommitTime = gitCommitTime;
    }
}
