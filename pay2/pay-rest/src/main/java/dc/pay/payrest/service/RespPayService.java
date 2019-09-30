package dc.pay.payrest.service;
/**
 * Created by admin on 2017/6/25.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Component
@Qualifier("RespPayService")
public class RespPayService {


    @Autowired
    @Qualifier("PayRestComponent")
    PayRestComponent payRestComponent;

    private static final Logger log =  LoggerFactory.getLogger(RespPayService.class);

    private static final String RESPAY_SERV_PATH = "/respPay/";
    private static final String RESPONSE_PAY_MSG = "responsePayMsg";


    //转发并获取第三方需要的消息
    public void postAndWriteResponse( String channelName,  Map<String, String> mapParams, HttpServletResponse response, HttpServletRequest request) {
        try{
            String result = postForPayServ(channelName,mapParams,request);
            writeResponse(response,result);
        }catch (Exception e){e.printStackTrace();}finally {
            mapParams=null;
        }
    }


    //目前只能处理,request.getInputStream内容是  {"A":"B"},不能处理request.getInputStream内容是  data= {"A":"B"}
    public Map<String,String> getMapUseJsonStr(HttpServletRequest request) {
        return  payRestComponent.getMapUseJsonStr(request);
    }


    // 处理request参数
    public Map<String,String> processRequestParam(HttpServletRequest request) {
        return  payRestComponent.processRequestParam(request);
    }

    //返回第三方消息
    public void writeResponse(HttpServletResponse response, String result) {
         payRestComponent.writeResponse(response,result,RESPONSE_PAY_MSG);
    }

    // 转发内容至支付网关
    public String postForPayServ(String channelName, Map<String,String> mapBodys, HttpServletRequest request) {
        return  payRestComponent.postForPayServ(channelName,mapBodys,request,RESPAY_SERV_PATH);
    }
}
