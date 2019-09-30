package dc.pay.mapper.admin;

import dc.pay.entity.admin.LoginLog;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
  * 登录记录 Mapper 接口
 * </p>
 *
 */
public interface LoginLogMapper extends Mapper<LoginLog> {

    /**
     * 获取操作日志
     */
    List<Map<String, Object>> getOperationLogs(@Param("beginTime") String beginTime, @Param("endTime") String endTime, @Param("logName") String logName, @Param("logType") String logType, @Param("orderByField") String orderByField, @Param("isAsc") boolean isAsc);

    /**
     * 获取登录日志
     */
    List<Map<String, Object>> getLoginLogs(@Param("loginName") String loginName,@Param("loginIp") String loginIp,@Param("beginTime") String beginTime, @Param("endTime") String endTime, @Param("logName") String logName, @Param("orderByField") String orderByField, @Param("isAsc") boolean isAsc);

}