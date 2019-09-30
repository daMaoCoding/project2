package dc.pay.business.wuxingdaifu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import net.dongliu.requests.RequestBuilder;
import net.dongliu.requests.Requests;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.*;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class WuXingDaiFuUtil {

    public static  final String api_key = "api_key";

    public static String signObjToStr(Object req) {
        JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(req));
        List<String> keys = new ArrayList<String>(jsonObject.keySet());
        keys.sort(Comparator.naturalOrder());
        List<String> paramStringList = new ArrayList<String>();
        for (String key : keys) {
            if (StringUtils.equals(key, "sign"))
                continue;
            Object value = jsonObject.get(key);
            String valueStr = value.toString();
            if (value instanceof BigDecimal) {
                valueStr = String.format("%.2f", ((BigDecimal) value).doubleValue());
            }
            paramStringList.add(String.format("%s=%s", key, valueStr));
        }
        String signStr = StringUtils.join(paramStringList, '&');
        return signStr;
    }


    public static String sign(Map payParam,String apiSecret) throws PayException {
        String signStr = signObjToStr(payParam);

        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            String pay_md5sign = new String(Hex.encodeHex(sha256_HMAC.doFinal(signStr.getBytes())));
            if(StringUtils.isBlank(pay_md5sign)) throw new PayException("五星代付使用密钥签名结果空，请检查密钥");
            return pay_md5sign;
        } catch (Exception e) {
            throw new PayException("五星代付使用密钥签名错误，请检查密钥.", e);
        }
    }


    public static RequestBuilder post(String url,Map req,String token) {
        JSONObject jsonObject;
        if (req == null)
            jsonObject = new JSONObject();
        else
            jsonObject = (JSONObject) JSON.toJSON(req);

        Map<String, String> headers = new HashMap<>();
        headers.put(api_key, token);
        return Requests.post(url).jsonBody(jsonObject).headers(headers).timeout(30000);
    }



}
