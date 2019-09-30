package dc.pay.business.mayidaifu;

import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.mayidaifu.Test.Base64;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;


/**
 * 
 * @author andrew
 * Sep 26, 2019
 */
@RequestDaifuHandler("MAYIDAIFU")
public final class MaYiDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MaYiDaiFuRequestHandler.class);

    public static final String FLAG              ="MAYIDAIFU"; //商户账号
    
    //添加转账任务接口
    //请求地址:
    //http://putorder.daxiangpaypay.com/service/getway/index
    //请求方式:
    //POST
    //输入参数：
    //参数  说明  类型(长度)
    //params  加密参数(查看附签名规则)   string
    private static final String  params               = "params";
    //mcode   商户唯一标识  String(32)
    private static final String  mcode               = "mcode";
    //params加密前参数(json格式)：
    //参数  说明  类型(长度)
    //business    业务固定值:  Transfer    string
    private static final String  business               = "business";
    //business_type   业务编码:    10101  int
    private static final String  business_type               = "business_type";
    //api_sn  Api订单号  String(32)
    private static final String  api_sn               = "api_sn";
    //notify_url  异步通知地址(需urlencode编码)    String(255)
    private static final String  notify_url               = "notify_url";
    //money   转账金额    Float(6)
    private static final String  money               = "money";
    //bene_no 收款卡号    String(19)
    private static final String  bene_no               = "bene_no";
    //bank_id 收款银行卡编码(查看附bank_id) String(3) 
    private static final String  bank_id               = "bank_id";
    //payee   收款人(需urlencode编码)   String(16)
    private static final String  payee               = "payee";
    //timestamp   时间戳 int(10)
    private static final String  timestamp               = "timestamp";
    //sign    签名字符(查看附签名规则)   String(32)、
    private static final String  sign               = "sign";
    
    //响应参数定义：以 json 格式同步返回响应数据
    //参数名称    参数变量名   类型  必填  说明
    //提 交 数 据 是 否成功   status  String  是   fail失败success成功
    //签名  sign    String  是   签名数据，签名规则见附录
//    private static final String  sign               = "sign";

    //支付接口请求方式：支持 GET 或 POST 支付接口请求地址：
    //签名  sign    String  是   签名数据，签名规则见附录
//    private static final String  sign               = "sign";

    //支付接口请求方式：支持 GET 或 POST 支付接口请求地址：
    //签名  sign    String  是   签名数据，签名规则见附录
//    private static final String  sign               = "sign";




    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
            handlerUtil.saveStrInRedis(FLAG+":"+channelWrapper.getAPI_MEMBERID(), channelWrapper.getAPI_KEY(), 60*60*12*5);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[蚂蚁代付]-[请求代付]-1.向缓存中，存储密钥出错：{}",e.getMessage(),e);
            throw new PayException("[蚂蚁代付]-[请求代付]-1.向缓存中，存储密钥出错：{}",e);
        }
        
        try {
                payParam.put(business,"Transfer");
                payParam.put(business_type,"10101");
                payParam.put(api_sn,channelWrapper.getAPI_ORDER_ID());
                payParam.put(notify_url,URLEncoder.encode(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL(),"UTF-8"));
                payParam.put(money,handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(bene_no,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(bank_id,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(payee,URLEncoder.encode(channelWrapper.getAPI_CUSTOMER_NAME()));
                payParam.put(timestamp,StringToTimestamp(DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"))+"");

                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                sb.append("key=" + channelWrapper.getAPI_KEY());
                String signStr = sb.toString(); //.replaceFirst("&key=","")
                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                String encryptText = Test.encode(JSON.toJSONString(payParam),channelWrapper.getAPI_KEY());

                byte[] encryptData = encryptText.getBytes();
                String paramsStr = Base64.encode(encryptData);

                Map<String,String> map = new TreeMap<>();
                map.put(mcode, channelWrapper.getAPI_MEMBERID());
                map.put(params, paramsStr);
                
                //发送请求获取结果
//                System.out.println("代付请求地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]);
//                System.out.println("代付请求参数payParam==>"+JSON.toJSONString(payParam));
//                System.out.println("代付请求参数map==>"+JSON.toJSONString(map));
//                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], map);
//                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], JSON.toJSONString(map),MediaType.APPLICATION_JSON_VALUE);
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], map, String.class, HttpMethod.POST, defaultHeaders);
//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], map,"utf-8");
                resultStr = UnicodeUtil.unicodeToString(resultStr);
//                System.out.println("代付请求返回==>"+resultStr);
                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
//                addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());

                if(StringUtils.isNotBlank(resultStr) ){
                    return getDaifuResult(resultStr,false);
                }else{ throw new PayException(EMPTYRESPONSE);}


                //结束

        }catch (Exception e){
            e.printStackTrace();
            throw new PayException(e.getMessage());
        }
    }



    //查询代付
    //第三方确定转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    //第三方确定转账取消并不会再处理，返回 PayEumeration.DAIFU_RESULT.ERROR
    //如果第三方确定代付处理中，返回  PayEumeration.DAIFU_RESULT.PAYING
   // 其他情况抛异常
    @Override
    protected PayEumeration.DAIFU_RESULT queryDaifuAllInOne(Map<String, String> payParam,Map<String, String> details) throws PayException {
       if(1==2) throw new PayException("[蚂蚁代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(business,"Query");
            payParam.put(business_type,"20102");
            payParam.put(timestamp,StringToTimestamp(DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"))+"");
            payParam.put(api_sn,channelWrapper.getAPI_ORDER_ID());
            
            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append("key=" + channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            String encryptText = Test.encode(JSON.toJSONString(payParam),channelWrapper.getAPI_KEY());
//            System.out.println("3DES加密结果:\n" +encryptText);

            byte[] encryptData = encryptText.getBytes();
            String paramsStr = Base64.encode(encryptData);
//            System.out.println("再一次base64最后加密结果:\n" + paramsStr);
            Map<String,String> map = new TreeMap<>();
            map.put(mcode, channelWrapper.getAPI_MEMBERID());
            map.put(params, paramsStr);

            
            //发送请求获取结果
//            System.out.println("代付查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1]);
//            System.out.println("代付查询参数payParam==>"+JSON.toJSONString(payParam));
//            System.out.println("代付查询参数map==>"+JSON.toJSONString(map));
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], map,"utf-8");
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], map, String.class, HttpMethod.POST, defaultHeaders);
            
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

//            System.out.println("代付查询返回==>"+resultStr);
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[蚂蚁代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(business,"Query");
            payParam.put(business_type,"20106");
            payParam.put(timestamp,StringToTimestamp(DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"))+"");

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if(StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                    continue;
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append("key=" + channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);



            String encryptText = Test.encode(JSON.toJSONString(payParam),channelWrapper.getAPI_KEY());
//            System.out.println("3DES加密结果:\n" +encryptText);

            byte[] encryptData = encryptText.getBytes();
            String paramsStr = Base64.encode(encryptData);
//            System.out.println("再一次base64最后加密结果:\n" + paramsStr);
            Map<String,String> map = new TreeMap<>();
            map.put(mcode, channelWrapper.getAPI_MEMBERID());
            map.put(params, paramsStr);

            //发送请求获取结果
//                System.out.println("代付余额查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2]);
//                System.out.println("代付余额查询参数payParam==>"+JSON.toJSONString(payParam));
//                System.out.println("代付余额查询参数map==>"+JSON.toJSONString(map));
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], map);
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], map, String.class, HttpMethod.POST, defaultHeaders);
                
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

//                System.out.println("代付余额查询返回==>"+resultStr);

            JSONObject jsonObj = JSON.parseObject(resultStr);
            //如果有验证，最好调用一下验证方法
            if(HandlerUtil.valJsonObj(jsonObj,"status","true") && jsonObj.containsKey("data") && StringUtils.isNotBlank( jsonObj.getString("data")) &&
                    jsonObj.getJSONObject("data").containsKey("money_df") && StringUtils.isNotBlank( jsonObj.getJSONObject("data").getString("money_df"))
                    ){
                String balance =  jsonObj.getJSONObject("data").getString("money_df");
//                return Long.parseLong(balance);
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[蚂蚁代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[蚂蚁代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        //给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
        //因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
        //代付：点击请求代付 操作
        if(!isQuery){
            if( HandlerUtil.valJsonObj(jsonObj,"status","true")){
                return PayEumeration.DAIFU_RESULT.PAYING;
            //输出错误参数：（如果遇到5000和6000错误代码，请通过“查询转账任务接口”核实订单）
            }else if ( HandlerUtil.valJsonObj(jsonObj,"status","false") && HandlerUtil.valJsonObj(jsonObj,"errorCode","5000","6000","1005")) {
                return PayEumeration.DAIFU_RESULT.PAYING;
            //蚂蚁-技术-四米 2019/9/25 10:50:48
            //“🙈   10:39:28  查看原文
            //错误代码    状态说明
            //1000    请求参数错误
            //**********
            // 存1005，其他都表示提交失败
            }else if ( HandlerUtil.valJsonObj(jsonObj,"status","false") && HandlerUtil.valJsonObj(jsonObj,"errorCode","1000","1001","1002","1003","1004","1006","1007","1008","1009","1010","1011","2000","3000","9999","0000")) {
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            if( HandlerUtil.valJsonObj(jsonObj,"status","true")){
                JSONObject jsonObj2 = jsonObj.getJSONObject("data");
                //status    10:待执行,20:执行中,30:执行异常,40:失败结束,50:已完成    int
                if( HandlerUtil.valJsonObj(jsonObj2,"status","30")) return PayEumeration.DAIFU_RESULT.UNKNOW;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","40")) return PayEumeration.DAIFU_RESULT.ERROR;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","10","20")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","50")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                throw new PayException(resultStr);
            //输出错误参数：
            //{"status":false,"msg":"错误信息","data":"错误码"}
//            }else if ( HandlerUtil.valJsonObj(jsonObj,"status","false") && HandlerUtil.valJsonObj(jsonObj,"data","1000","1001","1002","2000","3000","9999","0000")) {
                //13:13:59
                //蚂蚁-技术-四米 2019/9/25 13:13:59
                //@🙈 查询的时候，只要是false都可以重新下发
            }else if ( HandlerUtil.valJsonObj(jsonObj,"status","false")) {
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            throw new PayException(resultStr);
        }

    }




    /**
     * String(yyyy-MM-dd HH:mm:ss)转10位时间戳
     * @param time
     * @return
     */
    public static Integer StringToTimestamp(String time){

    int times = 0;
    try {  
        times = (int) ((Timestamp.valueOf(time).getTime())/1000);  
    } catch (Exception e) {  
        e.printStackTrace();  
    }
    if(times==0){
        System.out.println("String转10位时间戳失败");
    }
        return times; 
        
    }



}