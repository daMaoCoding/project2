package dc.pay.dao.pay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * ************************
 *
 * @author tony 3556239829
 */

@Repository
@Qualifier("respPayDao")
public class ResponsePayDao {
    private static final Logger log =  LoggerFactory.getLogger(ResponsePayDao.class);
}
