package dc.pay.service.tj;


import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.constant.PayEumeration;
import dc.pay.entity.pay.PayApiUrl;
import dc.pay.entity.tj.TjStatus;
import dc.pay.entity.tj.TongJi;
import dc.pay.entity.po.SortedChannel;
import dc.pay.entity.po.SortedChannelStatus;
import dc.pay.mapper.tj.TongJiMapper;
import dc.pay.scheduleJobs.ChannelStatusJob;
import dc.pay.service.cache.CacheService;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.SortUtils;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.page.PageModel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ************************
 *
 * @author tony 3556239829
 */

@Service
public class TongJiService {
    private static final Logger log =  LoggerFactory.getLogger(TongJiService.class);
    public static final String  ALL = "ALL";
    public static final String  SUCCESS = "SUCCESS";
    public static final String  ERROR = "ERROR";
    public static final String  ZERO = " 07:00:00 - ";
    public static final String  FIVENINE = " 06:59:59";
    public static final String  YES = "是";
    public static final String  dateFormater = "yyyy-MM-dd";
    public static final String  seven = " 07:00:00";
    public static final String  six = " 06:59:59";
    public static final String  startDateTimeByDayStart = "startDateTimeByDayStart" ;
    public static final String  endDateTimeByDayStart = "endDateTimeByDayStart" ;
    public static final String  startDateByBatchDay = "startDateByBatchDay" ;
    public static final String  endDateByBatchDay = "endDateByBatchDay" ;
    public static final String  startDateTimeByDayEnd = "startDateTimeByDayEnd" ;
    public static final String  endDateTimeByDayEnd = "endDateTimeByDayEnd" ;
    public static final String  desc = "desc";
    public static final String  asc = "asc";
    public static final String  ltcgv = "ltcgv";
    public static final String  getReqSuccessDivReqSumD = "getReqSuccessDivReqSumD";
    public static final String  zfcgl = "zfcgl";
    public static final String  getResSuccessDivReqSuccessD = "getResSuccessDivReqSuccessD";
    public static final String  cgcs = "cgcs";
    public static final String  getPageResSuccessSumL = "getPageResSuccessSumL";
    public static final String  cgrks = "cgrks";
    public static final String  getPageResSuccessAmountL = "getPageResSuccessAmountL";
    public static final String  pageInfo  ="pageInfo";
    public static final String  queryParam  ="queryParam";
    public static final String  page  ="page";
    public static final String  rows  ="rows";
    public static final String  allCglTotal  ="allCglTotal";
    public static final String  oidMaps  ="oidMaps";
    public static final String  payTypeMaps  ="payTypeMaps";
    public static final String  payCoMaps  ="payCoMaps";

     public static final String   pageReqSuccessSum ="pageReqSuccessSum";
     public static final String   pageReqSum ="pageReqSum";
     public static final String   pageResSuccessSum ="pageResSuccessSum";
     public static final String   pageResSuccessAmount ="pageResSuccessAmount";


    @Autowired
    TongJiMapper tongJiMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private TjStatusService tjStatusService;

    @Autowired
    private ChannelStatusJobService channelStatusJobService;


    /**
     * 查找通道中文名称
     */
    public static String  getChannelCNameByChannelName(String channelName){
        return  HandlerUtil.getChannelCNameByChannelName(channelName);
    }

    /**
     * 成功率(按通道统计)-图表-单条通道
     */
    public List<TongJi> getAllCglByChannel(TongJi tongJi) {
        String startDateTime=tongJi.getRiQiFanWei().split(" - ")[0];
        String endDateTime=tongJi.getRiQiFanWei().split(" - ")[1];
        String channelName = tongJi.getChannelName();
        String timeStmpStr = "%Y-%m-%d %H:00:00"; //默认按小时分组，超过3天按天排序

        int days = DateUtil.daysBetween(startDateTime,endDateTime,"yyyy-MM-dd HH:mm:ss");
        if(days>=7){
            timeStmpStr = "%Y-%m-%d"; //按照日期查询
        }
        if(StringUtil.isBlank(tongJi.getOid())){ //统计业主
            tongJi.setTongJiType("ALL");
        }
        List<TongJi>  tongJiListProd = tongJiMapper.getAllCglByChannel(tongJi,startDateTime,endDateTime,channelName,timeStmpStr); //分
         Map<String,TongJi>  tongJiList = proDbTongJiListByChannel(tongJiListProd,channelName); //组装数据
         List<TongJi> listByMapValue = getListByMapValue(tongJiList);
        return listByMapValue;

    }


    /**
     * 成功率(按通道统计)-图表-全部通道
     */
    public List<TongJi> getAllCglByAllChannel(TongJi tongJi) {
        String startDateTime=tongJi.getRiQiFanWei().split(" - ")[0];
        String endDateTime=tongJi.getRiQiFanWei().split(" - ")[1];
        String channelName = tongJi.getChannelName();
        String timeStmpStr = "%Y-%m-%d %H:00:00"; //默认按小时分组，超过3天按天排序
        String time_stmp_col_name="time_stmp"; //按小时查不加7小时
        int days = DateUtil.daysBetween(startDateTime,endDateTime,"yyyy-MM-dd HH:mm:ss");
        if(days>=7){
            timeStmpStr = "%Y-%m-%d"; //按照日期查询
            time_stmp_col_name="DATE_SUB(time_stmp, INTERVAL 7 DAY_HOUR )";//按日期查，+7小时
        }
        if(StringUtil.isBlank(tongJi.getOid())){ //统计业主
            tongJi.setOid("ALL");
        }
        if(StringUtil.isBlank(tongJi.getTongJiType())){ //统计类型
            tongJi.setTongJiType("ALL");
        }
        List<TongJi>  tongJiListProd = tongJiMapper.getAllCglByAllChannel(tongJi,time_stmp_col_name,startDateTime,endDateTime,timeStmpStr); //分
         Map<String,TongJi>  tongJiList = proDbTongJiListByChannel(tongJiListProd,channelName); //组装数据
         List<TongJi> listByMapValue = getListByMapValue(tongJiList);
        return listByMapValue;

    }


    /**
     * 处理页面通道Echar统计数据
     * @param listByMapValue
     * @return
     */
    public  ArrayList<Object> processEcharByChannelName(List<TongJi> listByMapValue)  {
        ArrayList<Object> resultList = Lists.newArrayList();
        for (TongJi tongJi : listByMapValue) {
            try {
                tongJi.setPageResSuccessAmount(HandlerUtil.getYuan(tongJi.getPageResSuccessAmount()));
            } catch (PayException e) {
                e.printStackTrace();
                return Lists.newArrayList();
            }
            resultList.add( new Object[] {tongJi.getReqTimeStmp(),tongJi.getPageReqSum(),tongJi.getPageReqSuccessSum(),tongJi.getPageResSuccessSum(),tongJi.getPageResSuccessAmount()});
        }
        return resultList;
    }



    //处理数据库统计信息-通道统计
    private Map<String,TongJi> proDbTongJiListByChannel(List<TongJi>  tongJiList,String channelName) {
        //提取统计时间(区间)
        TreeMap<String,TongJi> timeStmpsMaps = Maps.newTreeMap();
        for (TongJi tongJi : tongJiList) {
            TongJi tongJiRes = new TongJi();
            timeStmpsMaps.put(tongJi.getReqTimeStmp().trim(),tongJiRes);
        }
        //配置统计信息
        for (TongJi tongJi : tongJiList) {
            String reqTimeStmp = tongJi.getReqTimeStmp();
            String reqResult = tongJi.getReqResult();

            if (!timeStmpsMaps.containsKey(reqTimeStmp)) {
                continue;
            }
            if(SUCCESS.equalsIgnoreCase(reqResult)){
                timeStmpsMaps.get(reqTimeStmp).setPageReqSuccessSum(tongJi.getReqSum());  //请求成功
                timeStmpsMaps.get(reqTimeStmp).setPageReqSuccessAmount(tongJi.getReqAmount()); //请求成功金额

                timeStmpsMaps.get(reqTimeStmp).setPageResSuccessSum(tongJi.getResSum());      //响应成功
                timeStmpsMaps.get(reqTimeStmp).setPageResSuccessAmount(tongJi.getResAmount());//响应成功金额
            }
            if(ERROR.equalsIgnoreCase(reqResult)){
                timeStmpsMaps.get(reqTimeStmp).setPageReqErrorSum(tongJi.getReqSum()); //请求失败
            }
            timeStmpsMaps.get(reqTimeStmp).setReqTimeStmp(reqTimeStmp);
            timeStmpsMaps.get(reqTimeStmp).setPageChannelName(channelName);
        }
            return timeStmpsMaps;
    }


    /**
     * 通过tongji 的日期范围，查找日期范围内oid个数
     */
    public  int  getOidCount(TongJi tongJi){
        return  cacheService.getOidCount(tongJi);
    }




    /**
     * 成功率(全部通道,微信,支付宝,QQ,百度,京东,网银)
     * @param tongJi
     * @return
     */
    public List<TongJi> getAllCgl(TongJi tongJi) {
        setBlankRiqiFanWei(tongJi);
        if(StringUtil.isBlank(tongJi.getTongJiType())){ //统计类型,ALL,_ZFB_,_QQ_,_JD_,_BD_,_WY_
            tongJi.setTongJiType("ALL");
        }
        if(StringUtil.isBlank(tongJi.getOid())){ //统计业主
            tongJi.setOid("ALL");
        }

         int tjDays = DateUtil.daysBetween(tongJi.getRiQiFanWei().split(" - ")[0], tongJi.getRiQiFanWei().split(" - ")[1], DateUtil.dateTimeString); //统计时间1年内
         if(tjDays>366){
             log.error("查询日期超过1年:{},修改为1年内：{}",tongJi.getRiQiFanWei(),DateUtil.getDateTimeOneYearAgo());
             tongJi.setRiQiFanWei(DateUtil.getDateTimeOneYearAgo());
         }

        String startDateTime=tongJi.getRiQiFanWei().split(" - ")[0];
        String endDateTime=tongJi.getRiQiFanWei().split(" - ")[1];


        if(endDateTime.endsWith(seven)){endDateTime = endDateTime.replaceAll(seven,six);}//临时解决前端传至7:00-7:00,-->改为 x:6.59


        List<TongJi> tongJiListProd =null;
        if(DateUtil.betweenDateTime(startDateTime, endDateTime, DateUtil.dateTimeString) >86399){//跨天查询
            //构造开始查询日期时间
            // "startDateTimeByDayStart": "2018-02-01 08:00:00",  "endDateTimeByDayStart":  "2018-02-02 06:59:59",
            // "startDateByBatchDay":     "2018-02-02",           "endDateByBatchDay":      "2018-02-03",
            // "startDateTimeByDayEnd":   "2018-02-04 07:00:00",  "endDateTimeByDayEnd":    "2018-02-05 05:59:59"
            Map<String, String> tjDateTime = TongJiService.getTjDateTime(startDateTime, endDateTime);
            if( MapUtils.isEmpty(tjDateTime) || !tjDateTime.containsKey(startDateByBatchDay) ||StringUtils.isBlank(tjDateTime.get(startDateByBatchDay) ) || !tjDateTime.containsKey(endDateByBatchDay) ||StringUtils.isBlank(tjDateTime.get(endDateByBatchDay) ) ){
                tongJiListProd = cacheService.getAllCgl(tongJi, startDateTime, endDateTime); //不查统计
            }else{
                List<TongJi> dateTimeByDayStart=null ,dateTimeByDayEnd=null,dateByBatchDay=null; //开始  //结束 //统计
                if(valdataMap(tjDateTime,startDateTimeByDayStart) && valdataMap(tjDateTime,endDateTimeByDayStart)){
                    dateTimeByDayStart =  cacheService.getAllCgl(tongJi, tjDateTime.get(startDateTimeByDayStart), tjDateTime.get(endDateTimeByDayStart));
                }
                if( valdataMap(tjDateTime,startDateTimeByDayEnd) && valdataMap(tjDateTime,endDateTimeByDayEnd)){
                    dateTimeByDayEnd = cacheService.getAllCgl(tongJi, tjDateTime.get(startDateTimeByDayEnd), tjDateTime.get(endDateTimeByDayEnd));
                }
                if( valdataMap(tjDateTime,startDateByBatchDay) && valdataMap(tjDateTime,endDateByBatchDay)){
                    dateByBatchDay = cacheService.getAllCglInTjByDay(tongJi,tjDateTime.get(startDateByBatchDay),tjDateTime.get(endDateByBatchDay));
                }
                 tongJiListProd =  mergeTongJiList(dateTimeByDayStart,dateByBatchDay,dateTimeByDayEnd);//合并按天统计结果与统计记录结果
            }

        }else{
            tongJiListProd = cacheService.getAllCgl(tongJi, startDateTime, endDateTime); //查询1天内
        }
        Map<String,TongJi>  tongJiList = proDbTongJiList(tongJiListProd,tongJi);  //过滤
        List<TongJi> listByMapValue = getListByMapValue(tongJiList);
        listByMapValue = sortList(tongJi,listByMapValue);//页面排序
        return listByMapValue;
    }


    //简单验证Map
    public static boolean valdataMap( Map<String, String> tjDateTime,String key){
        if(MapUtils.isNotEmpty(tjDateTime)){
            return  tjDateTime.containsKey(key) && StringUtils.isNotBlank(tjDateTime.get(key));
        }
        return  false;
    }


     /**
     * 成功率,通道，OID,MemberID(全部通道,微信,支付宝,QQ,百度,京东,网银)
     * @param tongJi
     * @return
     */
    public Map<String,List<TongJi>>  getAllCglFordbChannelSort(TongJi tongJi) {
        setBlankRiqiFanWei(tongJi);
        if(StringUtil.isBlank(tongJi.getTongJiType())){ //统计类型,ALL,_ZFB_,_QQ_,_JD_,_BD_,_WY_
            tongJi.setTongJiType("ALL");
        }
        if(StringUtil.isBlank(tongJi.getOid())){ //统计业主
            tongJi.setOid("ALL");
        }
        String startDateTime=tongJi.getRiQiFanWei().split(" - ")[0];
        String endDateTime=tongJi.getRiQiFanWei().split(" - ")[1];
        List<TongJi> tongJiListProd = cacheService.getAllCglFordbChannelSort(tongJi, startDateTime, endDateTime);
        Map<String, List<TongJi>>   tongJiList = proDbTongJiListByKeyOid(tongJiListProd,tongJi);  //过滤
        return tongJiList;
    }






    public   Map<String,List<TongJi>>  groupByOid( List<TongJi> listByMapValue){
        Map<String,List<TongJi>>  result =Maps.newHashMap();
        for (TongJi tj : listByMapValue) {
            String reqOid = tj.getReqOid();
            if(StringUtils.isNotBlank(reqOid)){
                if(result.containsKey(reqOid)){
                    result.get(reqOid).add(tj);
                }else{
                    List<TongJi> tjList = Lists.newArrayList();
                    tjList.add(tj);
                    result.put(reqOid,tjList);
                }
            }
        }
        return result;
    }







     // 页面排序
    public List<TongJi> sortList(TongJi tongJi,List<TongJi> listByMapValue){
        SortUtils<TongJi> sortList = new SortUtils<TongJi>();
        String orderBy = tongJi.getOrderBy();  //getReqSuccessDivReqSumD,getResSuccessDivReqSuccessD,getPageResSuccessSumL,getPageResSuccessAmountL
        String sort = tongJi.getSort();         //desc 倒序
        if(StringUtils.isNotBlank(orderBy) && (StringUtils.isNotBlank(sort) &&(sort.equalsIgnoreCase(desc) || sort.equalsIgnoreCase(asc) ))){
            if(orderBy.equalsIgnoreCase(ltcgv)){ //联通成功率
                sortList.Sort(listByMapValue, getReqSuccessDivReqSumD, sort);
            }else if(orderBy.equalsIgnoreCase(zfcgl) ){ //支付成功率
                sortList.Sort(listByMapValue, getResSuccessDivReqSuccessD, sort);
            }else if(orderBy.equalsIgnoreCase(cgcs) ){ //成功次数
                sortList.Sort(listByMapValue, getPageResSuccessSumL, sort);
            }else if(orderBy.equalsIgnoreCase(cgrks)){ //成功入款数
                sortList.Sort(listByMapValue, getPageResSuccessAmountL, sort);
            }
        }
        return listByMapValue;
    }






    public void setBlankRiqiFanWei(TongJi tongJi) {
        if(StringUtil.isBlank(tongJi.getRiQiFanWei())){ //默认查询当天
            String riqiFanWei = getRiQiFanWei();
            if(StringUtils.isBlank(riqiFanWei)){
                riqiFanWei = DateUtil.formatDateTimeStrByParam("yyyy-MM-dd");
                riqiFanWei=riqiFanWei+ ZERO +riqiFanWei+ FIVENINE;
            }
            tongJi.setRiQiFanWei(riqiFanWei);
        }
    }


    /**
     * 汇总，不分页结果
     */
    public  Map<String,Long>  getAllCglTotal(TongJi tongJi){
        tongJi.setRows(Integer.MAX_VALUE); //全部统计，非本页统计
        tongJi.setPage(1);
        List<TongJi> tongJiList = getAllCgl(tongJi);
        PageModel pm = new PageModel(tongJiList, tongJi.getRows());
        List<TongJi> tongJiPageList = pm.getObjects(tongJi.getPage())==null?tongJiList:pm.getObjects(tongJi.getPage());
        Map<String,Long> allCglTotal = getAllCglTotal(tongJiPageList); //计算全部成功率统计-总和
        return allCglTotal;
    }




    /**
     * 成功率
     */
    public ModelAndView getCglModelAndView(TongJi tongJi) {
        ModelAndView result = new ModelAndView("tongji/channel");
        // PageHelper.startPage(tongJi.getPage(), tongJi.getRows());
        List<TongJi> tongJiList = getAllCgl(tongJi);
        PageModel pm = new PageModel(tongJiList, tongJi.getRows());
        List<TongJi> tongJiPageList = pm.getObjects(tongJi.getPage())==null?tongJiList:pm.getObjects(tongJi.getPage());
        Map<String,Long> allCglTotalMap = getAllCglTotal(tongJiPageList); //计算全部成功率统计-总和
       // Page<TongJi> pageT = new Page<>();
        PageInfo<TongJi> tongJiPageInfo = new PageInfo<>(tongJiPageList);
        tongJiPageInfo.setHasNextPage(pm.isHasNextPage());
        tongJiPageInfo.setHasPreviousPage(pm.isHasPreviousPage());
        tongJiPageInfo.setIsFirstPage(pm.isFirstPage());
        tongJiPageInfo.setIsLastPage(pm.isLastPage());
        tongJiPageInfo.setNavigateLastPage(pm.getTotalPages());
        tongJiPageInfo.setNavigatepageNums(pm.getNavigatepageNums());
        tongJiPageInfo.setPageNum(tongJi.getPage());
        tongJiPageInfo.setPageSize(tongJi.getRows());
        tongJiPageInfo.setPages(pm.getTotalPages());
        tongJiPageInfo.setTotal(pm.getTotalRows());
        tongJiPageInfo.setPrePage(pm.getPrePages());
        tongJiPageInfo.setNextPage(pm.getNextPages());

        result.addObject(pageInfo, tongJiPageInfo);
        result.addObject(queryParam, tongJi);
        result.addObject(page, tongJi.getPage());
        result.addObject(rows, tongJi.getRows());
        result.addObject(allCglTotal,allCglTotalMap);
        result.addObject(oidMaps, HandlerUtil.getAllOid());//业主们
        result.addObject(payTypeMaps, HandlerUtil.getAllPayType());//统计类型
        result.addObject(payCoMaps, HandlerUtil.getAllPayCo());//统计第三方名称(前缀)

        return result;
    }



    /**
     * 返回日期范围，今日7点前，返回昨日7点到今日6.59/今日7点后，返回今日7点到明日6.59
     * @return
     */
    public static String getRiQiFanWei(){
        DateTime todayDate = new DateTime( DateUtil.formatDateTimeStrByParam("yyyy-MM-dd"));
        LocalTime todayTime = new LocalTime();
        LocalTime seven = new LocalTime(07,00,00);
        LocalTime six = new LocalTime(06,59,59);
        if(todayTime.isAfter(seven)){
            return todayDate.withTime(seven).toString("yyyy-MM-dd HH:mm:ss").concat(" - ").concat(todayDate.withTime(seven).plus(86399999).toString("yyyy-MM-dd HH:mm:ss"));
        }else{
            return todayDate.withTime(seven).minusDays(1).toString("yyyy-MM-dd HH:mm:ss").concat(" - ").concat(todayDate.withTime(six).toString("yyyy-MM-dd HH:mm:ss"));
        }
    }


    /**
     * 返回日期， 7点前返回昨日，7点后返回今日
     * @return
     */
    public static String getNeedTjRiQi(){
        String searchDateBegin = DateUtil.formaterDate(getRiQiFanWei().split(" - ")[0], "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd");
       return  DateUtil.minusDays(searchDateBegin, "yyyy-MM-dd", 1);
    }



    /**
     * 昨天 ：1
     * 近7天: 7
     * 近30天:30
     * @return
     */
    public static String getRiQiFanWei(int days){
        DateTime todayDate = new DateTime( DateUtil.formatDateTimeStrByParam("yyyy-MM-dd"));
        LocalTime todayTime = new LocalTime();
        LocalTime seven = new LocalTime(07,00,00);
        LocalTime six = new LocalTime(06,59,59);
        if(todayTime.isAfter(seven)){
            return todayDate.withTime(seven).minusDays(days).toString("yyyy-MM-dd HH:mm:ss").concat(" - ").concat(todayDate.withTime(six).toString("yyyy-MM-dd HH:mm:ss"));
        }else{
            return  todayDate.withTime(seven).minusDays(days+1).toString("yyyy-MM-dd HH:mm:ss").concat(" - ").concat(todayDate.withTime(six).minusDays(1).toString("yyyy-MM-dd HH:mm:ss")) ;
        }
    }


    //处理数据库统计信息-keyOid
    private  Map<String, List<TongJi>> proDbTongJiListByKeyOid(List<TongJi>  tongJiList,TongJi searchCondition){
        Map<String, List<TongJi>>  result = Maps.newHashMap();
        Map<String, List<TongJi>> tongJiListOrderByOid = groupByOid(tongJiList);
        tongJiListOrderByOid.forEach((key,value)->{
            result.put(key,sortList(searchCondition,getListByMapValue( proDbTongJiList(value, searchCondition))));
        });
        return result;
    }





    //合并统计list
    public   List<TongJi>  mergeTongJiList(List<TongJi>... listTongji ){
        List<TongJi> mergedList  = Lists.newArrayList();
        for (List<TongJi> tongJis : listTongji) {
           if(CollectionUtils.isNotEmpty(tongJis)) CollectionUtils.addAll(mergedList,tongJis);
        }
        if(CollectionUtils.isNotEmpty(mergedList) ){
           Map<String, List<TongJi>> productMap =mergedList.stream().collect(Collectors.groupingBy(TongJi::getReqChannel, Collectors.toList()));
            List<TongJi> result = Lists.newArrayList();
            productMap.values().stream().forEach(x->doMergeMap(x,result));
            return   result;
        }
        return   mergedList;
    }



    //合并统计list/reqAmount/ReqSum/resAmount/resSum
    public   void doMergeMap(List<TongJi> x,List<TongJi> result){
        Map<String,TongJi> tmpTonjiMap= Maps.newHashMap();
        for (TongJi tongJi : x) {
            if(!tmpTonjiMap.containsKey(tongJi.getReqResult())){
                TongJi tjNew = new TongJi();  //HAHAHA....
                BeanUtils.copyProperties(tongJi,tjNew);
                tmpTonjiMap.put(tongJi.getReqResult(),tjNew);
            }else{
                TongJi tjInTmpMap = tmpTonjiMap.get(tongJi.getReqResult());
                tjInTmpMap.setReqAmount(HandlerUtil.plusLong(tongJi.getReqAmount(),tjInTmpMap.getReqAmount()) );
                tjInTmpMap.setReqSum(HandlerUtil.plusLong(tongJi.getReqSum(),tjInTmpMap.getReqSum()));
                tjInTmpMap.setResAmount(HandlerUtil.plusLong(tongJi.getResAmount(),tjInTmpMap.getResAmount()));
                tjInTmpMap.setResSum(HandlerUtil.plusLong(tongJi.getResSum(),tjInTmpMap.getResSum()));
            }
        }
        result.addAll(tmpTonjiMap.values());
    }






    //处理数据库统计信息
    private Map<String,TongJi> proDbTongJiList(List<TongJi>  tongJiList,TongJi searchCondition){
        //提取通道名称
        TreeMap<String,TongJi> channelNameMaps = Maps.newTreeMap();
        if(null==tongJiList) return channelNameMaps;
        try{
            for (TongJi tongJi : tongJiList) {
                TongJi tongJiRes = new TongJi();
                if(needPut(tongJi.getReqChannel().trim(),searchCondition)){  //过滤
                    channelNameMaps.put(tongJi.getReqChannel().trim(),tongJiRes);
                }
            }
            //配置统计信息
            for (TongJi tongJi : tongJiList) {
                String reqChannel = tongJi.getReqChannel().trim();
                String reqResult = tongJi.getReqResult();
                String resResult = tongJi.getResResult();
                if(!channelNameMaps.containsKey(reqChannel)){
                    continue;
                }
                //页面显示结果
                channelNameMaps.get(reqChannel).setPageChannelName(reqChannel);  //通道名称
                if(SUCCESS.equalsIgnoreCase(reqResult)){
                    channelNameMaps.get(reqChannel).setPageReqSuccessSum(tongJi.getReqSum());  //请求成功
                    channelNameMaps.get(reqChannel).setPageReqSuccessAmount(tongJi.getReqAmount()); //请求成功金额
                }
                if(ERROR.equalsIgnoreCase(reqResult)){
                    channelNameMaps.get(reqChannel).setPageReqErrorSum(tongJi.getReqSum()); //请求失败
                }
                if(SUCCESS.equalsIgnoreCase(resResult)){
                    channelNameMaps.get(reqChannel).setPageResSuccessSum(tongJi.getResSum());      //响应成功
                    channelNameMaps.get(reqChannel).setPageResSuccessAmount(tongJi.getResAmount());//响应成功金额
                }
                if(ERROR.equalsIgnoreCase(resResult)){
                    channelNameMaps.get(reqChannel).setPageResErrorSum(tongJi.getResSum()); //响应失败
                }
                if(StringUtils.isNotBlank(tongJi.getReqOid())){
                    channelNameMaps.get(reqChannel).setReqOid(tongJi.getReqOid()); //分组业主oid
                }

                if(StringUtils.isNotBlank(tongJi.getReqMemberID())){
                    channelNameMaps.get(reqChannel).setReqMemberID(tongJi.getReqMemberID()); //分组业主商户号
                }
            }
        }catch (Exception e){
            log.error("处理数据库统计信息出错,searchCondition:{},错误：{}",JSON.toJSONString(searchCondition),e.getMessage(),e);
        }
        return channelNameMaps;
    }


    /**
     * 通过查询条件，判断是否存入。
     * @return
     */
    public boolean needPut(String channelName,TongJi searchCondition){
        String tongJiType = searchCondition.getTongJiType().trim(); //统计类型
        String channelPrefix = searchCondition.getChannelPrefix().trim();//商户前缀
        String searchChannelName = searchCondition.getChannelName().trim(); //查询通道名称
        String searchOid = searchCondition.getOid();//查询业主OID

        boolean prefixCondition         = StringUtil.isBlank(channelPrefix)||"ALL".equalsIgnoreCase(channelPrefix)?true:channelName.startsWith(channelPrefix+"_BANK");
        boolean searchChannelCondition  = StringUtils.isBlank(searchChannelName)||"ALL".equalsIgnoreCase(searchChannelName)?true:channelName.equalsIgnoreCase(searchChannelName);
       // boolean searchOidCondition      = StringUtils.isBlank(searchOid)||"ALL".equalsIgnoreCase(oid)?true:oid.equalsIgnoreCase(searchOid);

        if(ALL.equals(tongJiType) && prefixCondition && searchChannelCondition  ){
            return true;
        }

        if( channelName.contains(tongJiType)    && prefixCondition && searchChannelCondition  ){
            return true;
        }
        return false;
    }



    /**
     * 根据map获取map包含的key,返回set
     */
    public static Set<String> getKeySetByMap(Map<String, String> map) {
        Set<String> keySet = new HashSet<String>();
        keySet.addAll(map.keySet());
        return keySet;
    }



    /**
     * 根据key的set返回key的list
     *
     * @param set
     * @return
     */
    public static List<String> getKeyListBySet(Set<String> set) {
        List<String> keyList = new ArrayList<String>();
        keyList.addAll(set);
        return keyList;
    }


    /**
     * 根据map返回key和value的list  //结果封装，到处用
     *  true为key,false为value
     */
    public static List<TongJi> getListByMapValue(Map<String,TongJi> map) {
        List<TongJi> list = Lists.newArrayList();
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            TongJi tongJi = map.get(key);
            tongJi.setChannelCName(getChannelCNameByChannelName(tongJi.getPageChannelName()));//通道完整名称，艾米森/微信
            tongJi.setChannelCoCName(HandlerUtil.getAllPayChannelAndCoCname().get(tongJi.getPageChannelName()));//服务商名称
            tongJi.setChannelCNameShort(tongJi.getChannelCName().replaceFirst(tongJi.getChannelCoCName()+" / ","")); //通道中文名称(短)  微信、/网银/建设银行
            tongJi.setChannelId(HandlerUtil.getChannelAndCoId().get(tongJi.getPageChannelName()));  //通道服务商id AIMISEN
            tongJi.initTongji();
            list.add(tongJi);
        }
        return list;
    }



    //计算全部成功率统计-总和
    //[请求成功/请求总数：20/30=20%], [响应成功/请求总数：20/30=10%],
    public Map<String, Long> getAllCglTotal(List<TongJi> tongJiPageList) {
        Map<String, Long> allCglTotal = Maps.newHashMap();
        long pageReqSuccessSumL=0; //请求成功
        long pageReqSumL=0; //请求总数
        long pageResSuccessSumL=0; //响应成功
        long pageResSuccessAmountL=0; //请求总数
        for (TongJi tongJi : tongJiPageList) {
            pageReqSuccessSumL+=Long.parseLong(tongJi.getPageReqSuccessSum());
            pageReqSumL+=Long.parseLong((tongJi.getPageReqSum()));
            pageResSuccessSumL+=Long.parseLong((tongJi.getPageResSuccessSum()));
            pageResSuccessAmountL+=Long.parseLong((tongJi.getPageResSuccessAmount()));
        }
        allCglTotal.put(pageReqSuccessSum,pageReqSuccessSumL);
        allCglTotal.put(pageReqSum,pageReqSumL);
        allCglTotal.put(pageResSuccessSum,pageResSuccessSumL);
        allCglTotal.put(pageResSuccessAmount,pageResSuccessAmountL);
        return allCglTotal;
    }


    //排序channle,成功率
    public ArrayList<SortedChannel> sortChannel(List<TongJi> tongJiList,Comparator comparators) {
        if(null==comparators)
            comparators = TongJi.Comparators.ChengGongLv;
        ArrayList<SortedChannel> sortedChannels = Lists.newArrayList();
        Collections.sort(tongJiList,comparators);
        for (int i = 0; i < tongJiList.size(); i++) {
            String channelName = tongJiList.get(i).getPageChannelName();
            String resSuccessDivReqSuccess = tongJiList.get(i).getResSuccessDivReqSuccess();
            String resSuccessAmount = tongJiList.get(i).getPageResSuccessAmount();
            String reqOid = tongJiList.get(i).getReqOid();
            String reqMemberID = tongJiList.get(i).getReqMemberID();
            String channelCName = HandlerUtil.getChannelCNameByChannelName(channelName);
            String pageReqSuccessSum = tongJiList.get(i).getPageReqSuccessSum();
            String pageResSuccessSum = tongJiList.get(i).getPageResSuccessSum();
            SortedChannel sortedChannel = new SortedChannel(i+1, channelName, channelCName, resSuccessDivReqSuccess,resSuccessAmount,reqOid,reqMemberID,pageReqSuccessSum,pageResSuccessSum);
            sortedChannels.add(sortedChannel);
        }
        return sortedChannels;
    }



    //排序channle,状态
    public ArrayList<SortedChannelStatus> sortChannelStatus(List<TongJi> tongJiList,Comparator comparators) {
        if(null==comparators) comparators = TongJi.Comparators.channelStatus;
        ArrayList<SortedChannelStatus> sortedChannels = Lists.newArrayList();
        Collections.sort(tongJiList,comparators);
        for (int i = 0; i < tongJiList.size(); i++) {
            String channelCoCName          = tongJiList.get(i).getChannelCoCName();
            String channelCNameShort       = tongJiList.get(i).getChannelCNameShort();
            Double reqSuccessDivReqSum     = Double.parseDouble(tongJiList.get(i).getReqSuccessDivReqSum());
            Double resSuccessDivReqSuccess = Double.parseDouble(tongJiList.get(i).getResSuccessDivReqSuccess());
            boolean reqWarning = ChannelStatusJob.reqWarning.compareTo(reqSuccessDivReqSum)==1?true:false;
            boolean resWarning = ChannelStatusJob.reqWarning.compareTo(resSuccessDivReqSuccess)==1?true:false;
            SortedChannelStatus sortedChannel =new SortedChannelStatus(channelCoCName, channelCNameShort, reqSuccessDivReqSum, resSuccessDivReqSuccess, reqWarning, resWarning) ;
            sortedChannels.add(sortedChannel);
        }
        return sortedChannels;
    }


    //获取异常通道数量
    public int  getWarningChannelCount(ArrayList<SortedChannelStatus> list){
        int i=0;
        for (int j = 0; j < list.size(); j++) {
            if(list.get(j).isReqWarning() ||list.get(j).isResWarning())i=i+1 ;
        }
        return i;
    }




    public  List<Map<String,Object>>  processNotifDbChannelSort(ArrayList<SortedChannel> resultList) {
        List<Map<String,Object>> calResult = Lists.newLinkedList();

        for (SortedChannel sortedChannel : resultList) {
            calResult.add(new HashMap<String,Object>(){{
                put("memberId",sortedChannel.getMemberId());
                put("channelBankName",sortedChannel.getChannelName());
                put("rate",getSuccessPayPermille(sortedChannel.getSuccessPayPercent()));
                put("successCount",Long.parseLong(sortedChannel.getResSuccessSum())); //blake(blake) 03-21 14:18:02 @tony 更新第三方支付通道排序的接口需要新增传入字段 successCount
            }});

        }
        return calResult;
    }


   public static long  getSuccessPayPermille(String percent){
        if(StringUtils.isNotBlank(percent)){
            try{
                return Math.round(Double.parseDouble(percent) * 10);
            }catch (Exception e){
                return 0;
            }
        }
        return 0;
    }

    //统计指定日期数据,3.1->查询范围为3.1/7:00 - 3.2/6:59
    public List<TongJi> getAllCglByDay(String date) {
        return tongJiMapper.getAllCglByDay( date.concat(seven),DateUtil.plusDays(date, dateFormater, 1).concat(six));
    }

    //批量插入
    public int insertAllCglByDayBatch(List<TongJi> tongJis,String tj_time_tmp){
        if(StringUtils.isNotBlank(tj_time_tmp)  && CollectionUtils.isNotEmpty(tongJis)){
            return tongJiMapper.insertAllCglByDayBatch(tongJis,tj_time_tmp);
        }
        return 0;
    }

    //批量删除
    public int delAllCglByDayBatch(String tj_time_stmp){
        if(StringUtils.isNotBlank(tj_time_stmp)){
            return tongJiMapper.delAllCglByDayBatch(tj_time_stmp);
        }
        return 0;
    }

    //统计日是否有数据
    public int  getCglCountbyDay(String tj_time_tmp){
        return tongJiMapper.getCglCountbyDay(tj_time_tmp);
    }



    //安日统计(日期格式yyyy-MM-dd)
    //确定2号统计1号数据，1号数据完结。3号有可能再次回调1号数据，导致流水表数据，与统计表数据不符 长款问题
    public void getAllCglByDay(Map<String,String> tj_status_by_day,String dataStr){
        String locker = UUID.randomUUID().toString();
        if(StringUtils.isBlank(dataStr)){
            dataStr=getNeedTjRiQi();//没有时间，统计系统时间
        }else if(ValidateUtil.valdateRiQi(dataStr,dateFormater)){
            try{
                dataStr = DateUtil.formatDate(dataStr, dateFormater);//转换标准时间2018-2-1 --> 2018-02-01
            }catch (Exception e){
                log.error("[按日统计,时间参数错误：{},日期格式应为：{}]",dataStr,dateFormater);
                return ;
            }
        }
        try{
            log.info("[本次启动后按日统计{}天，结果: {}]",tj_status_by_day.size(),JSON.toJSONString(tj_status_by_day));
            if(tj_status_by_day!=null && tj_status_by_day.containsKey(dataStr.concat(PayEumeration.TJ_BY_DAY)) && PayEumeration.TJ_STATUS_FINISH.equalsIgnoreCase(tj_status_by_day.get(dataStr.concat(PayEumeration.TJ_BY_DAY)))) return;
            TjStatus tjStatus = tjStatusService.getByTjTimeStmpAndName(dataStr, PayEumeration.TJ_BY_DAY); //日期，统计名称查询
            if(null==tjStatus){
                int i = tjStatusService.saveNewTjStatus(dataStr,locker);
                int countCglByDay = -1;
                if(i==1) countCglByDay = getCglCountbyDay(dataStr);  //该日期是否有数据
                if(countCglByDay==0){//没有保存过记录
                    tjStatus = tjStatusService.getByTjTimeStmpAndName(dataStr, PayEumeration.TJ_BY_DAY);
                    if(tjStatus.getTjLocker().equalsIgnoreCase(locker)){//本人锁定，本人增加
                        boolean  addFinish =  channelStatusJobService.insAllCglByDay(dataStr,tjStatus); //保存
                        if(!addFinish){
                            tjStatusService.delStatusByDayBatch(dataStr); //删除本条
                            delAllCglByDayBatch(dataStr); //删除今日统计数据
                        }else{
                            tj_status_by_day.put(dataStr.concat(PayEumeration.TJ_BY_DAY),PayEumeration.TJ_STATUS_FINISH); //缓存本日统计结果完成
                        }
                    }
                }else{
                    log.info("[本次启动后按日统计,数据已存在。日期：{}]",dataStr);
                }
            }else{
                if(!tjStatus.getTjStatus().equalsIgnoreCase(PayEumeration.TJ_STATUS_FINISH)){
                    Thread.sleep(2*60*1000); //停止2分钟
                    tjStatus = tjStatusService.getByTjTimeStmpAndName(dataStr, PayEumeration.TJ_BY_DAY);
                    if(null==tjStatus  || !tjStatus.getTjStatus().equalsIgnoreCase(PayEumeration.TJ_STATUS_FINISH)){
                        tjStatusService.delStatusByDayBatch(dataStr); //删除本条
                        delAllCglByDayBatch(dataStr); //删除今日统计数据
                    }
                }else{
                    tj_status_by_day.put(dataStr.concat(PayEumeration.TJ_BY_DAY),PayEumeration.TJ_STATUS_FINISH); //缓存本日统计结果完成
                }
            }

        }catch (Exception e){
            //log.error("[按日统计出错],日期：{},错误：{}",dataStr,e.getMessage(),e);
        }
    }



    //计算统计日期，6个(分解按开始日期->结束日期)
    public static Map<String,String>  getTjDateTime(String startDateTime,String endDateTime){
        HashMap<String, String> resutl = Maps.newLinkedHashMap();
        if(org.apache.commons.lang3.StringUtils.isNotBlank(startDateTime)  && org.apache.commons.lang3.StringUtils.isNotBlank(endDateTime) ){
            String startDateByBatchDay=null,endDateByBatchDay=null,startDateTimeByDayStart=null,endDateTimeByDayStart=null,startDateTimeByDayEnd=null,endDateTimeByDayEnd=null;
            //构造开始查询日期时间
            if(0==DateUtil.isSevenClock(startDateTime,DateUtil.dateTimeString)){ //整7点始查询
                startDateByBatchDay = DateUtil.formatDateInParttern(startDateTime, DateUtil.dateTimeString, DateUtil.dateString);
            }
            if(-1==DateUtil.isSevenClock(startDateTime,DateUtil.dateTimeString)){ //开始查询时间小于7点
                startDateTimeByDayStart = startDateTime;
                endDateTimeByDayStart = DateUtil.formatDateInParttern(startDateTimeByDayStart, DateUtil.dateTimeString, DateUtil.dateString).concat(DateUtil.sixFiftyNineClock);
                startDateByBatchDay = DateUtil.formatDateInParttern(endDateTimeByDayStart, DateUtil.dateTimeString, DateUtil.dateString);
            }
            if(1==DateUtil.isSevenClock(startDateTime,DateUtil.dateTimeString)){ //开始查询时间大于7点
                startDateTimeByDayStart = startDateTime;
                endDateTimeByDayStart = DateUtil.plusDays(startDateTimeByDayStart, DateUtil.dateTimeString, 1);//加1天
                endDateTimeByDayStart = DateUtil.formatDateInParttern(endDateTimeByDayStart, DateUtil.dateTimeString, DateUtil.dateString).concat(DateUtil.sixFiftyNineClock);
                startDateByBatchDay = DateUtil.formatDateInParttern(endDateTimeByDayStart, DateUtil.dateTimeString, DateUtil.dateString);
            }
            //================================================================================
            //构造结束查询日期时间
            if(0==DateUtil.isSixFiftyNineClock(endDateTime,DateUtil.dateTimeString)){ //结束查询时间：整点6.59.59
                endDateByBatchDay = DateUtil.minusDays(endDateTime, DateUtil.dateTimeString, 1);//减掉1天，因为6.59.59算前一天统计结果
                endDateByBatchDay = DateUtil.formatDateInParttern(endDateByBatchDay, DateUtil.dateTimeString, DateUtil.dateString);
            }
            if(-1==DateUtil.isSixFiftyNineClock(endDateTime,DateUtil.dateTimeString)){ //结束查询时间：小于6.59.59
                endDateByBatchDay = DateUtil.minusDays(endDateTime, DateUtil.dateTimeString, 2);//减掉2天
                endDateByBatchDay = DateUtil.formatDateInParttern(endDateByBatchDay, DateUtil.dateTimeString, DateUtil.dateString);
                startDateTimeByDayEnd = DateUtil.plusDays(endDateByBatchDay, DateUtil.dateString, 1);//加1天
                startDateTimeByDayEnd = DateUtil.formatDateInParttern(startDateTimeByDayEnd, DateUtil.dateString, DateUtil.dateString).concat(DateUtil.sevenClock);
                endDateTimeByDayEnd =endDateTime;
            }
            if(1==DateUtil.isSixFiftyNineClock(endDateTime,DateUtil.dateTimeString)){ //结束查询时间：大于6.59.59
                endDateByBatchDay = DateUtil.minusDays(endDateTime, DateUtil.dateTimeString, 1);//减掉1天，因为6.59.59算前一天统计结果
                endDateByBatchDay = DateUtil.formatDateInParttern(endDateByBatchDay, DateUtil.dateTimeString, DateUtil.dateString);

                startDateTimeByDayEnd = DateUtil.plusDays(endDateByBatchDay, DateUtil.dateString, 1);//加1天
                startDateTimeByDayEnd = DateUtil.formatDateInParttern(startDateTimeByDayEnd, DateUtil.dateString, DateUtil.dateString).concat(DateUtil.sevenClock);
                endDateTimeByDayEnd =   endDateTime;
            }
            if(DateUtil.betweenDateTime(startDateByBatchDay, endDateByBatchDay, DateUtil.dateString)<0){startDateByBatchDay=null;endDateByBatchDay=null;}

            String riQiFanWei = TongJiService.getRiQiFanWei();
            String todaysStartDateTime =  DateUtil.formatDateInParttern(riQiFanWei.split(" - ")[0], DateUtil.dateTimeString, DateUtil.dateString);

            if(DateUtil.compareDateTime(endDateByBatchDay, todaysStartDateTime, DateUtil.dateString)!=1){ //结束日期，统计表中无数据
                endDateByBatchDay = DateUtil.minusDays(todaysStartDateTime, "yyyy-MM-dd", 1);
                startDateTimeByDayEnd =  riQiFanWei.split(" - ")[0];
                endDateTimeByDayEnd =    riQiFanWei.split(" - ")[1];
            }

            resutl.put("startDateTimeByDayStart",startDateTimeByDayStart);
            resutl.put("endDateTimeByDayStart",endDateTimeByDayStart);
            resutl.put("startDateByBatchDay",startDateByBatchDay);
            resutl.put("endDateByBatchDay",endDateByBatchDay);
            resutl.put("startDateTimeByDayEnd",startDateTimeByDayEnd);
            resutl.put("endDateTimeByDayEnd",endDateTimeByDayEnd);
            return resutl;
        }
        return null;
    }


    //通知数据库排序通道-rest手动点击
    public  Map<String,List<TongJi>> notifDbChannelSortForRest(){
        TongJi tongJi = new TongJi();
        tongJi.setRows(Integer.MAX_VALUE);
        tongJi.setRiQiFanWei(DateUtil.printFromNowMinuteAgo(ChannelStatusJob.SECONDFORDBCHANNELSORT));
        Map<String,List<TongJi>> allCglFordbChannelSort = getAllCglFordbChannelSort(tongJi); //OID筛选
        return allCglFordbChannelSort;
    }


    //检查今日url正确性
    public Map<String,Map<String,Set<String>>> payApiUrl(){
        Map<String,Map<String,Set<String>>> result = Maps.newTreeMap();
        List<PayApiUrl> payApiUrls = tongJiMapper.payApiUrl();
        for (PayApiUrl payApiUrl : payApiUrls) {
            String oid = payApiUrl.getOid();
            if(!result.containsKey(oid)) result.put(oid,Maps.newTreeMap());
            Map<String,Set<String>> apiUrlOidMaps =result.get(oid);
            if(!apiUrlOidMaps.containsKey("web_url")) apiUrlOidMaps.put("web_url", new TreeSet<>());
            if(!apiUrlOidMaps.containsKey("jump_url"))apiUrlOidMaps.put("jump_url",new TreeSet<>());
            if(!apiUrlOidMaps.containsKey("notify_url"))apiUrlOidMaps.put("notify_url", new TreeSet<>());
            apiUrlOidMaps.get("web_url").add(payApiUrl.getWeb_url());
            apiUrlOidMaps.get("jump_url").add(payApiUrl.getJump_url());
            apiUrlOidMaps.get("notify_url").add(payApiUrl.getNotify_url());
        }
        return result;
    }

}
