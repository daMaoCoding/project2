package dc.pay.controller.mock;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import dc.pay.controller.pay.ResponsePayController;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.bill.Bill;
import dc.pay.entity.ReqPayInfo;
import dc.pay.service.bill.BillService;
import dc.pay.utils.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */

@RestController
@RequestMapping("/test")
@ComponentScan(value = {"dc.pay","abc.com"})
public class MockDBController {
    private static final Logger log =  LoggerFactory.getLogger(ResponsePayController.class);

    @Autowired
    BillService billService;


    /**
     * 压力测试
     */
    @RequestMapping(value = "/yaliceshi",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public  String yaliceshi() {
        return HandlerUtil.OK;
    }




    /**
     * 请求支付信息
     */
    @RequestMapping(value = "/db/getReqPayinfoByOrderId/{orderId}",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public  ReqPayInfo getReqPayinfoByOrderId(@PathVariable("orderId") String orderId,@RequestBody(required = false) Map<String,String> resParams) {
        log.debug("[/db/getReqPayinfoByOrderId/{orderId}]模拟数据库端返回-请求支付信息:" + printMemory());
        Bill bill = billService.getByAPI_ORDER_ID(orderId);
        if(null!=resParams && resParams.containsKey("ip") && null!=bill){ //模拟db端的操作，接受rest 传递的参数，ip，2017.12.25
            bill.setAPI_Client_IP(resParams.get("ip"));
        }
        if(null!=bill){
            ReqPayInfo reqPayInfo = new ReqPayInfo(bill.getAPI_ORDER_FROM(),bill.getAPI_OID(),bill.getAPI_Client_IP(),bill.getAPI_JUMP_URL_PREFIX(),bill.getAPI_WEB_URL(),bill.getAPI_OTHER_PARAM(),bill.getAPI_KEY(), bill.getAPI_PUBLIC_KEY(),bill.getAPI_MEMBERID(), bill.getAPI_AMOUNT().toString(), bill.getAPI_ORDER_ID(), String.valueOf(bill.getAPI_ORDER_TIME().getTime()), bill.getAPI_CHANNEL_BANK_NAME(), bill.getAPI_TIME_OUT().toString(), bill.getAPI_ORDER_STATE(), bill.getAPI_NOTIFY_URL_PREFIX(),bill.getAPI_MEMBER_PLATFORMID());
            // String reqPayInfoJsonString = JSON.toJSONString(reqPayInfo);
            reqPayInfo.setAPI_ORDER_ID(dc.pay.admin.core.util.DateUtil.getAllTimeWithRandomNum());  //开发测试：请求支付改变订单号
            return reqPayInfo;
        }
        return null;
    }



    //通知支付结果
    @RequestMapping(value = "/db/DBreceive/",method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public Map DbReceive(@RequestBody(required = false) Map<String, String> responseMap) throws InterruptedException {
        // Thread.sleep(1000L*3);  //模拟数据库处理
        //Map result   =dbResponseResult(false);
        //Map result =dbResponseResult(true);
        //Map result =  System.currentTimeMillis()%3==0?null:dbResponseResult(System.currentTimeMillis()%2==0?true:false);  //模拟数据库处理结果返回，有时成功，有时失败
        Map result = dbResponseResult(System.currentTimeMillis()%2==0?true:false);  //模拟数据库处理结果返回，有时成功，有时失败
        log.debug("----------------------------------------------------------------------------------------");
        log.debug("数据库接口已收到[付款结果]通知：内容入下");
        log.debug("Json:"+ JSON.toJSONString(responseMap));
        for (Map.Entry<String, String> entry : responseMap.entrySet()) {
            log.debug("键= " + entry.getKey() + " ,值= "+ entry.getValue());
        }
        log.debug("数据库接口返回：数据处理结果："+JSON.toJSONString(result));
        log.debug("----------------------------------------------------------------------------------------");
        return result ;

    }


    //通知代付结果
    @RequestMapping(value = "/db/DBreceiveDaifu/",method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public Map DBreceiveDaifu(@RequestBody(required = false) Map<String, String> responseMap,HttpServletRequest request) throws InterruptedException {
        log.debug("[/db/DBreceiveDaifu/]数据库接口已收到[代付结果]通知Header:" + JSON.toJSONString(getHeadersInfo(request)));
        log.debug("[/db/DBreceiveDaifu/]数据库接口已收到[代付结果]通知内容:" + JSON.toJSONString(responseMap));
        Map result = dbResponseResult(System.currentTimeMillis()%2==0?true:false);  //模拟数据库处理结果返回，有时成功，有时失败
        log.debug("----------------------------------------------------------------------------------------");
        log.debug("数据库接口已收到[代付结果]通知：内容入下");
        log.debug("Json:"+ JSON.toJSONString(responseMap));
        for (Map.Entry<String, String> entry : responseMap.entrySet()) {
            log.debug("键= " + entry.getKey() + " ,值= "+ entry.getValue());
        }
        log.debug("数据库接口返回：数据处理结果："+JSON.toJSONString(result));
        log.debug("----------------------------------------------------------------------------------------");
        return result ;

    }





   //REST 接收付款结果to crk
    @RequestMapping(value = "/rest/DBreceive/",method = {RequestMethod.POST, RequestMethod.GET})  ///crk/toCrk
    @ResponseBody
    public Map RestReceive(@RequestBody(required = false) Map<String, String> responseMap) throws InterruptedException {
        Map result =  restResponseResult(System.currentTimeMillis()%2==0?true:false);  //模拟数据库处理结果返回，有时成功，有时失败
        log.debug("----------------------------------------------------------------------------------------");
        log.debug("REST接口已收到[付款结果]通知：内容入下");
        log.debug("Json:"+ JSON.toJSONString(responseMap));
        log.debug("返回：数据处理结果："+JSON.toJSONString(result));
        log.debug("----------------------------------------------------------------------------------------");
        return result ;

    }



//{"result":0,"msg":"\n### Error updating database.  Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction\n### The error may involve com.fafafa.lboss.dao.UserDao.updateVersion-Inline\n### The error occurred while setting parameters\n### SQL: UPDATE t_user SET version = version + 1 WHERE oid = 256 AND id = ? AND oid = ?\n### Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction\n; SQL []; Deadlock found when trying to get lock; try restarting transaction; nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction"}
    private Map dbResponseResult(boolean b) {
        Map<String, String> dbResponse = Maps.newHashMap();
         if(b){
             dbResponse.put("result","1");
         }else{
             dbResponse.put("result","0");
            // dbResponse.put("msg","处理失败");
             dbResponse.put("msg","\\n### Error updating database.  Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction\\n### The error may involve com.fafafa.lboss.dao.UserDao.updateVersion-Inline\\n### The error occurred while setting parameters\\n### SQL: UPDATE t_user SET version = version + 1 WHERE oid = 256 AND id = ? AND oid = ?\\n### Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction\\n; SQL []; Deadlock found when trying to get lock; try restarting transaction; nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction");
         }
        return  dbResponse;
    }


    private Map restResponseResult(boolean b) {
        Map<String, String> dbResponse = Maps.newHashMap();
        if(b){
            dbResponse.put("code","200");
            dbResponse.put("data","1");
        }else{
            dbResponse.put("code","0");
            dbResponse.put("data","0");
            // dbResponse.put("msg","处理失败");
           // dbResponse.put("msg","\\n### Error updating database.  Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction\\n### The error may involve com.fafafa.lboss.dao.UserDao.updateVersion-Inline\\n### The error occurred while setting parameters\\n### SQL: UPDATE t_user SET version = version + 1 WHERE oid = 256 AND id = ? AND oid = ?\\n### Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction\\n; SQL []; Deadlock found when trying to get lock; try restarting transaction; nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction");
        }
        dbResponse.put("msg",null);

        return  dbResponse;
    }


    /**
     * 输出内存信息
     */
    public static String printMemory(){
        long maxMemory = Runtime.getRuntime().maxMemory()/1024/1024;//最大可用内存 -Xmx
        long freeMemory = Runtime.getRuntime().freeMemory()/1024/1024;//可用内存
        long totalMemory = Runtime.getRuntime().totalMemory()/1024/1024;
        return  "maxMemory: "+maxMemory+" ,freeMemory:"+freeMemory+" ,totalMemory:"+totalMemory;
    }





    /**
     * 第三方接受rest消息
     */
    @RequestMapping(value = "/{path}/*",method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public  void haveReceived(@PathVariable("path") String path, @RequestBody(required = false) Map<String,Object> resParams, HttpServletRequest request, HttpServletResponse response) {
        log.error("===================================================================================");
        log.error("[模拟第三方收到信息] 请求地址：   "+JSON.toJSONString(request.getRequestURL())+",path:"+path);
        log.error("[模拟第三方收到信息] 请求方法：   "+(request.getMethod()));
        log.error("[模拟第三方收到信息] Head内容：  "+JSON.toJSONString(getHeadersInfo(request)));
        log.error("[模拟第三方收到信息] 内容体Map:  "+JSON.toJSONString(resParams));
        log.error("===================================================================================");
    }


    private Map<String, String> getHeadersInfo(HttpServletRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        return map;
    }















    /**
     * 请求代付信息-模拟数据库返回
     */
    @RequestMapping(value = "/db/getReqDaifuinfoByOrderId/{orderId}",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public  ReqDaifuInfo getReqDaifuinfoByOrderId(@PathVariable("orderId") String orderId,@RequestBody(required = false) Map<String,String> resParams,HttpServletRequest request) {
        log.debug("[/db/getReqDaifuinfoByOrderId/{orderId}]模拟数据库端返回-请求[代付]参数:" +JSON.toJSONString(resParams));
        log.debug("[/db/getReqDaifuinfoByOrderId/{orderId}]模拟数据库端返回-请求[代付]Header:" + JSON.toJSONString(getHeadersInfo(request)));
        Bill bill = billService.getByAPI_ORDER_ID(orderId);
        if(null!=resParams && resParams.containsKey("ip") && null!=bill){ //模拟db端的操作，接受rest 传递的参数，ip，2017.12.25
            bill.setAPI_Client_IP(resParams.get("ip"));
        }
        if(null!=bill){
            ReqDaifuInfo reqDaifuInfo = new ReqDaifuInfo(bill.getAPI_ORDER_ID());

            reqDaifuInfo.setAPI_OID(bill.getAPI_OID());
            reqDaifuInfo.setAPI_Client_IP(bill.getAPI_Client_IP());
            reqDaifuInfo.setAPI_OTHER_PARAM(bill.getAPI_OTHER_PARAM());
            reqDaifuInfo.setAPI_KEY(bill.getAPI_KEY());
            reqDaifuInfo.setAPI_PUBLIC_KEY(bill.getAPI_PUBLIC_KEY());
            reqDaifuInfo.setAPI_MEMBERID(bill.getAPI_MEMBERID());
            reqDaifuInfo.setAPI_AMOUNT(bill.getAPI_AMOUNT().toString());
            reqDaifuInfo.setAPI_OrDER_TIME( String.valueOf(bill.getAPI_ORDER_TIME().getTime()));
            reqDaifuInfo.setAPI_CHANNEL_BANK_NAME(bill.getAPI_CHANNEL_BANK_NAME());
            reqDaifuInfo.setAPI_ORDER_STATE(bill.getAPI_ORDER_STATE());
            reqDaifuInfo.setAPI_NOTIFY_URL_PREFIX(bill.getAPI_NOTIFY_URL_PREFIX());

            reqDaifuInfo.setAPI_CUSTOMER_ACCOUNT(bill.getAPI_CUSTOMER_ACCOUNT());
            reqDaifuInfo.setAPI_CUSTOMER_BANK_NAME(bill.getAPI_CUSTOMER_BANK_NAME());
            reqDaifuInfo.setAPI_CUSTOMER_BANK_BRANCH(bill.getAPI_CUSTOMER_BANK_BRANCH());
            reqDaifuInfo.setAPI_CUSTOMER_BANK_SUB_BRANCH(bill.getAPI_CUSTOMER_BANK_SUB_BRANCH());
            reqDaifuInfo.setAPI_CUSTOMER_BANK_NUMBER(bill.getAPI_CUSTOMER_BANK_NUMBER());
            reqDaifuInfo.setAPI_CUSTOMER_NAME(bill.getAPI_CUSTOMER_NAME());

            // String reqPayInfoJsonString = JSON.toJSONString(reqPayInfo);
            reqDaifuInfo.setAPI_ORDER_ID(dc.pay.admin.core.util.DateUtil.getAllTimeWithRandomNum());  //开发测试：请求代付改变订单号
            return reqDaifuInfo;
        }
        return null;
    }



}