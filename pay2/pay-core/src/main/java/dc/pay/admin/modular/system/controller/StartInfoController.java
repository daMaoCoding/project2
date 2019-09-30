package dc.pay.admin.modular.system.controller;

import com.github.pagehelper.PageInfo;
import dc.pay.admin.common.annotion.Permission;
import dc.pay.entity.runtime.StartInfo;
import dc.pay.service.runtime.StartInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Controller
@RequestMapping("/runTime")
public class StartInfoController {

    @Autowired
    StartInfoService startInfoService;


    @RequestMapping
    @Permission
    public ModelAndView getAll(StartInfo startInfo) {
        ModelAndView result = new ModelAndView("runTime/startInfo/index");
        List<StartInfo> countryList = startInfoService.getAll(startInfo);
        result.addObject("pageInfo", new PageInfo<StartInfo>(countryList));
        result.addObject("queryParam", startInfo);
        result.addObject("page", startInfo.getPage());
        result.addObject("rows", startInfo.getRows());
        return result;
    }


    @RequestMapping(value = "view/{id}")
    public ModelAndView view(@PathVariable Long id) {
        ModelAndView result = new ModelAndView("runTime/startInfo/view");
        StartInfo startInfo = startInfoService.getById(id);
        result.addObject("startInfo", startInfo);
        return result;
    }




    //pay-core 心跳
    @RequestMapping(value = "paycoreheartbeat")
    public ModelAndView paycoreheartbeat() {
        ModelAndView result = new ModelAndView("runTime/paycoreheartbeat");
        List<Map<String,String>>  heartBeatList = startInfoService.getPayCoreHeartBeat();
        result.addObject("heartBeatList", heartBeatList);
        return result;
    }


    //pay-rest 心跳
    @RequestMapping(value = "payrestheartbeat")
    public ModelAndView payrestheartbeat() {
        ModelAndView result = new ModelAndView("runTime/payrestheartbeat");
        List<Map<String,String>>  heartBeatList = startInfoService.getPayRestHeartBeat();
        result.addObject("heartBeatList", heartBeatList);
        return result;
    }


    @RequestMapping(value = "payapiurlhealthcheck")
    @Permission
    public  ModelAndView   payApiUrlHealthCheck() {
        ModelAndView result = new ModelAndView("runTime/payapiurlhealthcheck");
        List<Map<String,Object>>    payApiUrlList =  startInfoService.getPayApiUrlHealthCheck();
        result.addObject("payApiUrlList", payApiUrlList);
        return result;
    }


}
