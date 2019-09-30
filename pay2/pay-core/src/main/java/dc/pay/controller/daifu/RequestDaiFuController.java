package dc.pay.controller.daifu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.business.RequestDaifuResult;
import dc.pay.business.RequestPayResult;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.constant.PayEumeration;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.ReqDaifuQueryBalance;
import dc.pay.entity.ReqPayInfo;
import dc.pay.service.daifu.RequestDaiFuService;
import dc.pay.service.pay.RequestPayService;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
@RestController
@RequestMapping("/reqDaiFu")
public class RequestDaiFuController {
    private static final Logger log =  LoggerFactory.getLogger(RequestDaiFuController.class);
    private static final String DEFAULT_API_NOTIFY_URL_PREFIX = "http://www.defaultAPiNotifyUrlPrefix.com";
    private static final String DEFAULT_API_OID = "DEFAULT_API_OID";
    private static final String DEFAULT_API_ORDER_FROM = "6";
    private static final String DEFAULT_ORDER_OID_SPLIT = "O";

    @Autowired
    private RequestDaiFuService requestDaiFuService;

    @Autowired
    HandlerUtil handlerUtil;


    @RequestMapping(value = "/{orderId}",method ={RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public RequestDaifuResult getReqDaifuInfoById(@PathVariable("orderId") String orderId, @RequestBody(required = false) Map<String,String> resParams) {
        RequestDaifuResult requestDaifuResult = requestDaiFuService.requestDaifuResultByIdSaveDB(orderId, resParams);
        return removeDetailAndParams(requestDaifuResult);
    }


    @RequestMapping(value = "/query/{orderId}",method ={RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseDaifuResult getQueryDaifuInfoById(@PathVariable("orderId") String orderId, @RequestBody(required = false) Map<String,String> resParams) {
        ResponseDaifuResult responseDaifuResult = requestDaiFuService.queryDaifuResultById(orderId, resParams);
        responseDaifuResult.setReqDaifuInfo(null); //来源：后台查询，非pay-rest
        return responseDaifuResult;
    }


    @RequestMapping(value = "/query/balance",method ={RequestMethod.POST},consumes={"application/json"} )
    @ResponseBody
    public ReqDaifuQueryBalance getQueryDaifuBalance(@RequestBody(required = false) ReqDaifuInfo reqDaifuInfo) {
        return requestDaiFuService.getQueryDaifuBalance(reqDaifuInfo) ;
    }


    @RequestMapping(value = {"/testReqDaiFu/*"},method = {RequestMethod.POST, RequestMethod.GET},consumes={"application/json"} )
    @ResponseBody
    public RequestDaifuResult testGetReqDaifuInfoById(@RequestBody(required = false) ReqDaifuInfo reqDaifuInfo) {
        try {
             ValidateUtil.valdataReqDaifuInfo(reqDaifuInfo) ;
        } catch (PayException e) {
            return new RequestDaifuResult(false,e.getMessage(),PayEumeration.DAIFU_RESULT.ERROR);
        }
        log.info("[生成订单支付信息-测试支付通道]_application/json: {}", JSON.toJSONString(reqDaifuInfo));
        if(!reqDaifuInfo.getAPI_ORDER_ID().startsWith("T")){
            if(StringUtils.isBlank(reqDaifuInfo.getAPI_ORDER_ID())){
                reqDaifuInfo.setAPI_ORDER_ID(HandlerUtil.getDateTimeByMilliseconds(String.valueOf(System.currentTimeMillis()),"yyyyMMddHHmmssSSS"));
            }
        }
        if(StringUtils.isBlank(reqDaifuInfo.getAPI_AMOUNT()) || Long.parseLong(reqDaifuInfo.getAPI_AMOUNT()) < 1){
            reqDaifuInfo.setAPI_AMOUNT(1+"");
        }
        if(StringUtils.isBlank(reqDaifuInfo.getAPI_AMOUNT()) && Long.parseLong(reqDaifuInfo.getAPI_AMOUNT()) > 10000){
            return new RequestDaifuResult(false,"防止误操作(测试通道其实也是在真转账)，测试金额不能大于100元，如第三方有限制，请联系第三方修改金额。",PayEumeration.DAIFU_RESULT.ERROR);
        }
        if(StringUtil.isBlank(reqDaifuInfo.getAPI_NOTIFY_URL_PREFIX())){
            reqDaifuInfo.setAPI_NOTIFY_URL_PREFIX(DEFAULT_API_NOTIFY_URL_PREFIX);
        }
        if(StringUtils.isBlank(reqDaifuInfo.getAPI_OID()) || (StringUtils.isNotBlank(reqDaifuInfo.getAPI_ORDER_ID()) && reqDaifuInfo.getAPI_ORDER_ID().contains(DEFAULT_ORDER_OID_SPLIT))){//OID
            String OID = reqDaifuInfo.getAPI_ORDER_ID().split(DEFAULT_ORDER_OID_SPLIT).length!=2?null:reqDaifuInfo.getAPI_ORDER_ID().split(DEFAULT_ORDER_OID_SPLIT)[1];
            reqDaifuInfo.setAPI_OID(StringUtils.isBlank(OID)?DEFAULT_API_OID:OID);
        }
        if(StringUtils.isBlank(reqDaifuInfo.getAPI_Client_IP())){
            reqDaifuInfo.setAPI_Client_IP(HandlerUtil.DEFAULT_API_Client_IP);
        }

        reqDaifuInfo.setAPI_ORDER_ID("T".concat( new SimpleDateFormat(DateUtil.dateTimeString2).format(new Date())).concat(RandomUtils.nextInt(100,900)+""));
        RequestDaifuResult requestDaifuResult = requestDaiFuService.getRequesDaifuResultByReqPayInfoSaveDB(reqDaifuInfo, 0L);

        return  removeDetailAndParams(requestDaifuResult);
    }



    //去掉结果中的参数及过程
    public RequestDaifuResult removeDetailAndParams(RequestDaifuResult requestDaifuResult){
        if(null!=requestDaifuResult){
            requestDaifuResult.setDetails(null);
            requestDaifuResult.setParams(null);
        }
        return requestDaifuResult;
    }

}
