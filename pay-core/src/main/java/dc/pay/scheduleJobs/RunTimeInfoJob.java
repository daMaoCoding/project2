package dc.pay.scheduleJobs;

import com.alibaba.fastjson.JSON;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.RunTimeInfo;
import dc.pay.constant.PayEumeration;
import dc.pay.entity.po.AutoQueryDaifu;
import dc.pay.entity.runtime.StartInfo;
import dc.pay.mapper.runtime.StartInfoMapper;
import dc.pay.service.resDb.PayJmsSender;
import dc.pay.service.tj.TongJiService;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Component
public class RunTimeInfoJob {

    public static final String PAY_API_URL_HEALTH_CHECK = "payApiUrlHealthCheck";
    public static final String BILL_VERIFY = "billVerify";
    public static final String PAY_CORE_HEART_BEAT = "payCoreHeartBeat";
    public static final String PAY_REST_HEART_BEAT = "payRestHeartBeat";
    public static final String CLEAN_DATA_JOB = "cleanDataJob";
    public static final String MAX_ID = "maxId";
    public static final String COUNT_ID = "countId";
    @Autowired
    RunTimeInfo runTimeInfo;

    @Autowired
    RedisTemplate redisTemplate;
    private HashOperations<String, String, String> hashOps;

    @Autowired
    private StartInfoMapper startInfoMapper;

    @Autowired
    private TongJiService tongJiService;

    @Autowired
    private PayJmsSender payJmsSender;

    @Autowired
    HandlerUtil handlerUtil;


    long PAYCOREHEARTBEATTIMEOUT = 7*24*60*60;  //超时时间
    long ONEHOURSECONDS = 24*60*60;
    private static final  String  DELIMITER_OR = "|";
    private static final  String  DELIMITER_MH = ":";



    @PostConstruct
    void init(){
        hashOps = redisTemplate.opsForHash();
    }




    //pay-core心跳记录-redis-写入
    @Scheduled(cron  = "*/10 * * * * *")
    public void paycoreheartbeat()  {
        StartInfo startInfo = runTimeInfo.getStartInfo();
        if(null!=startInfo && runTimeInfo!=null){
            String key = PAY_CORE_HEART_BEAT.concat(DELIMITER_MH).concat(DateUtil.formatDateTimeStrByParam(DateUtil.dateString));
            redisTemplate.expire(key, PAYCOREHEARTBEATTIMEOUT, TimeUnit.SECONDS);//过期时间
            String hashKey = runTimeInfo.getServerId();
            String hashValue = startInfo.getGitShortCommitId().concat(DELIMITER_OR).concat(startInfo.getGitCommitTime()).concat(DELIMITER_OR).concat(startInfo.getGitBranch()).concat(DELIMITER_OR)
                    .concat(DateUtil.formatDateTime(runTimeInfo.getStartInfo().getStartDateTime())).concat(DELIMITER_OR).concat(DateUtil.curDateTimeStr());
            hashOps.put(key,hashKey,hashValue);
        }

    }


    //pay-core心跳记录-redis-读取
    public  Map<String,String> getPayCoreHeartBeat(){
        String key = PAY_CORE_HEART_BEAT.concat(DELIMITER_MH).concat(DateUtil.formatDateTimeStrByParam(DateUtil.dateString));
        return  hashOps.entries(key);
    }

    //pay-rest心跳记录-redis-读取
    public  Map<String,String> getPayRestHeartBeat(){
        String key = PAY_REST_HEART_BEAT.concat(DELIMITER_MH).concat(DateUtil.formatDateTimeStrByParam(DateUtil.dateString));
        return  hashOps.entries(key);
    }

    //PayApiUrl记录-redis-读取
    public  Map<String, Map<String, Set<String>>>   getPayApiUrlHealthCheck(){
        String key = PAY_API_URL_HEALTH_CHECK.concat(DELIMITER_MH).concat(String.valueOf(DateTime.now().getHourOfDay()));
        HashOperations<String, String,  Map<String, Set<String>>> hashOps =  redisTemplate.opsForHash();
        return  hashOps.entries(key);
    }


    //清理数据
    @Scheduled(cron  = "0 0 6 * * *")
    public void cleanData()  {
        try{
            Instant start = Instant.now();
            String key = CLEAN_DATA_JOB.concat(DELIMITER_MH).concat(DateUtil.formatDateTimeStrByParam(DateUtil.dateString));
            String value = handlerUtil.getStrFromRedis(key);
            if(StringUtil.isBlank(value)){
                handlerUtil.saveStrInRedis(key,runTimeInfo.getServerId(),PAYCOREHEARTBEATTIMEOUT);
                Thread.sleep(HandlerUtil.getRandomNumber(2000,8000));
                value = handlerUtil.getStrFromRedis(key);
                if(value.equalsIgnoreCase(runTimeInfo.getServerId())){
                    //流水表时间
                    String payListCleanTime = DateUtil.minusDays( DateUtil.curDateStr().concat(DateUtil.sixFiftyNineClock), DateUtil.dateTimeString, 7);
                    //统计时间
                    String tjCleanTime = DateUtil.minusDays( DateUtil.curDateStr().concat(DateUtil.sixFiftyNineClock), DateUtil.dateTimeString, 100);
                    //请求流水表清理
                    Long deleteReqpayListCount=0L;
                    Map<Long,Long> reqPayListMaxCleanIdAndCount = startInfoMapper.getReqPayListMaxCleanIdAndCount(payListCleanTime);
                    if(null!=reqPayListMaxCleanIdAndCount && reqPayListMaxCleanIdAndCount.size()==2){
                        Long maxId = reqPayListMaxCleanIdAndCount.get(MAX_ID);
                        Long countId = reqPayListMaxCleanIdAndCount.get(COUNT_ID);
                        deleteReqpayListCount = startInfoMapper.cleanReqPayList(maxId);
                    }
                    //响应流水表清理
                    Long deleteRespayListCount=0L;
                    Map<Long,Long> resPayListMaxCleanIdAndCount = startInfoMapper.getResPayListMaxCleanIdAndCount(payListCleanTime);
                    if(null!=resPayListMaxCleanIdAndCount && resPayListMaxCleanIdAndCount.size()==2){
                        Long maxId = resPayListMaxCleanIdAndCount.get(MAX_ID);
                        Long countId = resPayListMaxCleanIdAndCount.get(COUNT_ID);
                        deleteRespayListCount = startInfoMapper.cleanResPayList(maxId);
                    }


                    //请求[代付]流水表清理
                    Long deleteReqDaifuListCount=0L;
                    Map<Long,Long> reqDaifuListMaxCleanIdAndCount = startInfoMapper.getReqDaifuListMaxCleanIdAndCount(payListCleanTime);
                    if(null!=reqDaifuListMaxCleanIdAndCount && reqDaifuListMaxCleanIdAndCount.size()==2){
                        Long maxId = reqDaifuListMaxCleanIdAndCount.get(MAX_ID);
                        Long countId = reqDaifuListMaxCleanIdAndCount.get(COUNT_ID);
                        deleteReqDaifuListCount = startInfoMapper.cleanReqDaifuList(maxId);
                    }
                    //响应[代付]流水表清理
                    Long deleteResDaifuListCount=0L;
                    Map<Long,Long> resDaifuListMaxCleanIdAndCount = startInfoMapper.getResDaifuListMaxCleanIdAndCount(payListCleanTime);
                    if(null!=resDaifuListMaxCleanIdAndCount && resDaifuListMaxCleanIdAndCount.size()==2){
                        Long maxId = resDaifuListMaxCleanIdAndCount.get(MAX_ID);
                        Long countId = resDaifuListMaxCleanIdAndCount.get(COUNT_ID);
                        deleteResDaifuListCount = startInfoMapper.cleanResDaifuList(maxId);
                    }


                    //清理启动状态统计信息
                    Long cleanStartInfo =startInfoMapper.cleanStartInfo();
                    Long cleanTjByDay =startInfoMapper.cleanTjByDay(tjCleanTime);
                    Long cleanTjStatus = startInfoMapper.cleanTjStatus(tjCleanTime);
                    Instant end = Instant.now();
                    value = value.concat(",[FinishTime：").concat(DateUtil.getCurDateTime()).concat("],[CostTime:").concat(String.valueOf((Duration.between(start, end).toMillis())/1000L))
                            .concat("s],ReqPayList：[").concat(String.valueOf(deleteReqpayListCount))
                            .concat("],ResPayList:[").concat(String.valueOf(deleteRespayListCount))
                            .concat("],ReqDaifuList:[").concat(String.valueOf(deleteReqDaifuListCount))
                            .concat("],ResDaifuList:[").concat(String.valueOf(deleteResDaifuListCount))
                            .concat("],StartInfo:[").concat(String.valueOf(cleanStartInfo))
                            .concat("],TjByDay:[").concat(String.valueOf(cleanTjByDay))
                            .concat("],TjByDayStatus:[").concat(String.valueOf(cleanTjStatus)).concat("]");
                    handlerUtil.saveStrInRedis(key,value,PAYCOREHEARTBEATTIMEOUT);
                }
            }
        }catch (Exception e){
        }
    }

    //保存核对成功入款笔数和总金额
    public void billVerify(String dateTimeStr,long secondes,Map result)  {
        String key = BILL_VERIFY.concat(DELIMITER_MH).concat(DateUtil.formatDateTimeStrByParam(DateUtil.dateString));
        String hashKey = runTimeInfo.getServerId().concat(DELIMITER_OR).concat(String.valueOf(secondes)).concat(DELIMITER_OR).concat(dateTimeStr);
        String hashValue =  JSON.toJSONString(result).replaceAll("\"","'");
        hashOps.put(key,hashKey,hashValue);
        redisTemplate.expire(key, PAYCOREHEARTBEATTIMEOUT, TimeUnit.SECONDS);//过期时间
    }


   // @Scheduled(cron  = "0 0 * * * *")
    public void payApiUrlHealthCheck()  {
        Map<String, Map<String, Set<String>>>  payApiUrlMaps = tongJiService.payApiUrl();
        if(null!=payApiUrlMaps && MapUtils.isNotEmpty(payApiUrlMaps)){
            String key =PAY_API_URL_HEALTH_CHECK.concat(DELIMITER_MH).concat(String.valueOf(DateTime.now().getHourOfDay()));
            boolean hashKey = redisTemplate.opsForHash().hasKey(key,payApiUrlMaps.keySet().iterator().next());
            if(!hashKey){
                handlerUtil.saveMapInRedis(key,ONEHOURSECONDS,payApiUrlMaps);
            }
        }
    }


    @Scheduled(cron  = "*/10 * * * * *")
    public void addQueryOrderJob()  {
        if(null!=runTimeInfo && StringUtils.isNotBlank(runTimeInfo.getServerId())){
            int i=100;//每次处理最大个数
            String key = String.format(PayEumeration.AutoQueryDaifuKeyTMP,runTimeInfo.getServerId());
            Map<String, AutoQueryDaifu> entries = redisTemplate.opsForHash().entries(key);
            for (Map.Entry<String, AutoQueryDaifu> entry : entries.entrySet()) {
                String  hashKey = entry.getKey();
                AutoQueryDaifu hashValue = entry.getValue();
                if(StringUtils.isNotBlank(hashKey) && null!=hashValue && i>0 ){
                    long delay = hashValue.getQueryTime() - System.currentTimeMillis();
                    //清理2天
                    if(hashValue.getTimes()>=hashValue.getTotalTimes() && System.currentTimeMillis()- hashValue.getQueryTime()>=2*24*60*60*1000){
                        redisTemplate.opsForHash().delete(hashValue.getKey(),hashValue.getOrderId());
                        return;
                    }
                    //MQ过长处理中
                    if(hashValue.isMqInProcess()  && System.currentTimeMillis()- hashValue.getQueryTime()>=120*1000 &&hashValue.getTimes()<=hashValue.getTotalTimes()){
                        hashValue.setMqInProcess(false);
                        delay = 0;
                    }
                    //处理中的不发送，到达次数的不发送
                    if(hashValue.isMqInProcess() || hashValue.getTimes()>hashValue.getTotalTimes()){
                        return;
                    }
                    hashValue.setMqInProcess(true);
                    hashValue.setTimes(hashValue.getTimes()+1);
                    redisTemplate.opsForHash().put(key,hashKey,hashValue);
                    payJmsSender.sendLater(hashValue,delay);
                    i--;
                }
            }
        }

    }




}
