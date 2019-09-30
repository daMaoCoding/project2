package dc.pay.payrest;/**
 * Created by admin on 2017/6/25.
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Component
@Qualifier("RespPayService")
public class RespPayService {

    private static final String RESPAY_SERV_PATH = "/respPay/";
    private static final String RESPONSE_PAY_MSG = "responsePayMsg";
    private static final String RESPONSE_PAY_CODE = "responsePayCode";

    @Autowired
    @Qualifier("PayRestComponent")
    PayRestComponent payRestComponent;


    /**
     * 转发内容至支付网关
     */
    public String postForPayServ(String channelName,Map<String,String> mapParams,HttpServletRequest request){
        return payRestComponent.postForPayServUtil(channelName, mapParams, request, RESPAY_SERV_PATH);
    }

    /**
     * 转发回复内容至第三方支付(无论验证是否成功，总返回第三方支付验证成功,目的：使第三方不再继续发送通知，具体错误由人工处理)
     */
    public  void writeResponse(HttpServletResponse response, String result){
        payRestComponent.writeResponse(response, result,  RESPONSE_PAY_MSG);
    }


    /**
     * 处理request参数
     */
    public  Map<String, String> processRequestParam(HttpServletRequest request) {
        return  payRestComponent.processRequestParam(request);
    }

    //目前只能处理,request.getInputStream内容是  {"A":"B"},不能处理request.getInputStream内容是  data= {"A":"B"}
    public Map<String, String> getMapUseJsonStr(HttpServletRequest request) {
        return payRestComponent.getMapUseJsonStr(request);
    }


}
