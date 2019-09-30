package dc.pay.business.baifu;

import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class BaiFuPayUtil {
    public static final   String buildJsonParam(Map<String,String> params,String sign){
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append("\"").append(paramKeys.get(i)).append("\"").append(":").append("\"").append(params.get(paramKeys.get(i))).append("\"");
            if(i<paramKeys.size()-1)
                sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

}
