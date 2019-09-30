package dc.pay.payrest.scheduleJobs;
import dc.pay.payrest.config.RunTimeInfo;
import dc.pay.payrest.config.StartInfo;
import dc.pay.payrest.util.PayRestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Component
public class RunTimeInfoJob {

    @Autowired
    RunTimeInfo runTimeInfo;

    @Autowired
    RedisTemplate redisTemplate;
    private HashOperations<String, String, String> hashOps;

    long PAYCOREHEARTBEATTIMEOUT = 30*24*60*60;  //超时时间
    private static final  String  DELIMITER_OR = "|";
    private static final  String  DELIMITER_MH = ":";

    @PostConstruct
    void init(){
        hashOps = redisTemplate.opsForHash();
    }

    //pay-rest心跳记录-redis-写入
    @Scheduled(cron  = "*/10 * * * * *")
    public void paycoreheartbeat()  {
        StartInfo startInfo = runTimeInfo.getStartInfo();
        String key ="payRestHeartBeat".concat(DELIMITER_MH).concat(PayRestUtil.formatDateTimeStrByParam(PayRestUtil.Const.dateString));
        redisTemplate.expire(key, PAYCOREHEARTBEATTIMEOUT, TimeUnit.SECONDS);//过期时间
        String hashKey = runTimeInfo.getServerId();
        String hashValue = startInfo.getGitShortCommitId().concat(DELIMITER_OR).concat(startInfo.getGitCommitTime()).concat(DELIMITER_OR).concat(startInfo.getGitBranch()).concat(DELIMITER_OR)
                           .concat(PayRestUtil.formatDateTime(runTimeInfo.getStartInfo().getStartDateTime())).concat(DELIMITER_OR).concat(PayRestUtil.curDateTimeStr());
        hashOps.put(key,hashKey,hashValue);
    }




}
