package dc.pay.service.tj;


import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.entity.tj.BillVerify;
import dc.pay.entity.tj.TongJi;
import dc.pay.entity.po.SortedChannel;
import dc.pay.mapper.tj.TongJiMapper;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * ************************
 *
 * @author tony 3556239829
 */

@Service
public class TongJiWebService {
    private static final Logger log =  LoggerFactory.getLogger(TongJiWebService.class);

    @Autowired
    TongJiMapper tongJiMapper;

    @Autowired
    private TongJiService tongJiService;


    //统计明细，列表
    public Map<String,Object> processGetCgl(Map<String, Object> result) {
        Map<String,Object> vueResutl = Maps.newTreeMap();
        vueResutl.put("page",result.get("page"));
        vueResutl.put("rows",result.get("rows"));
        vueResutl.put("total",((PageInfo<TongJi>)result.get("pageInfo")).getTotal());
        vueResutl.put("allCglTotal",result.get("allCglTotal"));
        List<TongJi> tongjis =  ((PageInfo<TongJi>)result.get("pageInfo")).getList();
        List<Object> vueList = Lists.newArrayList();
        for (TongJi tongJi : tongjis) {
            TreeMap<String, String>  mpaObj= Maps.newTreeMap();
        //    System.out.println(tongJi.getChannelCoCName());
            mpaObj.put("tongdaofuwushang", tongJi.getChannelCoCName());
            mpaObj.put("tongdaomingcheng", tongJi.getChannelCNameShort());
            mpaObj.put("pageChannelName",tongJi.getPageChannelName());
            mpaObj.put("liantongchenggonglv",tongJi.getReqSuccessDivReqSum()); //连通成功率
            mpaObj.put("zhifuchenggonglv",tongJi.getResSuccessDivReqSuccess()); //支付成功率
            mpaObj.put("chenggongcishu",tongJi.getPageResSuccessSum());         //成功次数
            mpaObj.put("chenggongrukuanshu",tongJi.getPageResSuccessAmount()); //成功入款数
            vueList.add(mpaObj);
        }
        vueResutl.put("list",vueList);
        return vueResutl;
    }


    //第三方平台通道入款综合统计（业主+第三方公司名-->请求数，请求成功数，请求率，响应成功数，响应成功率，入款金额）
    public List<Object> getCglByCo(Map<String, Object> result,String orderBy,String sort) {
        List<Object> vueResutl = Lists.newArrayList();
        List<TongJi> pageInfo =Lists.newArrayList();
        if(MapUtils.isNotEmpty(result) && result.containsKey("pageInfo")) pageInfo = ((PageInfo<TongJi>) result.get("pageInfo")).getList();
        if(CollectionUtils.isNotEmpty(pageInfo) ){
            Map<String, List<TongJi>> byCoMap =pageInfo.stream().filter((t)-> StringUtils.isNotBlank(t.getChannelCoCName())&&StringUtils.isNotBlank(t.getChannelCNameShort())).collect(Collectors.groupingBy(TongJi::getChannelCoCName, Collectors.toList()));
            byCoMap.forEach((k,v)->{
                Map<String,Object> mergeByCoMap  = doMergeMap(k,v);
                vueResutl.add(mergeByCoMap);
            });
        }
        return vueResutl;
    }



    //计算第三方公司下不同类型支付统计合计
    public  Map<String,Object>  doMergeMap(String k,List<TongJi> v){
        Map<String,Object> result = Maps.newHashMap();
        if(StringUtils.isNotBlank(k) && CollectionUtils.isNotEmpty(v)){
            result.put("name",k);
            String reqSum = v.stream().map(TongJi::getPageReqSum).reduce(HandlerUtil::plusLong).get();
            String reqSuccess = v.stream().map(TongJi::getPageReqSuccessSum).reduce(HandlerUtil::plusLong).get();
            result.put("reqSum",reqSum);
            result.put("reqSuccess",reqSuccess);
            result.put("reqSuccessDivReqsum",HandlerUtil.divLong(reqSuccess,reqSum).toString());
            String getPageResSuccessSum = v.stream().map(TongJi::getPageResSuccessSum).reduce(HandlerUtil::plusLong).get();
            result.put("resSuccessDivRessum",HandlerUtil.divLong(getPageResSuccessSum,reqSuccess));
            result.put("resSum",getPageResSuccessSum);
            result.put("resSuccessAmount",v.stream().map(TongJi::getPageResSuccessAmount).reduce(HandlerUtil::plusLong).get());
            ArrayList<Object> resultInsdeList = Lists.newArrayList();
            v.forEach(x->{
                Map<String,Object> resultInsde = Maps.newHashMap();
                resultInsde.put("name",x.getChannelCNameShort());
                resultInsde.put("reqSum",x.getPageReqSum());
                resultInsde.put("reqSuccess",x.getPageReqSuccessSum());
                resultInsde.put("reqSuccessDivReqsum",x.getReqSuccessDivReqSum());
                resultInsde.put("resSum",x.getPageResSum());
                resultInsde.put("resSuccessDivRessum",x.getResSuccessDivReqSuccess());
                resultInsde.put("resSuccessAmount",x.getPageResSuccessAmount());
                resultInsdeList.add(resultInsde);
            });
            result.put("list",resultInsdeList);
        }
        return  result;
    }




    //统计图表：汇总统计：
    public Map<String,Object> processGetCgl_hztj(Map<String, Object> result, TongJi tongJi) {
        int oidCountR = 1;
        if("ALL".equalsIgnoreCase(tongJi.getOid())){
            oidCountR = tongJiService.getOidCount(tongJi);
        }
        Map<String,Object> vueResutl = Maps.newTreeMap();
        vueResutl.put("allCglTotal",result.get("allCglTotal"));
        vueResutl.put("oidCount",oidCountR);
        return vueResutl;
    }


    //统计图表：通道成功率最高的20个通道,支付额最高的20个通道
    public Map<String,Object> getCgl_cgvTop(ArrayList<SortedChannel> sortedChannelList) {
        Map<String,Object> vueResutl = Maps.newHashMap();
        vueResutl.put("sortedChannelList",sortedChannelList);
        vueResutl.put("totalNumber",sortedChannelList.size());
        return vueResutl;
    }


    //处理支付提供商名称
    public Map<String,String> processGetPayCo(Map<String, Object> result) {
        Map<String,String> vuePayCoResutl = Maps.newHashMap();
        for (TongJi tongJi : ((PageInfo<TongJi>)result.get("pageInfo")).getList()) {
            if(tongJi!=null && StringUtils.isNotBlank(tongJi.getChannelId())){
                vuePayCoResutl.put(tongJi.getChannelId(),tongJi.getChannelCoCName());
            }
        }
       return vuePayCoResutl;
    }



    @Async("paySimpleAsync")
    public Future<Map<String, Object>> getTodayCgl(TongJi tongJiSource){
        TongJi tongJi = new TongJi();
        BeanUtils.copyProperties(tongJiSource,tongJi);
        Map<String, Object> today      = tongJiService.getCglModelAndView(tongJi).getModel();// 今日
        Map<String, Object> todayMap = processGetCgl_hztj(today, tongJi);
        return new AsyncResult<Map<String, Object>>(todayMap);
    }


    @Async("paySimpleAsync")
    public Future<Map<String, Object>> getYesterdayCgl(TongJi tongJiSource) {
        TongJi tongJi = new TongJi();
        BeanUtils.copyProperties(tongJiSource,tongJi);
        tongJi.setRiQiFanWei(tongJiService.getRiQiFanWei(1));
        Map<String, Object> yesterday  = tongJiService.getCglModelAndView(tongJi).getModel();// 昨日
        Map<String, Object> yesterdayMap = processGetCgl_hztj(yesterday, tongJi);
        return new AsyncResult<Map<String, Object>>(yesterdayMap);
    }


    @Async("paySimpleAsync")
    public Future<Map<String, Object>> getSevendayCgl(TongJi tongJiSource) {
        TongJi tongJi = new TongJi();
        BeanUtils.copyProperties(tongJiSource,tongJi);
        tongJi.setRiQiFanWei(tongJiService.getRiQiFanWei(7));
        Map<String, Object> sevenday   = tongJiService.getCglModelAndView(tongJi).getModel();// 近7天
        Map<String, Object> sevendayMap = processGetCgl_hztj(sevenday, tongJi);
        return new AsyncResult<Map<String, Object>>(sevendayMap);
    }


    @Async("paySimpleAsync")
    public Future<Map<String, Object>> getThirtydayCgl(TongJi tongJiSource) {
        TongJi tongJi = new TongJi();
        BeanUtils.copyProperties(tongJiSource,tongJi);
        tongJi.setRiQiFanWei(tongJiService.getRiQiFanWei(30));
        Map<String, Object> thirtyday  = tongJiService.getCglModelAndView(tongJi).getModel();// 近30天
        Map<String, Object> thirtydayMap = processGetCgl_hztj(thirtyday, tongJi);
        return new AsyncResult<Map<String, Object>>(thirtydayMap);
    }




    //核对统计数据，7日内
    public List<BillVerify> billVerify(TongJi tongJi){
        String startDateTime=tongJi.getRiQiFanWei().split(" - ")[0];
        String endDateTime=tongJi.getRiQiFanWei().split(" - ")[1];
        int secondes = DateUtil.betweenDateTime(startDateTime, endDateTime, DateUtil.dateTimeString);
        if(-1!=secondes ){  //  || secondes<=604799
            return tongJiMapper.billVerify(startDateTime,endDateTime);
        }
        return Lists.newArrayList();
    }


}
