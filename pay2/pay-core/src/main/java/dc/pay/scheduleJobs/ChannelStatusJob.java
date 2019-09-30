package dc.pay.scheduleJobs;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import dc.pay.config.PayProps;
import dc.pay.entity.tj.TongJi;
import dc.pay.entity.po.SortedChannel;
import dc.pay.entity.po.SortedChannelStatus;
import dc.pay.service.tj.ChannelStatusJobService;
import dc.pay.service.tj.TjStatusService;
import dc.pay.service.tj.TongJiService;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ChannelStatusJob {
    public static Map<String,Object> channelStatusWarning =  Maps.newHashMap(); //报警
    private static final Logger log =  LoggerFactory.getLogger(ChannelStatusJob.class);

    @Value("${payProps.enableChannelStatusJob:false}")
    private boolean   enableChannelStatusJob;  //是否开启统计通道状态任务

    @Value("${payProps.enableNotifDbChannelSort:false}")
    private boolean   enableNotifDbChannelSort;  //是否开启通知db排序(多个pay服务，运维需设置只有一个启动)


    private  final int  SECOND =5*60;             //统计时间(秒),最大65年
    public static  final Double reqWarning = 75.00d ;  //请求成功率，低于80%报警
    public static  final Double resWarning = 25.00d ;  //支付成功率，低于25%报警

    public static final int  SECONDFORDBCHANNELSORT =10*60; //统计时间(秒),通知db接口通道排序，rest手动调用排序


    public static Map<String,String> tj_status_by_day = Maps.newHashMap();  //缓存本日统计状态结果

    @Autowired
    private TongJiService tongJiService;

    @Autowired
    private ChannelStatusJobService channelStatusJobService;


    @Autowired
    HandlerUtil handlerUtil;

    @Autowired
    PayProps payProps;


    @Autowired
    private TjStatusService tjStatusService;


    //意见反馈，报警
    @Scheduled(fixedRate = SECOND*1000L)
    public void reportCurrentTime() {
        if(enableChannelStatusJob){
            TongJi tongJi = new TongJi();
            tongJi.setRows(Integer.MAX_VALUE);
            tongJi.setRiQiFanWei(DateUtil.printFromNowMinuteAgo(SECOND));
            List<TongJi> tongJiList = tongJiService.getAllCgl(tongJi);
            ArrayList<SortedChannelStatus> sortedChannelStatuses = tongJiService.sortChannelStatus(tongJiList, null);
            channelStatusWarning.put("rqfw",tongJi.getRiQiFanWei());//日期范围
            channelStatusWarning.put("list",sortedChannelStatuses);
            channelStatusWarning.put("tdzs",sortedChannelStatuses.size()); //通道总数
            channelStatusWarning.put("yczs",tongJiService.getWarningChannelCount(sortedChannelStatuses));//异常总数
            channelStatusWarning.put("zczs",(Integer.parseInt(channelStatusWarning.get("tdzs").toString()))-(Integer.parseInt(channelStatusWarning.get("yczs").toString())) );//正常总数
        }

    }


    //通知数据库排序通道
    @Scheduled(fixedRate = SECONDFORDBCHANNELSORT*1000L)
    public void notifDbChannelSort() throws JsonProcessingException {
        if(enableNotifDbChannelSort){
            DateTime now = new DateTime();
            long calTimeStart = now.minusSeconds(SECONDFORDBCHANNELSORT).getMillis(); //本次统计结束时间
            long calTimeEnd   =now.getMillis();  //本次统计开始时间
            Map<String,List<TongJi>> allCglFordbChannelSort = notifDbChannelSortForRest();
            allCglFordbChannelSort.forEach((key,value)->{
                ArrayList<SortedChannel> resultList = tongJiService.sortChannel(value,null);
                List<Map<String, Object>> notifDbChannelSortList = tongJiService.processNotifDbChannelSort(resultList);//排序内容
                Map<String,Object> result = Maps.newHashMap();
                result.put("calTime",System.currentTimeMillis());
                result.put("calTimeStart",calTimeStart);
                result.put("calTimeEnd",calTimeEnd);
                result.put("calResult",notifDbChannelSortList);
                String dbresponse = handlerUtil.sendToMS(payProps.getNotifDbChannelSortUrl(), channelStatusJobService.notifDbChannelSortAddOid(key), JSON.toJSONString(result), HttpMethod.POST);
                log.debug("[通知数据库排序通道],时间={}，HTTP-CUST-OID={}，url={}，JsonData={}，db返回={}",now,key,payProps.getNotifDbChannelSortUrl(),JSON.toJSONString(result),dbresponse);
            });
        }
    }


    //通知数据库排序通道-rest手动点击
    public  Map<String,List<TongJi>> notifDbChannelSortForRest(){
        return tongJiService.notifDbChannelSortForRest();
    }




    //按日、oid统计成功率
    //@Scheduled(fixedRate = 5*60*60*1000L)
    //@Scheduled(fixedRate = 10*1000L)
    @Scheduled(cron  = "0 2/10 07 * * *")
    public void getAllCglByDay()  {
        tongJiService.getAllCglByDay(tj_status_by_day,null);
    }



}
