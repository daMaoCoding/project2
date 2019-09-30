package dc.pay.controller.pay;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.business.RequestPayResult;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.pay.ReqPayList;
import dc.pay.service.pay.ReqPayListService;
import dc.pay.service.pay.RequestPayService;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
@RestController
@RequestMapping("/reqPay")
public class RequestPayController {
    private static final Logger log =  LoggerFactory.getLogger(RequestPayController.class);
    private static final String DEFAULT_API_JUMP_URL_PREFIX = "http://www.defaultAPiJumpUrlPrefix.com";
    private static final String DEFAULT_API_WEB_URL = "http://www.defaultAPiWebUrl.com";
    private static final String DEFAULT_API_NOTIFY_URL_PREFIX = "http://www.defaultAPiNotifyUrlPrefix.com";
    private static final String DEFAULT_API_ORDER_FROM = "6";
    private static final String DEFAULT_ORDER_OID_SPLIT = "O";
    private static final String DEFAULT_API_OID = "DEFAULT_API_OID";

    @Autowired
    private RequestPayService requestPayService;

    @Autowired
    private ReqPayListService reqPayListService;

    @Autowired
    HandlerUtil handlerUtil;


    @RequestMapping(value = "/{orderId}",method ={RequestMethod.POST, RequestMethod.GET})
    public RequestPayResult getReqPayInfoById(@PathVariable("orderId") String orderId,@RequestBody(required = false) Map<String,String> resParams) {
        /*System.out.println("+++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("@RequestMapping(value = /{orderId})");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++");*/
        return requestPayService.requestPayResultByIdSaveDB(orderId,resParams);
    }


    @RequestMapping(value = {"/testReqPay/*"},method = {RequestMethod.POST, RequestMethod.GET},consumes={"application/json"} )
    public RequestPayResult testGetReqPayInfoById(@RequestBody(required = false) ReqPayInfo reqPayInfo) {
        log.info("[生成订单支付信息-测试支付通道]_application/json: {}", JSON.toJSONString(reqPayInfo));
        if(!reqPayInfo.getAPI_ORDER_ID().startsWith("T")){
            if(StringUtils.isBlank(reqPayInfo.getAPI_ORDER_ID())){
                reqPayInfo.setAPI_ORDER_ID(HandlerUtil.getDateTimeByMilliseconds(String.valueOf(System.currentTimeMillis()),"yyyyMMddHHmmssSSS"));
            }
          //  reqPayInfo.setAPI_ORDER_ID("T".concat(reqPayInfo.getAPI_ORDER_ID()));  //测试订单号
        }
        if(StringUtils.isBlank(reqPayInfo.getAPI_AMOUNT()) || Long.parseLong(reqPayInfo.getAPI_AMOUNT()) < 1){
            int randomNum = HandlerUtil.getRandomIntBetweenAA(1, 1000); //随机金额
            reqPayInfo.setAPI_AMOUNT(String.valueOf(Integer.parseInt("101")+randomNum));  //15元以上
        }
        if(StringUtil.isBlank(reqPayInfo.getAPI_JUMP_URL_PREFIX())){
            reqPayInfo.setAPI_JUMP_URL_PREFIX(DEFAULT_API_JUMP_URL_PREFIX);
        }
        if(StringUtil.isBlank(reqPayInfo.getAPI_WEB_URL())){
            reqPayInfo.setAPI_WEB_URL(DEFAULT_API_WEB_URL);
        }
        if(StringUtil.isBlank(reqPayInfo.getAPI_NOTIFY_URL_PREFIX())){
            reqPayInfo.setAPI_NOTIFY_URL_PREFIX(DEFAULT_API_NOTIFY_URL_PREFIX);
        }
        if(StringUtils.isBlank(reqPayInfo.getAPI_OID()) || (StringUtils.isNotBlank(reqPayInfo.getAPI_ORDER_ID()) && reqPayInfo.getAPI_ORDER_ID().contains(DEFAULT_ORDER_OID_SPLIT))){//OID
            String OID = reqPayInfo.getAPI_ORDER_ID().split(DEFAULT_ORDER_OID_SPLIT).length!=2?null:reqPayInfo.getAPI_ORDER_ID().split(DEFAULT_ORDER_OID_SPLIT)[1];
            reqPayInfo.setAPI_OID(StringUtils.isBlank(OID)?DEFAULT_API_OID:OID);
        }
        if(StringUtils.isBlank(reqPayInfo.getAPI_Client_IP())){
            reqPayInfo.setAPI_Client_IP(HandlerUtil.DEFAULT_API_Client_IP);
        }

        if(StringUtils.isBlank(reqPayInfo.getAPI_ORDER_FROM())){
            reqPayInfo.setAPI_ORDER_FROM(DEFAULT_API_ORDER_FROM);
        }

       // reqPayInfo.setAPI_ORDER_ID(reqPayInfo.getAPI_ORDER_ID().concat("R"+RandomUtils.nextInt(100,900)));
        reqPayInfo.setAPI_ORDER_ID("T".concat( new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())).concat(RandomUtils.nextInt(100,900)+""));

        if(reqPayInfo.getAPI_CHANNEL_BANK_NAME().endsWith("_FS")){ //反扫-直接返回正确
            try {
                RsaUtil.decryptAndCache(reqPayInfo.getAPI_KEY());
            } catch (PayException e) {
                return new  RequestPayResult(false,reqPayInfo ,"秘钥解密出错");
            }
            return new RequestPayResult(reqPayInfo ,"/alwaysSucceedFoFanSao/");
        }

        return  requestPayService.getRequestPayResultByReqPayInfoSaveDB(reqPayInfo,0L);
    }



    /**
     * Json 通过订单号查询 - pay-rest 请求跳转html
     * @param orderId
     * @return
     */
    @RequestMapping(value = "/viewJson/{orderId}",method = {RequestMethod.POST, RequestMethod.GET},consumes={"application/json"})
    @ResponseBody
    public ReqPayList view(@PathVariable(value = "orderId",required = true)  String orderId) {
        if(StringUtils.isNotBlank(orderId) && !orderId.contains("'")&& !orderId.contains(";") && orderId.length()<35){
            ReqPayList  reqPayList   = reqPayListService.getByOrderId(orderId.trim());
            if(null!=reqPayList && reqPayList.getRestView()<10) reqPayListService.updataRestView(orderId);
            if(null!=reqPayList && reqPayList.getRestView()>2){
                reqPayList.getRequestPayResult().setRequestPayHtmlContent("该笔订单已访问过("+reqPayList.getRestView()+")次，为避免重复支付，请重新充值。<br>订单号："+orderId+"  ,时间："+DateUtil.formatDateTime(reqPayList.getTimeStmp()));
            }
            return reqPayList;
        }
        return null;
    }



    /**
     * 返回html
     */
    @RequestMapping(value = {"/getreqthml/{orderId}"},method = {RequestMethod.POST, RequestMethod.GET})
    public void  getreqthml(@PathVariable(value = "orderId",required = true) String orderId, HttpServletResponse response) {
        String resPMsg = "Error";
        try{
            if(StringUtils.isNotBlank(orderId) && orderId.startsWith(HandlerUtil.TMPORDERKEY) ){
                String strFromRedis = handlerUtil.getStrFromRedis(orderId);
                resPMsg = StringUtils.isNotBlank(strFromRedis)?strFromRedis:resPMsg;
                reqPayListService.writeResponse(response,resPMsg);
            }
        }catch (Exception e){
            log.error("HTML内容错误：{}",orderId,e);
        }
    }








}
