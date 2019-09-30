package dc.pay.base.processor;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.ResListI;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.business.ResponsePayResult;
import dc.pay.config.PayProps;
import dc.pay.dao.daifu.RequestDaiFuDao;
import dc.pay.dao.pay.RequestPayDao;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.service.cache.CacheService;
import dc.pay.service.daifu.ResDaiFuListService;
import dc.pay.service.pay.ResPayListService;
import dc.pay.service.resDb.PayJmsSender;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Slf4j
public abstract class PayResService<T> {
    protected static final String ResPayRemoteIpKey = "ResPayRemoteIp";

    protected ResDaiFuListService resDaiFuListService;
    protected PayProps payProps;
    protected PaymentManager paymentManager;
    protected RequestDaiFuDao requestDaiFuDao; //   @Qualifier("reqDaiFuDao")
    protected HandlerUtil handlerUtil;
    protected PayJmsSender payJmsSender;
    protected CacheService cacheService;

    protected ResPayListService resPayListService;
    protected RequestPayDao reqPayDao;   //  @Qualifier("reqPayDao")

    public abstract T receive(String channelName, Map<String, String> responseMap);
    public abstract String  responseForDbInterface(T t);   //响应数据库
    public abstract String responseForRestInterface( T t);  //响应rest



    interface Info<T>{
        public T getVar() ;


    }



    //通知db是否成功判断
    public boolean getResDbResult(String dbMsg,String key,String value){
        if(null==dbMsg || StringUtils.isBlank(dbMsg) ) return false;
        JSONObject dbMsgJson = JSON.parseObject(dbMsg);
        if(dbMsgJson.containsKey(key) && dbMsgJson.getString(key).equalsIgnoreCase(value)) return true;
        return  false;
    }




    public void resDbMsgNextTime(ResListI  resList){
        if(respDbIsNotEnough(resList.getResDbCount()) && StringUtils.isNotBlank(resList.getResDbResult())&& !resList.getResDbResult().equalsIgnoreCase("SUCCESS")){
            try{
                // 3/ 5/15/15/30/180/180/1800/1800/1800/3600
                int nextNotifCoe = getNotfieCoe(resList.getResDbCount());
                payJmsSender.sendLater(resList,nextNotifCoe*1000L+RandomUtils.nextLong(500, 2000));
            }catch (Exception ex){
                log.error("[再次通知数据库，失败]:{}",JSON.toJSONString(resList),ex);
            }
        }
    }



    protected  boolean respDbIsNotEnough(int count){
        try{
            if(count<HandlerUtil.RESP_DB_TIMES) return  true;
        }catch (Exception ex){
            return false;
        }
        return false;
    }



    //计算通知频次
    // 3/5/15/15/30/180/180/1800/3600/3600
    protected int getNotfieCoe(int resDbCount){
        int nextNotifCoe = 3600;
        switch(resDbCount)
        {
            case 1:
                nextNotifCoe=3;
                break;
            case 2:
                nextNotifCoe=5;
                break;
            case 3:
                nextNotifCoe=15;
                break;
            case 4:
                nextNotifCoe=15;
                break;
            case 5:
                nextNotifCoe=30;
                break;
            case 6:
                nextNotifCoe=180;
                break;
            case 7:
                nextNotifCoe=180;
                break;
            case 8:
                nextNotifCoe=1800;
                break;
            case 9:
                nextNotifCoe=3600;
                break;
            default:
                nextNotifCoe=3600;
                break;
        }
        return nextNotifCoe;
    }





}
