package dc.pay.service.runtime;

import com.github.pagehelper.PageHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.entity.runtime.StartInfo;
import dc.pay.mapper.runtime.StartInfoMapper;
import dc.pay.scheduleJobs.RunTimeInfoJob;
import dc.pay.service.tj.TongJiService;
import dc.pay.utils.RestTemplateUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

/**
 * ************************
 * @author tony 3556239829
 */
@Service
public class StartInfoService {

    public static final String GIT_COMMIT_ID = "gitCommitId";
    public static final String GIT_COMMIT_DATE = "gitCommitDate";
    public static final String BRANCH = "branch";
    public static final String START_DATE = "startDate";
    public static final String HB_TIME = "HBTime";
    public static final String SERVER_ID = "serverID";
    @Autowired
    StartInfoMapper startInfoMapper;


    @Autowired
    RunTimeInfoJob runTimeInfoJob;

    @Autowired
    private TongJiService tongJiService;


    public List<StartInfo> getAll(StartInfo startInfo) {
        if (null!=startInfo && startInfo.getPage() != null && startInfo.getRows() != null) {
            PageHelper.startPage(startInfo.getPage(), startInfo.getRows());
        }

        Example example = new Example(StartInfo.class);
        Example.Criteria criteria = example.createCriteria();

        if(null!=startInfo && StringUtils.isNotBlank(startInfo.getGitCommitId())) {
            criteria.andEqualTo(GIT_COMMIT_ID, startInfo.getGitCommitId());
            example.setOrderByClause("serverID asc,id desc");
        }
        if(null!=startInfo && StringUtils.isNotBlank(startInfo.getProfiles())){
           criteria.andEqualTo("profiles", startInfo.getProfiles());
           example.setOrderByClause("serverID asc,id desc");
        }

        if(null==startInfo || ( StringUtils.isBlank(startInfo.getGitCommitId()) &&  StringUtils.isBlank(startInfo.getProfiles())  )){
            example.setOrderByClause("id desc");
        }

        example.setDistinct(false);
       List<StartInfo>  startInfoLists = startInfoMapper.selectByExample(example);
        return startInfoLists;
    }

    public StartInfo getById(Long id) {
        return startInfoMapper.selectByPrimaryKey(id);
    }


    public void save(StartInfo startInfo) {
        if (startInfo.getId() != null) {
            startInfoMapper.updateByPrimaryKey(startInfo);
        } else {
            startInfoMapper.insert(startInfo);
        }
    }


    //pay-core心跳记录-redis-读取
    public  List<Map<String,String>> getPayCoreHeartBeat(){
        Map<String, String> redisMaps = runTimeInfoJob.getPayCoreHeartBeat();
        List<Map<String,String>> resultList = Lists.newLinkedList();
        redisMaps.forEach((k,v)->{
            if(null!=v && v.split("\\|").length==5){
                HashMap<String, String> parseMaps = Maps.newHashMap();
                parseMaps.put(GIT_COMMIT_ID,v.split("\\|")[0]);
                parseMaps.put(GIT_COMMIT_DATE,v.split("\\|")[1]);
                parseMaps.put(BRANCH,v.split("\\|")[2]);
                parseMaps.put(START_DATE,v.split("\\|")[3]);
                parseMaps.put(HB_TIME,v.split("\\|")[4]);
                parseMaps.put(SERVER_ID,k);
                resultList.add(parseMaps);
            }
        });
        return resultList;
    }



    //pay-rest心跳记录-redis-读取
    public  List<Map<String,String>> getPayRestHeartBeat(){
        Map<String, String> redisMaps = runTimeInfoJob.getPayRestHeartBeat();
        List<Map<String,String>> resultList = Lists.newLinkedList();
        redisMaps.forEach((k,v)->{
            if(null!=v && v.split("\\|").length==5){
                HashMap<String, String> parseMaps = Maps.newHashMap();
                parseMaps.put(GIT_COMMIT_ID,v.split("\\|")[0]);
                parseMaps.put(GIT_COMMIT_DATE,v.split("\\|")[1]);
                parseMaps.put(BRANCH,v.split("\\|")[2]);
                parseMaps.put(START_DATE,v.split("\\|")[3]);
                parseMaps.put(HB_TIME,v.split("\\|")[4]);
                parseMaps.put(SERVER_ID,k);
                resultList.add(parseMaps);
            }
        });
        return resultList;
    }



    //PayApiUrl记录-redis-读取
    public   List<Map<String,Object>>    getPayApiUrlHealthCheck(){
        List<Map<String,Object>>    payApiUrlList = Lists.newLinkedList();
       // Map<String, Map<String, Set<String>>>  payApiUrlMaps= runTimeInfoJob.getPayApiUrlHealthCheck();
        Map<String, Map<String, Set<String>>>  payApiUrlMaps= tongJiService.payApiUrl();
        payApiUrlMaps.forEach((k,v)->{
            if(null!=v){
                HashMap<String, Object> parseMaps = Maps.newHashMap();
                parseMaps.put("OID",k);
                parseMaps.put("web_url",v.get("web_url"));
                parseMaps.put("jump_url",RestTemplateUtil.payapiurlhealthcheck(v.get("jump_url")));
                parseMaps.put("notify_url",RestTemplateUtil.payapiurlhealthcheck(v.get("notify_url")));
                payApiUrlList.add(parseMaps);
            }
        });
        return payApiUrlList;
    }


}
