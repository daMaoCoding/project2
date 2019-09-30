package dc.pay.payrest;
/**
 * Created by admin on 2017/5/29.
 */

import dc.pay.utils.XmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * ************************
 * 接收&响应 第三方支付平台 web 接口
 * @author tony 3556239829
 */

@RestController
@RequestMapping("/respDaifuWeb")
public class RespDaifuController {

    @Autowired
    @Qualifier("RespDaifuService")
    RespDaifuService respDaifuService;


    /**
     * 接受第三方支付通知 & 转发
     */
    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET},consumes={"application/json"})
    public void  receiveJSON(@PathVariable(value = "channelName",required = true) String channelName, @RequestBody(required = false) Map<String,String> mapBodys, HttpServletResponse response,HttpServletRequest request) {
        String result = respDaifuService.postForPayServ(channelName , mapBodys,request);
        respDaifuService.writeResponse(response,result);
    }


    /**
     * 接受第三方支付通知 & 转发
     */
    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET},consumes={"application/x-www-form-urlencoded"} )
    public void receiveForm(@PathVariable(value = "channelName",required = true) String channelName, @RequestParam(required = false) Map<String,String> mapParams, HttpServletResponse response,HttpServletRequest request) {
        String result = respDaifuService.postForPayServ(channelName,mapParams,request);
        respDaifuService.writeResponse(response,result);
    }



    /**
     * 接受第三方支付通知 & 转发
     */
    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET},consumes={"text/xml","application/xml"})
    public void  receiveXML(@PathVariable(value = "channelName",required = true) String channelName, HttpServletRequest request,HttpServletResponse response) throws Exception {
        Map<String,String> mapBodys = XmlUtil.toMap(XmlUtil.parseRequst(request).getBytes(), "utf-8");
        String result = respDaifuService.postForPayServ(channelName , mapBodys,request);
        respDaifuService.writeResponse(response,result);
    }





    /**
     * 接受第三方支付通知 & 转发
     */
    @RequestMapping(value = {"/{channelName}/*",""},method = {RequestMethod.POST, RequestMethod.GET} )
    public void receive(@PathVariable(value = "channelName",required = true) String channelName, HttpServletRequest request,HttpServletResponse response) {
        Map<String, String> mapParams = respDaifuService.processRequestParam(request);
        if(null==mapParams || mapParams.isEmpty()){
            mapParams = respDaifuService.getMapUseJsonStr(request);
        }
        String result = respDaifuService.postForPayServ(channelName,mapParams,request);
        respDaifuService.writeResponse(response,result);
    }




}
