package dc.pay.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.PropertyPreFilter;
import dc.pay.entity.pay.ReqPayList;
import dc.pay.entity.pay.ResPayList;
import dc.pay.entity.tj.TongJi;
import dc.pay.mapper.tj.TongJiMapper;
import dc.pay.service.tj.TongJiService;
import dc.pay.service.tj.TongJiWebService;
import dc.pay.utils.DateUtil;
import dc.pay.utils.ipUtil.qqwry.qqwry3.IpHelperCZ;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
//@CacheConfig(cacheManager="redisCacheManager")
//@CacheConfig(cacheManager="ehCacheCacheManager")
public class CacheService {
    private static final Logger log =  LoggerFactory.getLogger(CacheService.class);
    @Autowired
    private TongJiService tongJiService;
    @Autowired
    private TongJiWebService tongJiWebService;
    @Autowired
    TongJiMapper tongJiMapper;


    @Autowired
    RedisTemplate redisTemplate;
    private HashOperations<String, String, Long> hashOps;


    @PostConstruct
    void init(){
        hashOps = redisTemplate.opsForHash();
    }


    //按地区统计属性
    long cacheTjByLocationTimeOut = 30*24*60*60;
    String [] propertys  = new String[]{"reqSum","reqError","reqSuccess","reqSuccessAmount","resSum","resError","resSuccess","resSuccessAmount"};



    //统计缓存key
    public static PropertyPreFilter cglTjCacheFilter = new PropertyPreFilter () {
        @Override
        public boolean apply(JSONSerializer serializer, Object object, String name) {
           if(name.equalsIgnoreCase("riQiFanWei") || name.equalsIgnoreCase("oid"))
               return true;
            return false;
        }
    };


    //@Cacheable(value="cglTjCache",key="T(com.alibaba.fastjson.JSON).toJSONString(#tongJi,T(dc.pay.service.cache.CacheService).cglTjCacheFilter).concat(#startDateTime).concat(':').concat(#endDateTime)")
    @Cacheable(value="cglTjCache",key="T(dc.pay.utils.HandlerUtil).getMD5UpperCase((T(com.alibaba.fastjson.JSON).toJSONString(#tongJi,T(dc.pay.service.cache.CacheService).cglTjCacheFilter).concat(#startDateTime).concat(':').concat(#endDateTime)))")
    public List<TongJi> getAllCgl(TongJi tongJi, String startDateTime, String endDateTime) {
        if(StringUtils.isBlank(startDateTime) || StringUtils.isBlank(endDateTime)) return null;
        return tongJiMapper.getAllCgl(tongJi,startDateTime,endDateTime);
    }



    //查询按日统计数据
    //@Cacheable(cacheManager ="ehCacheCacheManager",value="dateByBatchDayCache",key="T(com.alibaba.fastjson.JSON).toJSONString(#tongJi,T(dc.pay.service.cache.CacheService).cglTjCacheFilter).concat(#startDateTime).concat(':').concat(#endDateTime)")
   // @Cacheable(value="dateByBatchDayCache",key="T(com.alibaba.fastjson.JSON).toJSONString(#tongJi,T(dc.pay.service.cache.CacheService).cglTjCacheFilter).concat(#startDateTime).concat(':').concat(#endDateTime)")
    @Cacheable(value="dateByBatchDayCache",key="T(dc.pay.utils.HandlerUtil).getMD5UpperCase((T(com.alibaba.fastjson.JSON).toJSONString(#tongJi,T(dc.pay.service.cache.CacheService).cglTjCacheFilter).concat(#startDateTime).concat(':').concat(#endDateTime)))")
    public List<TongJi> getAllCglInTjByDay(TongJi tongJi, String startDateTime, String endDateTime) {
        if(StringUtils.isBlank(startDateTime) || StringUtils.isBlank(endDateTime)) return null;
        return tongJiMapper.getAllCglInTjByDay(tongJi,startDateTime,endDateTime);
    }



    /**
     * 通过tongji 的日期范围，查找日期范围内oid个数
     */
    //@Cacheable(value="oidCountCache",key="'oidCountCache'+#tongJi.riQiFanWei.concat(#tongJi.oid)")
    @Cacheable(value="oidCount",key="T(dc.pay.utils.HandlerUtil).getMD5UpperCase('oidCountCache'.concat(#tongJi.riQiFanWei).concat(#tongJi.oid) )")
    public  int  getOidCount(TongJi tongJi){
        tongJiService.setBlankRiqiFanWei(tongJi);
        String startDateTime=tongJi.getRiQiFanWei().split(" - ")[0];
        String endDateTime=tongJi.getRiQiFanWei().split(" - ")[1];
        return  tongJiMapper.getOidCount(startDateTime,endDateTime);
    }



    //@Cacheable(value="cglTjCache",key="T(com.alibaba.fastjson.JSON).toJSONString(#tongJi).concat(#startDateTime).concat(':').concat(#endDateTime)")
    @Cacheable(value="cglTjCache",key="T(dc.pay.utils.HandlerUtil).getMD5UpperCase((T(com.alibaba.fastjson.JSON).toJSONString(#tongJi).concat(#startDateTime).concat(':').concat(#endDateTime)))")
    public List<TongJi> getAllCglFordbChannelSort(TongJi tongJi, String startDateTime, String endDateTime) {
        if(StringUtils.isBlank(startDateTime) || StringUtils.isBlank(endDateTime)) return null;
        return tongJiMapper.getAllCglFordbChannelSort(tongJi,startDateTime,endDateTime);
    }



    //按地区统计缓存-请求支付(取消需求)
   // @Async("payAsync")
    public void cacheTjRequestPayByLocation(ReqPayList reqPayList){
        try{
            if(null!=reqPayList && null!=reqPayList.getReqPayInfo() && org.apache.commons.lang3.StringUtils.isNotBlank(reqPayList.getResult()) && !reqPayList.getReqPayInfo().getAPI_ORDER_ID().startsWith("T")){
                String key ="tjByLocation".concat(":").concat(DateUtil.formatDateTimeStrByParam(DateUtil.dateString));
                redisTemplate.expire(key, cacheTjByLocationTimeOut, TimeUnit.SECONDS);//过期时间
                String oid = reqPayList.getReqPayInfo().getAPI_OID();
                String location = IpHelperCZ.getAddresByip( reqPayList.getReqPayInfo().getAPI_Client_IP());
                String hashKey =oid.concat(":").concat(location).concat(":");
                hashOps.increment(key,hashKey.concat(propertys[0]),1);//reqSum+1
                if(reqPayList.getResult().equalsIgnoreCase("ERROR")){
                    hashOps.increment(key,hashKey.concat(propertys[1]),1);//reqError+1
                }else{
                    hashOps.increment(key,hashKey.concat(propertys[2]),1);//reqSuccess+1
                    hashOps.increment(key,hashKey.concat(propertys[3]),Long.parseLong(reqPayList.getReqPayInfo().getAPI_AMOUNT()));//reqSuccessAmount+
                }
            }
        }catch (Exception e){
            log.error("[按地区统计缓存-请求支付]出错：{},reqPay:{}",e.getMessage(), JSON.toJSONString(reqPayList),e);
        }
    }


    //按地区统计缓存-响应支付(取消需求)
   // @Async("payAsync")
    public void cacheTjResponsePayByLocation(ResPayList respay){
        try{
            if(null!=respay && null!=respay.getReqPayInfo() && org.apache.commons.lang3.StringUtils.isNotBlank(respay.getResult()) && !respay.getReqPayInfo().getAPI_ORDER_ID().startsWith("T")){
                String key ="tjByLocation".concat(":").concat(DateUtil.formatDateTimeStrByParam(DateUtil.dateString));
                redisTemplate.expire(key, cacheTjByLocationTimeOut, TimeUnit.SECONDS);//过期时间
                String oid = respay.getReqPayInfo().getAPI_OID();
                String location = IpHelperCZ.getAddresByip( respay.getReqPayInfo().getAPI_Client_IP());
                String hashKey =oid.concat(":").concat(location).concat(":");
                hashOps.increment(key,hashKey.concat(propertys[4]),1);//resSum+1
                if(respay.getResult().equalsIgnoreCase("ERROR")){
                    hashOps.increment(key,hashKey.concat(propertys[5]),1);//resError+1
                }else{
                    hashOps.increment(key,hashKey.concat(propertys[6]),1);//resSuccess+1
                    hashOps.increment(key,hashKey.concat(propertys[7]),Long.parseLong(respay.getReqPayInfo().getAPI_AMOUNT()));//reqSuccessAmount+
                }
            }
        }catch (Exception e){
            log.error("[按地区统计缓存-响应支付]出错：{},reqPay:{}",e.getMessage(), JSON.toJSONString(respay),e);
        }
    }




}
