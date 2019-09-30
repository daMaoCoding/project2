package dc.pay.admin.modular.system.controller;

import dc.pay.admin.common.annotion.Permission;
import dc.pay.base.BaseController;
import dc.pay.entity.tj.TongJi;
import dc.pay.entity.po.SortedChannel;
import dc.pay.scheduleJobs.ChannelStatusJob;
import dc.pay.service.tj.TongJiService;
import dc.pay.utils.DateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * ************************
 * @author tony 3556239829
 */
@RestController
@RequestMapping("/tongji")
public class TongJiController extends BaseController{
    private static final Logger log =  LoggerFactory.getLogger(TongJiController.class);

    @Autowired
    private TongJiService tongJiService;

    @Autowired
    private ChannelStatusJob channelStatusJob;


    /**
     * 成功率(全部通道,微信,支付宝,QQ,百度,京东,网银)
     * @param tongJi
     * @return
     */
    @Permission
    @RequestMapping(value = {"/index/",""},method = {RequestMethod.POST, RequestMethod.GET})
    public ModelAndView cgl(TongJi tongJi) {
        if(StringUtils.isBlank(tongJi.getOrderBy())){
            tongJi.setOrderBy("zfcgl");
            tongJi.setSort("desc");
        }
        ModelAndView cglModelAndView = tongJiService.getCglModelAndView(tongJi);//by page
        Map<String,Long>  allCglTotal = tongJiService.getAllCglTotal(tongJi); //by total
        cglModelAndView.getModel().put("allCglTotal",allCglTotal);//by total
        cglModelAndView.getModel().put("fmk", useStaticPacker("dc.pay.scheduleJobs.ChannelStatusJob"));//freemark静态枚举变量等
       // cglModelAndView.getModel().put("reqWarning", ChannelStatusJob.reqWarning);//颜色红绿
       // cglModelAndView.getModel().put("resWarning",ChannelStatusJob.resWarning);//颜色红绿


        return cglModelAndView;
    }


    /**
     * 通道成功率(图表)-单个通道
     * @param channelName
     * @return
     */
    @ResponseBody
    @RequestMapping(value = {"/cgl/{channelName}/{riQiFanWei}/{oid}",""},method = {RequestMethod.POST, RequestMethod.GET},produces = {"application/json"})
    public List tdcgl(@PathVariable(value = "channelName",required = true) String channelName, @PathVariable(value = "riQiFanWei",required = true) String riQiFanWei, @PathVariable(value = "oid",required = true) String oid) {
           TongJi tongJi = new TongJi();
           tongJi.setRiQiFanWei(riQiFanWei);
           tongJi.setChannelName(channelName);
           tongJi.setOid(oid);
           List<TongJi> tongJiList = tongJiService.getAllCglByChannel(tongJi);
           log.info("成功率统计-图表：通道："+channelName+",日期："+riQiFanWei);
           //'请求总数','请求成功','支付成功',"支付金额"
           ArrayList<Object> resultList = tongJiService.processEcharByChannelName(tongJiList);
          return resultList;
    }



    /**
     * 通道成功率(图表)-全部通道
     * @return
     */
    @ResponseBody
    @RequestMapping(value = {"/cgl/allChannel/{riQiFanWei}/{oid}",""},method = {RequestMethod.POST, RequestMethod.GET},produces = {"application/json"})
    public List qbtdcgl(@PathVariable(value = "riQiFanWei",required = true) String riQiFanWei, @PathVariable(value = "oid",required = true) String oid) {
           TongJi tongJi = new TongJi();
           tongJi.setRiQiFanWei(riQiFanWei);
           tongJi.setOid(oid);
           List<TongJi> tongJiList = tongJiService.getAllCglByAllChannel(tongJi);
           log.info("成功率统计-图表-全部通道：日期："+riQiFanWei);
           //'请求总数','请求成功','支付成功',"支付金额"
           ArrayList<Object> resultList = tongJiService.processEcharByChannelName(tongJiList);
          return resultList;
    }



    /**
     * 通道成功率(图表)-全部通道(增加支付商筛选）)
     * @return
     */
    @ResponseBody
    @RequestMapping(value = {"/cgl/allChannel/{tongJiType}/{riQiFanWei}/{oid}",""},method = {RequestMethod.POST, RequestMethod.GET},produces = {"application/json"})
    public List qbtdcgl(@PathVariable(value = "tongJiType",required = true) String tongJiType,@PathVariable(value = "riQiFanWei",required = true) String riQiFanWei, @PathVariable(value = "oid",required = true) String oid) {
           TongJi tongJi = new TongJi();
           tongJi.setRiQiFanWei(riQiFanWei);
           tongJi.setOid(oid);
           tongJi.setTongJiType(tongJiType);
           List<TongJi> tongJiList = tongJiService.getAllCglByAllChannel(tongJi);
           log.info("成功率统计-图表-全部通道：日期："+riQiFanWei);
           //'请求总数','请求成功','支付成功',"支付金额"
           ArrayList<Object> resultList = tongJiService.processEcharByChannelName(tongJiList);
          return resultList;
    }




    /**
     * 5分钟内通道成功率排序(只统计已有的)
     * @return
     */
    @ResponseBody
    @RequestMapping(value = {"/sortChannel/{mm}","/sortChannel","/sortChannel/"},method = {RequestMethod.POST, RequestMethod.GET},produces = {"application/json"})
    public List sortChannel(@PathVariable(value = "mm",required = false) Integer mm) {
        TongJi tongJi = new TongJi();
        tongJi.setRiQiFanWei(DateUtil.printFromNowMinuteAgo(null==mm?5*60:mm));
        List<TongJi> tongJiList = tongJiService.getAllCgl(tongJi);
        ArrayList<SortedChannel> resultList = tongJiService.sortChannel(tongJiList,null);
        return resultList;
    }




    /**
     * 手动-按日统计-日期
     */
    @ResponseBody
    @RequestMapping(value = {"/tj_by_day/insert/{dataStr}"},method = {RequestMethod.GET})
    public Object  manualTjByDay (@PathVariable(value = "dataStr",required = false) String dataStr) {
       if( ValidateUtil.valdateRiQi(dataStr, tongJiService.dateFormater)){
           DateTimeFormatter format = DateTimeFormat.forPattern(tongJiService.dateFormater);
           if(DateTime.parse(dataStr, format).isBefore(DateTime.parse(tongJiService.getNeedTjRiQi(), format))){
               tongJiService.getAllCglByDay(channelStatusJob.tj_status_by_day,dataStr);
               log.info("[手动-按日统计-日期]： {},缓存结果：{}",dataStr,channelStatusJob.tj_status_by_day);
               return channelStatusJob.tj_status_by_day;
           }else {
               return "日期时间不能超过(应小于)："+tongJiService.getNeedTjRiQi();
           }
       }
        return "日期格式不正确：yyyy-MM-dd";
    }


}