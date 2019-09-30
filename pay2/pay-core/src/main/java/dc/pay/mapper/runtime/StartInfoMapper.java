package dc.pay.mapper.runtime;

import dc.pay.base.BaseMapper;
import dc.pay.entity.runtime.StartInfo;

import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public interface StartInfoMapper extends BaseMapper<StartInfo> {

    Map<Long,Long> getReqPayListMaxCleanIdAndCount(String dateTime);
    Long cleanReqPayList(Long maxId);

    Map<Long,Long> getResPayListMaxCleanIdAndCount(String dateTime);
    Long cleanResPayList(Long maxId);

    Map<Long,Long> getReqDaifuListMaxCleanIdAndCount(String dateTime);
    Long cleanReqDaifuList(Long maxId);

    Map<Long,Long> getResDaifuListMaxCleanIdAndCount(String dateTime);
    Long cleanResDaifuList(Long maxId);


    Long cleanStartInfo();
    Long cleanTjStatus(String dateTime);
    Long cleanTjByDay(String dateTime);


}
