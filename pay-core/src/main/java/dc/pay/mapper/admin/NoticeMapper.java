package dc.pay.mapper.admin;

import dc.pay.entity.admin.Notice;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
  * 通知表 Mapper 接口
 * </p>
 */
public interface NoticeMapper extends Mapper<Notice> {

    List<Map<String, Object>> list(@Param("condition") String condition);

}