package dc.pay.controller.pay;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayResService;
import dc.pay.business.ResponsePayResult;
import dc.pay.service.pay.ResponsePayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 * ************************
 * @author tony 3556239829
 */
@RestController
@RequestMapping("/respPay")
public class ResponsePayController {
    private static final Logger log =  LoggerFactory.getLogger(ResponsePayController.class);
    @Autowired
    private PayResService<ResponsePayResult> payResService;

    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET},consumes={"application/json"} )
    public ResponsePayResult receiveJSON(@PathVariable(value = "channelName",required = true) String channelName, @RequestBody(required = false) Map<String,String> mapBodys) {
            log.info("[收到充值回调通知(PayGate)]_application/json: {}",JSON.toJSONString(mapBodys));
            ResponsePayResult responsePayResult = payResService.receive(channelName, mapBodys);
            return responsePayResult;
    }

}