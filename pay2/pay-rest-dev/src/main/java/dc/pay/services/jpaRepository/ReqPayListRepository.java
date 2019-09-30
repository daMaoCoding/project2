package dc.pay.services.jpaRepository;

import dc.pay.entity.jpa.ReqPayList;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;



public interface ReqPayListRepository extends CrudRepository<ReqPayList, Long> {

    @Query("select a  from ReqPayList a where a.orderId = :orderId")
    public ReqPayList findbyOrOrderId(@Param("orderId")String orderId);

}
