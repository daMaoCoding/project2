package dc.pay.controller.channel;/**
 * Created by admin on 2017/7/13.
 */

import dc.pay.utils.excel.channelConfig.ExcelChannel;
import dc.pay.utils.excel.channelConfig.ExcelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ************************
 * @author tony 3556239829
 */
@RestController
@RequestMapping("/channelConfig")
public class ChannelConfigController {
    private static final Logger log =  LoggerFactory.getLogger(ChannelConfigController.class);
    @RequestMapping(value = {"","/"},method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public List<ExcelChannel> getAllChannelConfParam() throws Exception {
        List<ExcelChannel> channelConfParam = ExcelHelper.getChannelConfParamJsonAndExcel();
        log.debug("查询支付通道配置参数.........."+channelConfParam.size());
        return channelConfParam;
    }
}
