package dc.pay.mapper.daifu;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseMapper;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.entity.pay.ResPayList;
import org.apache.ibatis.annotations.Param;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface ResDaifuListMapper extends BaseMapper<ResDaiFuList> {

    String getAllAmount(@Param("resDaiFuList") ResDaiFuList resDaiFuList);
}
