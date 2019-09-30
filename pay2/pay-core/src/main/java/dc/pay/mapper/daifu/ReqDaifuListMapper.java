package dc.pay.mapper.daifu;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseMapper;
import dc.pay.entity.daifu.ReqDaiFuList;
import dc.pay.entity.pay.ReqPayList;
import org.apache.ibatis.annotations.Param;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface ReqDaifuListMapper extends BaseMapper<ReqDaiFuList> {

    String getAllAmount(@Param("reqDaiFuList") ReqDaiFuList reqDaiFuList);

    ReqDaiFuList getReqDaiFuListByMemberId(@Param("memberId") String memberId);
    ReqDaiFuList getReqDaiFuListByMemberIdIncludeTestOrder(@Param("memberId") String memberId);


}
