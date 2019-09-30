package dc.pay.payrest.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.payrest.service.RequestPayJumpService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * ************************
 * 网银跳转
 * @author tony 3556239829
 */

@RestController
@RequestMapping("/wy")
public class RequestPayJumpController {
    private static final Logger log =  LoggerFactory.getLogger(RequestPayJumpController.class);
    @Autowired
    @Qualifier("RequestPayJumpService")
    RequestPayJumpService requestPayJumpService;


    @RequestMapping(value = {"/jmp/{orderId}"},method = {RequestMethod.POST, RequestMethod.GET},produces = {"text/plain;charset=UTF-8" })
    public void  jumpToBank(@PathVariable(value = "orderId",required = true) String orderId, HttpServletResponse response) {
        String resPMsg = "Error";
       try{
            if(StringUtils.isBlank(orderId) || orderId.contains("'")|| orderId.contains(";") ||orderId.length()>30){
                throw new Exception("网银跳转出错,订单号错误："+orderId);
            }
           String result = requestPayJumpService.getToPayServ(orderId.trim());
           if(StringUtils.isNotBlank(result)){
               JSONObject reqPayList = JSON.parseObject(result);
               JSONObject reqPayInfo = reqPayList.getJSONObject("requestPayResult");
               String requestPayCode = reqPayInfo.getString("requestPayCode");
               String requestPayHtmlContent = reqPayInfo.getString("requestPayHtmlContent");
               if("SUCCESS".equalsIgnoreCase(requestPayCode))
                   resPMsg = requestPayHtmlContent;
           }
           requestPayJumpService.writeResponse(response,resPMsg);
       }catch (Exception e){
           log.error("网银跳转出错,订单号："+orderId+" ,错误消息："+e.getMessage(),e);
           requestPayJumpService.writeResponse(response,resPMsg);
       }

    }


}
