package dc.pay.controller.tj;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.entity.tj.BillVerify;
import dc.pay.entity.tj.TongJi;
import dc.pay.entity.po.SortedChannel;
import dc.pay.scheduleJobs.ChannelStatusJob;
import dc.pay.scheduleJobs.RunTimeInfoJob;
import dc.pay.utils.DateUtil;
import dc.pay.service.tj.TongJiService;
import dc.pay.service.tj.TongJiWebService;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.Future;

/**
 * ************************
 * @author tony 3556239829
 */
@RestController
@RequestMapping("/web/tongji")
public class WebTongJiController {
    private static final Logger logger =  LoggerFactory.getLogger(WebTongJiController.class);

    @Autowired
    private TongJiService tongJiService;

    @Autowired
    private TongJiWebService tongJiWebService;

    @Autowired
    private RunTimeInfoJob runTimeInfoJob;


    //下拉列表，业主->OID
    //@Cacheable(cacheNames="allOids",key = "#orderId") //caches[0].name  {reqpayinfo}
    @RequestMapping(value = {"/getAllOid/*",""},method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public Map<String,String> getAllOid(){
        return  HandlerUtil.getAllOid();
    }

    //下拉列表， 统计类型
    @RequestMapping(value = {"/getAllPayType/*",""},method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public Map<String,String> getAllPayType(){
        return  HandlerUtil.getAllPayType();
    }

    //下拉列表，第三方公司名称（可以传参数oid）
    @RequestMapping(value = {"/getAllPayCo/*",""},method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public Map<String,String> getAllPayCo(@RequestBody(required = false) TongJi tongJi){
        logger.info("[下拉列表，第三方公司名称]:{} ",JSON.toJSONString(tongJi));
        if(null==tongJi) return HandlerUtil.getAllPayCo();
        tongJi.setRows(Integer.MAX_VALUE);
        Map<String, Object> result  = tongJiService.getCglModelAndView(tongJi).getModel();
        return tongJiWebService.processGetPayCo(result);
    }


    //统计明细，列表
    @RequestMapping(value = {"/getCgl/*",""},method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getCgl(@RequestBody TongJi tongJi){
        logger.info("[/getCgl/*统计明细，列表]:", JSON.toJSONString(tongJi));
        Map<String, Object> result  = tongJiService.getCglModelAndView(tongJi).getModel();//by page
        Map<String,Long>  allCglTotal = tongJiService.getAllCglTotal(tongJi); //by total
        result.put("allCglTotal",allCglTotal);//by total
        return tongJiWebService.processGetCgl(result);
    }



    //第三方平台通道入款综合统计（业主+第三方公司名-->请求数，请求成功数，请求率，响应成功数，响应成功率，入款金额）
    @RequestMapping(value = {"/getCglByCo/*",""},method = {RequestMethod.POST})
    @ResponseBody
    public List<Object> getCglByCo(@RequestBody TongJi tongJi){
        logger.info("[/getCglByCo/*第三方平台通道入款综合统计]:", JSON.toJSONString(tongJi));
        tongJi.setRows(Integer.MAX_VALUE);
        String orderBy = tongJi.getOrderBy();
        String sort = tongJi.getSort();
        tongJi.setOrderBy(null);
        tongJi.setSort(null);
        Map<String, Object> result  = tongJiService.getCglModelAndView(tongJi).getModel();//get all
        return tongJiWebService.getCglByCo(result,orderBy,sort);
    }




    //统计图表：汇总统计：（暂无用）
    @RequestMapping(value = {"/getCgl-hztj/*",""},method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getCgl_hztj(@RequestBody TongJi tongJi){
        tongJi.setRows(Integer.MAX_VALUE);
        Map<String, Object> result  = tongJiService.getCglModelAndView(tongJi).getModel();
        return tongJiWebService.processGetCgl_hztj(result,tongJi);
    }

    //统计图表：汇总统计：
    @RequestMapping(value = {"/getCgl-hztj-bydays/*",""},method = {RequestMethod.POST})
    @ResponseBody
    public Map<String,  Map<String, Object>>  getCgl_hztj_bydays(@RequestBody TongJi tongJi){
        tongJi.setRows(Integer.MAX_VALUE);
        TreeMap<String,  Map<String, Object>> payTypeMaps = Maps.newTreeMap();
        Future<Map<String, Object>> todayCgl = tongJiWebService.getTodayCgl(tongJi);
        Future<Map<String, Object>> yesterdayCgl = tongJiWebService.getYesterdayCgl(tongJi);
        Future<Map<String, Object>> sevendayCgl = tongJiWebService.getSevendayCgl(tongJi);
        Future<Map<String, Object>> thirtydayCgl = tongJiWebService.getThirtydayCgl(tongJi);
        while(true) {
            if(todayCgl.isDone() && yesterdayCgl.isDone()  && sevendayCgl.isDone() && thirtydayCgl.isDone() ) {
                try {
                    payTypeMaps.put("todayMap",todayCgl.get());
                    payTypeMaps.put("yesterdayMap",yesterdayCgl.get());
                    payTypeMaps.put("sevendayMap",sevendayCgl.get());
                    payTypeMaps.put("thirtydayMap",thirtydayCgl.get());
                } catch (Exception e) {
                    logger.error("[统计图表：汇总统计：/getCgl-hztj-bydays/]:tongji:{},出错:{}",JSON.toJSONString(tongJi),e.getMessage(),e);
                }finally {
                    break;
                }
            }
        }
        return payTypeMaps;
    }





    //统计图表：支付成功率最高前20个通道：
    @RequestMapping(value = {"/getCgl-cgvTop/*",""},method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getCgl_cgvTop(@RequestBody TongJi tongJi){
        tongJi.setRows(Integer.MAX_VALUE);
        List<TongJi> tongJiList = tongJiService.getAllCgl(tongJi);
        ArrayList<SortedChannel> resultList = tongJiService.sortChannel(tongJiList,null);
        return tongJiWebService.getCgl_cgvTop(resultList); //top20
    }

    //统计图表：支付成功额最高前20个通道：
    @RequestMapping(value = {"/getCgl-cgeTop/*",""},method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> getCgl_cgeTop(@RequestBody TongJi tongJi){
        tongJi.setRows(Integer.MAX_VALUE);
        List<TongJi> tongJiList = tongJiService.getAllCgl(tongJi);
        ArrayList<SortedChannel> resultList = tongJiService.sortChannel(tongJiList,TongJi.Comparators.RuJuanJinEr);
        return tongJiWebService.getCgl_cgvTop(resultList); //top20
    }




    //通道报警
    @RequestMapping(value = {"/channelStatusWarning/*",""},method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public Map<String, Object> channelStatusWarning(){
        return ChannelStatusJob.channelStatusWarning;
    }


    //通知数据库排序通道-For Rest(节奏：rest->pay/rest->db)
    @RequestMapping(value = {"/payOwnerConfig/ownerChannelRateUpdate/*",""},method = {RequestMethod.POST},produces = {"application/json"})
    @ResponseBody
    public Map<String, Object> ownerChannelRateUpdate(@RequestBody(required = false) Map<String,String> resParams){
        Map<String,Object> result = Maps.newHashMap();
        DateTime now = new DateTime();
        long calTimeStart = now.minusSeconds(ChannelStatusJob.SECONDFORDBCHANNELSORT).getMillis(); //本次统计结束时间
        long calTimeEnd   =now.getMillis();  //本次统计开始时间
        result.put("calTime",System.currentTimeMillis());
        result.put("calTimeStart",calTimeStart);
        result.put("calTimeEnd",calTimeEnd);
        if(resParams.containsKey("oid")){
            String oid = resParams.get("oid");
            Map<String,List<TongJi>> allCglFordbChannelSort = tongJiService.notifDbChannelSortForRest();
                List<TongJi> tongJis = allCglFordbChannelSort.containsKey(oid)?allCglFordbChannelSort.get(oid):Lists.newArrayList();
                ArrayList<SortedChannel> resultList = tongJiService.sortChannel(tongJis,null);
                List<Map<String, Object>> notifDbChannelSortList = tongJiService.processNotifDbChannelSort(resultList);//排序内容
                result.put("calResult",notifDbChannelSortList);
            logger.debug("[手动-通知数据库排序通道],时间={}，oid={}， JsonData={}",now,oid,JSON.toJSONString(result));
        }else{
            result.put("calResult",Lists.newArrayList());
        }
        return result;
    }



    //核对成功入款笔数和总金额,7天内
    @RequestMapping(value = {"/billVerify/*",""},method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public Map<String, Object> billVerify(@RequestBody TongJi tongJi){
        logger.info("[/billVerify/核对成功入款笔数和总金额]:", JSON.toJSONString(tongJi));
        String dateTimeStr = DateUtil.curDateTimeStr();
        long startTime=System.currentTimeMillis()/1000;   //获取开始时间
        if(null==tongJi || StringUtils.isBlank(tongJi.getRiQiFanWei())  || !tongJi.getRiQiFanWei().contains("-")){
            tongJiService.setBlankRiqiFanWei(tongJi);
        }
        List<BillVerify> billVerifies = tongJiWebService.billVerify(tongJi);
        Map<String, Object> result  = Maps.newHashMap();
        result.put("riQiFanWei",tongJi.getRiQiFanWei());
        result.put("list",billVerifies);
        long endTime=System.currentTimeMillis()/1000; //获取结束时间
        runTimeInfoJob.billVerify(dateTimeStr,endTime-startTime,result);//保存redis
        return result;
    }


}