package dc.pay.mapper.tj;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseMapper;
import dc.pay.entity.tj.TjStatus;
import org.apache.ibatis.annotations.Param;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface TjStatusMapper extends BaseMapper<TjStatus> {

     TjStatus  getByTjTimeStmp(@Param("tj_time_stmp") String tj_time_stmp, @Param("tj_name")String tj_name); //统计状态
     int  getTjStatusByDateAndName(@Param("tj_time_stmp") String tj_time_stmp, @Param("tj_name")String tj_name); //统计状态个数
     int  delStatusByDayBatch(@Param("tj_time_stmp") String tj_time_stmp); //删除当日统计byday

}
