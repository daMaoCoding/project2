package dc.pay.mapper.pay;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseMapper;
import dc.pay.entity.pay.ReqPayList;
import org.apache.ibatis.annotations.Param;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface ReqpayListMapper extends BaseMapper<ReqPayList> {

    String getAllAmount(@Param("reqPayList")ReqPayList reqPayList);

    ReqPayList getReqpayListByMemberId(@Param("memberId")String memberId);

    int updataRestView(@Param("orderId")String orderId);
}
