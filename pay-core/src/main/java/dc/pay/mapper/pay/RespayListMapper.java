package dc.pay.mapper.pay;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseMapper;
import dc.pay.entity.pay.ResPayList;
import org.apache.ibatis.annotations.Param;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface RespayListMapper extends BaseMapper<ResPayList> {

    String getAllAmount(@Param("resPayList")ResPayList resPayList);
}
