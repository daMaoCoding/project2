package dc.pay.base;/**
 * Created by admin on 2017/6/3.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.MySqlMapper;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface BaseMapper<T> extends Mapper<T>, MySqlMapper<T> {
    String LOG_FOR_DB_ONLY = "logForDbOnly";
    public static final Logger logForDbOnly = LoggerFactory.getLogger(LOG_FOR_DB_ONLY);
}