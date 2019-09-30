package dc.pay.service.tj;

import dc.pay.constant.PayEumeration;
import dc.pay.entity.tj.TjStatus;
import dc.pay.entity.tj.TongJi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChannelStatusJobService {



    @Autowired
    private TongJiService tongJiService;


    @Autowired
    private TjStatusService tjStatusService;



    //通知排序
    public  Map<String,String>   notifDbChannelSortAddOid(String oid){
        Map<String,String> spHeader = new HashMap<String,String>() {
            {
                put("HTTP-CUST-OID",oid);
            }
        };
        return spHeader;
    }



    @Transactional
    public boolean insAllCglByDay(String dataStr, TjStatus tjStatus) throws RuntimeException {
        List<TongJi> allCglByDay = tongJiService.getAllCglByDay(dataStr); //该段时间统计结果
        int tjCount    = tongJiService.insertAllCglByDayBatch(allCglByDay, dataStr);
        int insStatCount =  tjStatusService.saveFinishTjStatus(tjStatus,tjCount);
        int statusCount = tjStatusService.getTjStatusByDateAndName(dataStr, PayEumeration.TJ_BY_DAY);
        if(insStatCount==1 && statusCount==1 && tjStatus.getTjStatus().equalsIgnoreCase(PayEumeration.TJ_STATUS_FINISH)){return true;}
        return false;
    }


}
