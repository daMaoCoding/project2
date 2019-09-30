package dc.pay.mapper.bill;/**
 * Created by admin on 2017/6/5.
 */

import dc.pay.base.BaseMapper;
import dc.pay.entity.bill.Bill;
import org.apache.ibatis.annotations.Param;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface BillMapper extends BaseMapper<Bill> {

    Bill getByAPI_ORDER_ID(@Param("API_ORDER_ID") String API_ORDER_ID);


}
