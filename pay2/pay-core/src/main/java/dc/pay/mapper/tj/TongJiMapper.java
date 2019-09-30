package dc.pay.mapper.tj;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseMapper;
import dc.pay.entity.pay.PayApiUrl;
import dc.pay.entity.tj.BillVerify;
import dc.pay.entity.tj.TongJi;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface TongJiMapper extends BaseMapper<TongJi> {



    /**
     * 获取业主个数
     */
    int  getOidCount(@Param("startDateTime")String startDateTime,@Param("endDateTime")String endDateTime);


    /**
     * 获取指定日期，统计的条数
     */
    int  getCglCountbyDay(@Param("tj_time_stmp")String tj_time_stmp);



    /**
     * 成功率(全部通道,微信,支付宝,QQ,百度,京东,网银)
     */
    List<TongJi>  getAllCgl(@Param("tongJiSearchCondition") TongJi tongJiSearchCondition,@Param("startDateTime")String startDateTime,@Param("endDateTime")String endDateTime);


    /**
     * 成功率-按日统计(全部通道,微信,支付宝,QQ,百度,京东,网银)
     */
    List<TongJi>  getAllCglInTjByDay(@Param("tongJiSearchCondition") TongJi tongJiSearchCondition,@Param("startDateTime")String startDateTime,@Param("endDateTime")String endDateTime);




    /**
     * 成功率,通道，OID,MemberID(全部通道,微信,支付宝,QQ,百度,京东,网银)
     */
    List<TongJi>  getAllCglFordbChannelSort(@Param("tongJiSearchCondition") TongJi tongJiSearchCondition,@Param("startDateTime")String startDateTime,@Param("endDateTime")String endDateTime);

    /**
     * 成功率,通道，OID(全部通道,微信,支付宝,QQ,百度,京东,网银)
     */
    List<TongJi>  getAllCglByDay(@Param("startDateTime")String startDateTime,@Param("endDateTime")String endDateTime);


    //批量插入日统计数据
    int insertAllCglByDayBatch(@Param("tongJis")List<TongJi> tongJis,@Param("tj_time_stmp")String tj_time_stmp);

    //删除日统计数据
    int delAllCglByDayBatch(@Param("tj_time_stmp")String tj_time_stmp);

    /**
     * 成功率(按通道名)-图表-单个通道
     */
    List<TongJi>  getAllCglByChannel(@Param("tongJiSearchCondition") TongJi tongJiSearchCondition,@Param("startDateTime")String startDateTime,@Param("endDateTime")String endDateTime,@Param("channelName")String channelName,@Param("timeStmpStr")String timeStmpStr);


   /**
     * 成功率(按通道名)-图表-全部通道
     */
    List<TongJi>  getAllCglByAllChannel(@Param("tongJiSearchCondition") TongJi tongJiSearchCondition,@Param("time_stmp_col_name")String time_stmp_col_name,@Param("startDateTime")String startDateTime,@Param("endDateTime")String endDateTime,@Param("timeStmpStr")String timeStmpStr);

    /**
     * 核对入款数据
     */
    List<BillVerify> billVerify(@Param("startDateTime")String startDateTime, @Param("endDateTime")String endDateTime);


    /**
     * 检查今日url正确性
     */
    List<PayApiUrl> payApiUrl();


}
