package dc.pay.service.tj;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.constant.PayEumeration;
import dc.pay.entity.tj.TjStatus;
import dc.pay.mapper.tj.TjStatusMapper;
import dc.pay.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * ************************
 * @author tony 3556239829
 */
@Service
public class TjStatusService {

    @Autowired
    TjStatusMapper tjStatusMapper;


    public TjStatus getById(Long id) {
        return tjStatusMapper.selectByPrimaryKey(id);
    }

    //日期统计名称查询统计状态
    public  TjStatus getByTjTimeStmpAndName(String tj_time_stmp,String tj_name) {
        return tjStatusMapper.getByTjTimeStmp(tj_time_stmp,tj_name);
    }

   //日期统计名称查询统计状态个数
    public   int  getTjStatusByDateAndName(String tj_time_stmp,String tj_name) {
        return tjStatusMapper.getTjStatusByDateAndName(tj_time_stmp,tj_name);
    }



    public void deleteById(long id) {
        tjStatusMapper.deleteByPrimaryKey(id);
    }


    //删除统计-日统计
     public int  delStatusByDayBatch( String tj_time_stmp) {
       return  tjStatusMapper.delStatusByDayBatch(tj_time_stmp);
    }





    public int save(TjStatus tjStatus) {
        if (tjStatus.getId() != null) {
          return  tjStatusMapper.updateByPrimaryKey(tjStatus);
        } else {
            return tjStatusMapper.insert(tjStatus);
        }
    }




    //生成新统计状态
    public int saveNewTjStatus(String dateStr,String locker){
        TjStatus tjStatus = new TjStatus();
        tjStatus.setTjIsLocked(PayEumeration.TJ_LOCKED_STATUS_ON);
        tjStatus.setTjLocker(locker);
        tjStatus.setTjName(PayEumeration.TJ_BY_DAY);
        tjStatus.setTjStatus(PayEumeration.TJ_STATUS_NEW);
        tjStatus.setTjTimeStmp(DateUtil.getDate(dateStr,"yyyy-MM-dd"));
        tjStatus.setTimeStmp(new Date());
        return save(tjStatus);
    }

    //保存完成统计状态
    public int saveFinishTjStatus(TjStatus tjStatus,int tjCount){
        tjStatus.setTimeStmp(new Date());
        tjStatus.setTjCount(tjCount);
        tjStatus.setTjIsLocked(PayEumeration.TJ_LOCKED_STATUS_OFF);
        tjStatus.setTjStatus(PayEumeration.TJ_STATUS_FINISH);
        return save(tjStatus);
    }



}
