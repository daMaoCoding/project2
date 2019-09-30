package dc.pay.business.tiantiandaifu;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;


/**
 * 
 * @author andrew
 * Sep 19, 2019
 */
@RequestDaifuHandler("TIANTIANDAIFU")
public final class TianTianDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TianTianDaiFuRequestHandler.class);

    private static final String  FLAG               = "TIANTIANDAIFU:";
    
    //公共参数
    //请求参数
    //参数名 参数类型    参数说明    是否必填
    //groupId String  请求方的合作编号（邮件下发）  必填
    private static final String  groupId               = "groupId";
    //service String  请求的交易服务码    必填
    private static final String  service               = "service";
    //signType    String  签名类型（RSA）   必填
    private static final String  signType               = "signType";
    //datetime    String  系统时间（yyyyMMddHHmmss）    必填
    private static final String  datetime               = "datetime";
    //sign    String  数据的签名字符串    必填
//    private static final String  sign               = "sign";

    //请求参数
    //参数名 参数类型    参数说明    是否必填
    //merchantCode    String  平台商户编号  必填
    private static final String  merchantCode               = "merchantCode";
    //terminalCode    String  平台商户终端编号    必填
    private static final String  terminalCode               = "terminalCode";
    //orderNum    String  合作商订单号，全局唯一 必填
    private static final String  orderNum               = "orderNum";
    //transDate   String  交易日期（yyyyMMdd）  必填
    private static final String  transDate               = "transDate";
    //transTime   String  交易时间（HH24mmss）  必填
    private static final String  transTime               = "transTime";
    //accountName String  收款人账户名  必填
    private static final String  accountName               = "accountName";
    //bankCard    String  收款人账户号  必填
    private static final String  bankCard               = "bankCard";
    //bankName    String  收款人账户开户行名称  必填
    private static final String  bankName               = "bankName";
    //bankLinked  String  收款人账户开户行联行号 必填
    private static final String  bankLinked               = "bankLinked";
    //transMoney  String  交易金额    必填
    private static final String  transMoney               = "transMoney";
    //bankCode    String  银行编码    详询平台方
    private static final String  bankCode               = "bankCode";
    //phoneNum    String  银行预留手机号 详询平台方
//    private static final String  phoneNum               = "phoneNum";
    //bankProvinceName    String  开户行省    详询平台方
//    private static final String  bankProvinceName               = "bankProvinceName";
    //bankCityName    String  开户行市    详询平台方
//    private static final String  bankCityName               = "bankCityName";
    //idCard  String  身份证号    详询平台方
//    private static final String  idCard               = "idCard";
    //notifyUrl String  代付结果异步通知地址  必填
    private static final String  notifyUrl               = "notifyUrl";

    //代付结果查询 调试本接口前，请先阅读文档说明！！！
    //合作商通过调用此接口可以根据上送的订单号查询订单状态。 注意： 合作商根据公共参数中pl_code判断查询是否成功。 在pl_code的值为“0000”时根据pl_transState判断支付状态。 代付日期和代付时间指查询的代付订单的下单日期和下单时间。
    //请求参数
    //参数名 参数类型    参数说明    是否必填
//    //merchantCode    String  平台商户编号  必填
//    private static final String  merchantCode               = "merchantCode";
//    //orderNum    String  合作商订单号，全局唯一 必填
//    private static final String  orderNum               = "orderNum";
//    //transDate   String  代付日期（yyyyMMdd）  非必填
//    private static final String  transDate               = "transDate";
//    //transTime   String  代付时间（HHmmss）    非必填
//    private static final String  transTime               = "transTime";
    
    //查询余额 调试本接口前，请先阅读文档说明！！！
    //合作商通过调用此接口查询余额。
    //请求参数
//    //参数名 参数类型    参数说明    是否必填
//    //merchantCode    String  平台商户编号  必填
//    private static final String  merchantCode               = "merchantCode";
//    //orderNum    String  合作商订单号，全局唯一 必填
//    private static final String  orderNum               = "orderNum";
    

//    static {
//        String errorString = "Failed manually overriding key-length permissions.";
//        int newMaxKeyLength;
//        try {
//            if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
//                Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
//                Constructor con = c.getDeclaredConstructor();
//                con.setAccessible(true);
//                Object allPermissionCollection = con.newInstance();
//                Field f = c.getDeclaredField("all_allowed");
//                f.setAccessible(true);
//                f.setBoolean(allPermissionCollection, true);
//                c = Class.forName("javax.crypto.CryptoPermissions");
//                con = c.getDeclaredConstructor();
//                con.setAccessible(true);
//                Object allPermissions = con.newInstance();
//                f = c.getDeclaredField("perms");
//                f.setAccessible(true);
//                ((Map) f.get(allPermissions)).put("*", allPermissionCollection);
//                c = Class.forName("javax.crypto.JceSecurityManager");
//                f = c.getDeclaredField("defaultPolicy");
//                f.setAccessible(true);
//                Field mf = Field.class.getDeclaredField("modifiers");
//                mf.setAccessible(true);
//                mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
//                f.set(null, allPermissions);
//                newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(errorString, e);
//        }
//        if (newMaxKeyLength < 256)
//            throw new RuntimeException(errorString); // hack failed
//    }
    
    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
            log.error("[天天代付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantCode&请求方的合作编号groupId&平台商户终端编号terminalCode" );
            throw new PayException("[天天代付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantCode&请求方的合作编号groupId&平台商户终端编号terminalCode" );
        }
        
//        System.out.println("公钥存入时长：（天）"+(60*60*12*5));
        
        try {
            handlerUtil.saveStrInRedis(FLAG+channelWrapper.getAPI_MEMBERID().split("&")[1], channelWrapper.getAPI_PUBLIC_KEY(), 60*60*12*5);
                //组装参数
                payParam.put(merchantCode,channelWrapper.getAPI_MEMBERID().split("&")[0]);
               payParam.put(terminalCode,channelWrapper.getAPI_MEMBERID().split("&")[2]);
                payParam.put(orderNum,channelWrapper.getAPI_ORDER_ID());
                payParam.put(transDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                payParam.put(transTime,DateUtil.formatDateTimeStrByParam("HHmmss"));
                payParam.put(accountName,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(bankCard,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(bankName,channelWrapper.getAPI_CUSTOMER_BANK_NAME());
                payParam.put(transMoney,channelWrapper.getAPI_AMOUNT());
                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(bankLinked,"123");
                payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());

                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                String paramStr=sb.substring(0, sb.length()-1);
                
                pay_md5sign = ApiUtil.base64Encode(ApiUtil.encrypt(channelWrapper.getAPI_PUBLIC_KEY(), paramStr.getBytes("UTF-8")));

                Map<String, String> requestParam = new HashMap<String, String>();
                // 公共参数
               requestParam.put(groupId, channelWrapper.getAPI_MEMBERID().split("&")[1]);// 请求方的合作编号
               requestParam.put(service, "DF004");// 请求的交易服务码
               requestParam.put(signType, "RSA");// 签名类型（RSA）
               SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
               requestParam.put(datetime, sdf.format(new Date())); // 系统时间（yyyyMMddHHmmss）
               requestParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
               
                //发送请求获取结果
//               String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], requestParam,"utf-8");
               
//               String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], JSON.toJSONString(requestParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE);
//               String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], JSON.toJSONString(requestParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE);
//               String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], requestParam, String.class, HttpMethod.GET);
               String resultStr = ApiUtil.transaction2(requestParam, payParam, channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]);
               
//                System.out.println("代付请求返回resultStr==>"+resultStr);
//                System.out.println("代付请求地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]);
//                System.out.println("代付请求参数payParam==>"+JSON.toJSONString(payParam));
//                System.out.println("代付请求参数requestParam==>"+JSON.toJSONString(requestParam));
//                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
                details.put(RESPONSEKEY, StringUtils.isBlank(resultStr) ? "" : new String(ApiUtil.verify(channelWrapper.getAPI_PUBLIC_KEY(), ApiUtil.base64Decode(new String(JSONObject.parseObject(resultStr).getString("pl_sign").getBytes("ISO_8859_1")))), "utf-8"));//强制必须保存下第三方结果
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
       if(1==2) throw new PayException("[天天代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(merchantCode,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(orderNum,channelWrapper.getAPI_ORDER_ID());
            payParam.put(transDate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMdd"));
            payParam.put(transTime,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "HHmmss"));

            Map<String, String> requestParam = new HashMap<String, String>();
            // 公共参数
            requestParam.put(groupId, channelWrapper.getAPI_MEMBERID().split("&")[1]);// 请求方的合作编号
            requestParam.put(service, "DF003");// 请求的交易服务码
            requestParam.put(signType, "RSA");// 签名类型（RSA）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            requestParam.put(datetime, sdf.format(new Date())); // 系统时间（yyyyMMddHHmmss）

//            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
//            sb.append("key=" + channelWrapper.getAPI_KEY());
            String paramStr=sb.substring(0, sb.length()-1);
            pay_md5sign = ApiUtil.base64Encode(ApiUtil.encrypt(channelWrapper.getAPI_PUBLIC_KEY(), paramStr.getBytes("UTF-8")));
            requestParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], requestParam);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], requestParam,"utf-8");
//            System.out.println("代付查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1]);
//            System.out.println("代付查询返回responseMap==>"+responseMap);
//            System.out.println("代付查询参数payParam==>"+JSON.toJSONString(payParam));
//            System.out.println("代付查询参数requestParam==>"+JSON.toJSONString(requestParam));
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], requestParam,"utf-8");
            String resultStr = ApiUtil.transaction1(requestParam, payParam, channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1]);
//            System.out.println("代付查询返回==>"+resultStr);
            
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[天天代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            payParam.put(merchantCode,channelWrapper.getAPI_MEMBERID().split("&")[0]);
//            payParam.put(orderNum,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderNum,"CX" + sdf.format(new Date()));

            Map<String, String> requestParam = new HashMap<String, String>();
            // 公共参数
            requestParam.put(groupId, channelWrapper.getAPI_MEMBERID().split("&")[1]);// 请求方的合作编号
            requestParam.put(service, "CX001");// 请求的交易服务码
            requestParam.put(signType, "RSA");// 签名类型（RSA）
            requestParam.put(datetime, sdf.format(new Date())); // 系统时间（yyyyMMddHHmmss）

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            String paramStr=sb.substring(0, sb.length()-1);
            pay_md5sign = ApiUtil.base64Encode(ApiUtil.encrypt(channelWrapper.getAPI_PUBLIC_KEY(), paramStr.getBytes("UTF-8")));
            requestParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //调用ApiUtil方法，得到装有公共返回参数的map
          String resultStr = ApiUtil.transaction1(requestParam, payParam, channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2]);
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], requestParam, String.class, HttpMethod.GET);
          
            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], requestParam,"utf-8");
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], JSON.toJSONString(requestParam), MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            Map<String, String> urlParams = handlerUtil.getUrlParams("?"+resultStr);
            //如果有验证，最好调用一下验证方法
            if(urlParams.containsKey("pl_money2") && StringUtils.isNotBlank(urlParams.get("pl_money2"))  ){
                String balance =  urlParams.get("pl_money2");
                return Long.parseLong(balance);
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[天天代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[天天代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        //给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
        //因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
        //代付：点击请求代付 操作
        if(!isQuery){
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if( HandlerUtil.valJsonObj(jsonObj,"pl_code","0000")){
                JSONObject jsonObj2 = new JSONObject();
                try {
                    String responseSignStr = new String(ApiUtil.verify(channelWrapper.getAPI_PUBLIC_KEY(), ApiUtil.base64Decode(new String(jsonObj.getString("pl_sign").getBytes("ISO_8859_1")))), "utf-8");
                    String [] apiStr = responseSignStr.split("&");
                    for(String apiParamStr: apiStr){
                        String [] paramKeyValues=apiParamStr.split("=");
                        jsonObj2.put(paramKeyValues[0], paramKeyValues.length == 1 ? "" : apiParamStr.replace(paramKeyValues[0] + "=", ""));
                    }
                    
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    log.error("[天天代付]-[请求代付]-2.解密出错：{}",e.getMessage(),e);
                    throw new PayException(e.getMessage(),e);
                }
                //pl_transState String  交易状态（1-成功，2-失败，3-未明）
                //🙈 2019/9/19 14:49:15
//                所以，我能把请求后返回的3，在我平台设置成订单处理中吧？
//                mc 2019/9/19 14:49:23
//                对   处理中
//                if( HandlerUtil.valJsonObj(jsonObj2,"pl_transState","3")) return PayEumeration.DAIFU_RESULT.UNKNOW;
                if( HandlerUtil.valJsonObj(jsonObj2,"pl_transState","2")) return PayEumeration.DAIFU_RESULT.ERROR;
                if( HandlerUtil.valJsonObj(jsonObj2,"pl_transState","3")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj2,"pl_transState","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            }
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            JSONObject jsonObj = new JSONObject();
            String [] apiStr = resultStr.split("&");
            for(String apiParamStr: apiStr){
                String [] paramKeyValues=apiParamStr.split("=");
                jsonObj.put(paramKeyValues[0], paramKeyValues.length == 1 ? "" : apiParamStr.replace(paramKeyValues[0] + "=", ""));
            }
            //pl_transState   String  交易状态（1-成功，2-失败，3-未明）    必填
//            🙈 2019/9/19 15:29:45
//            查询 ，pl_transState=3也是处理中吧？
//            15:33:47
//            mc 2019/9/19 15:33:47
//            对
            if( HandlerUtil.valJsonObj(jsonObj,"pl_transState","3")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"pl_transState","2")) return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"pl_transState","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            throw new PayException(resultStr);
        }

       
    }

    

}