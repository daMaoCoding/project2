package dc.pay.payrest.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import dc.pay.payrest.service.PayRestComponent;
import dc.pay.payrest.service.RequestPayJumpService;
import dc.pay.payrest.service.RespPayService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Map;

/**
 * ************************
 * 反扫跳转
 * @author tony 3556239829
 * http://localhost:8081/fs/ZHITONGBAO_BANK_WAP_QQ_FS/{ZHITONGBAO_QQ_FS-mYPcE}/{30000}
 */

@Controller
@RequestMapping("/fs")
public class RequestPayFsJumpController {
    private static final Logger log =  LoggerFactory.getLogger(RequestPayFsJumpController.class);
    @Autowired
    @Qualifier("RequestPayJumpService")
    RequestPayJumpService requestPayJumpService;

    @Autowired
    @Qualifier("RespPayService")
    RespPayService respPayService;


    @Autowired
    @Qualifier("PayRestComponent")
    PayRestComponent payRestComponent;


    @RequestMapping(value = {"/{channelName}/{orderId}/{amount}/{ip}"},method = {RequestMethod.POST, RequestMethod.GET})
    public String jumpToShowRequestPay(@PathVariable(value = "channelName",required = true) String channelName, @PathVariable(value = "orderId",required = true) String orderId, @PathVariable(value = "amount",required = true) String amount,@PathVariable(value = "ip",required = true) String ip,  HttpServletResponse response,Model model) {
          if(requestPayJumpService.valInput(channelName) && requestPayJumpService.valInput(orderId)  && requestPayJumpService.valInput(amount)){
              model.addAttribute("channelName", channelName);
              model.addAttribute("orderId", orderId);
              model.addAttribute("amount",requestPayJumpService.getYuan(amount));
              model.addAttribute("now", new Date());
              model.addAttribute("ip",ip);
              return "showRequestPay";
          }else{
              requestPayJumpService.writeResponse(response, "Error");
          }
          return null;
    }



    //反扫请求支付
    @RequestMapping(value = {"/jumpToRequestPay/*"},method = {RequestMethod.POST})
    @ResponseBody
    public Map<String, String>  jumpToRequestPay(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> resultMap = Maps.newHashMap();
        Map<String, String> params = payRestComponent.processRequestParam(request);
        if(payRestComponent.valMap(params) && params.containsKey("orderId")){
            params.put("sources","pay-rest-fs");
            String result = requestPayJumpService.getRequestPayResult(params.get("orderId").trim(),params);
            if(StringUtils.isNotBlank(result) && result.contains("requestPayCode")){
                JSONObject resultJsonObject = JSONObject.parseObject(result);
                String requestPayCode = resultJsonObject.getString("requestPayCode");
                resultMap.put("code",requestPayCode);
                if(requestPayCode.equalsIgnoreCase("SUCCESS")){
                    resultMap.put("msg","受理成功,请稍后刷新查看余额！");
                }else{
                    resultMap.put("msg","受理失败,请检查输入并重试！");
                }
            }else{
                resultMap.put("code","ERROR");
                resultMap.put("msg","网络错误,请重试。");
            }
        }else{
            resultMap.put("code","ERROR");
            resultMap.put("msg","参数错误,请重试。");
        }
        return resultMap;
    }



}
