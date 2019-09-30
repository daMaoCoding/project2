package dc.pay.business.bibaodaifu;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.RestTemplateUtil;


/**
 * 
 * @author andrew
 * May 17, 2019
 */
@RequestDaifuHandler("BIBAODAIFU")
public final class BiBaoDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BiBaoDaiFuRequestHandler.class);

    //公共参数
    //version 版本号 String(10) Y 固定 v1.0.3
    private static final String  version               = "version";
    //merchantId 商户号 String(32) Y 平台分配的商户号
    private static final String  merchantId               = "merchantId";
    //charset 字符串编码 String(10) Y UTF-8,GBK,GB2312 等 
    private static final String  charset               = "charset";
    //signType 签名算法 String(12) Y RSA 目前只支持 RSA
    private static final String  signType               = "signType";
    //cipher 业务数据密文 text Y 业务参数加密获得
    private static final String  cipher               = "cipher";
    //remark 其它信息 String(30) N 商户请求的其它信息
//    private static final String  remark               = "remark";
    //sign  签名参数 text Y  公共参数签名获得
    private static final String  sign               = "sign";
    
    //业务参数 (组成 JSON 字符串，RSA 加密放入 cipher)
    //reqNo 商户请求流水号 String(50) Y 商户请求流水，需保证在商户 端不重复。只能包含字符、数 据、下划线。
    private static final String  reqNo               = "reqNo";
    //reqTime 提交时间 String(20) Y 格式:yyyyMMddHHmmss 如：20171126180510
    private static final String  reqTime               = "reqTime";
    //transAmt 交易金额 Int Y 订单总金额，单位为分
    private static final String  transAmt               = "transAmt";
    //acctName 收款人 String(30) Y 
    private static final String  acctName               = "acctName";
    //acctNo 收款人银行账号 String(30) Y 
    private static final String  acctNo               = "acctNo";
    //acctType 对公对私类型 Int N 1-对私，2-对公 为空默认对私
    private static final String  acctType               = "acctType";
    //bankCode 开户行代码 String(10) Y 收款人开户行代码，如 ICBC 工商银行 参见《附录：银行代码对照 表》。  
    private static final String  bankCode               = "bankCode";
    //province 开户行省份 String(30) N 预留 
    private static final String  province               = "province";
    //city 开户行城市 String(30) N 预留
    private static final String  city               = "city";
    //branchBankName 支行名称 String(32) N 预留
    private static final String  branchBankName               = "branchBankName";
    //notifyUrl 异步通知地址 String(200) 公网能访问到
    private static final String  notifyUrl               = "notifyUrl";
    
    //2.2 代付结果查询接口
    //字段名 中文名 类型 类型 说明 公共参数 
    //version 版本号 String(10) Y   固定 v1.0.3 
    //merchantId 商户号 String(32) Y   平台分配的商户号 
    //charset 字符串编码 String(10) Y   UTF-8,GBK,GB2312 等 
    //signType  签名算法 String(12) Y   RSA 目前只支持 RSA 
    //reqNo  原商户请求流水号 String(50) Y    要查询的原商户请求流水号 
    //remark  其它信息 String(30) N   商户请求的其它信息 
    //sign  签名参数 text Y  公共参数签名获得
    
    //2.4 余额查询 2.4.1
    //字段名 中文名 类型 类型 说明 公共参数 
    //version 版本号 String(10) Y   固定 v1.0.3 
    //merchantId 商户号 String(32) Y   平台分配的商户号 
    //cipher  业务数据密文 Text Y  业务参数加密获得 
    //signType 签名算法 String(12) Y   RSA 目前只支持 RSA 
    //sign  签名参数 Text Y  公共参数签名获得



    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {

                payParam.put(version,"v1.0.3");
                payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
                payParam.put(charset,"UTF-8");
                payParam.put(signType,"RSA");
                
                Map<String, String> param = new TreeMap<String, String>();

                param.put(reqNo,channelWrapper.getAPI_ORDER_ID());
                param.put(reqTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                param.put(transAmt,channelWrapper.getAPI_AMOUNT());
                param.put(acctName,channelWrapper.getAPI_CUSTOMER_NAME());
                param.put(acctNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                param.put(acctType,"1");
                param.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                param.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                param.put(province,"");
                param.put(city,"");
                param.put(branchBankName,"");

                //加密业务数据
                String contentJSON =  JSONObject.toJSONString(param);
                
//                byte[] encryptByPublicKey = RuiJieTongUtil.encryptByPublicKey(contentJSON.getBytes(), channelWrapper.getAPI_PUBLIC_KEY());
//                payParam.put(cipher,new String(encryptByPublicKey));
                
                String my_cipher = PayUtils.encrypt(contentJSON,channelWrapper.getAPI_PUBLIC_KEY());
                payParam.put(cipher,my_cipher);


                //签名
                String pay_md5sign = PayUtils.sign(payParam,channelWrapper.getAPI_KEY());
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
//                System.out.println("代付请求地址==>"+JSON.toJSONString(channelWrapper));
//                System.out.println("代付请求地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
//                System.out.println("代付请求地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]);
                //发送请求获取结果
//                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");

                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
//                addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());

//                System.out.println("代付请求返回==>"+resultStr);
//                System.out.println("代付请求参数==>"+JSON.toJSONString(payParam));
                
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
       if(1==2) throw new PayException("[币宝代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            //2.2 代付结果查询接口
            //字段名 中文名 类型 类型 说明 公共参数 
            // 版本号 String(10) Y   固定 v1.0.3 
            // 商户号 String(32) Y   平台分配的商户号 
            // 字符串编码 String(10) Y   UTF-8,GBK,GB2312 等 
            //  签名算法 String(12) Y   RSA 目前只支持 RSA 
            //  原商户请求流水号 String(50) Y    要查询的原商户请求流水号 
            //remark  其它信息 String(30) N   商户请求的其它信息 
            //sign  签名参数 text Y  公共参数签名获得
            payParam.put(version,"v1.0.3");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(charset,"UTF-8");
            payParam.put(signType,"RSA");
            payParam.put(reqNo,channelWrapper.getAPI_ORDER_ID());
            
            //生成md5
            String pay_md5sign = PayUtils.sign(payParam,channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[币宝代付][代付余额查询]该功能未完成。");

        try {
            
            //组装参数
            //2.4 余额查询 2.4.1
            //字段名 中文名 类型 类型 说明 公共参数 
            // 版本号 String(10) Y   固定 v1.0.3 
            // 商户号 String(32) Y   平台分配的商户号 
            //cipher  业务数据密文 Text Y  业务参数加密获得 
            // 签名算法 String(12) Y   RSA 目前只支持 RSA 
            //sign  签名参数 Text Y  公共参数签名获得
            payParam.put(version,"v1.0.3");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(signType," RSA");

            //生成md5
            String pay_md5sign = PayUtils.sign(payParam,channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"code","00000")) {
                //业务数据
                String bizContent = PayUtils.decrypt(jsonObj.getString("cipher"),channelWrapper.getAPI_KEY());
                JSONObject jsonObj2 = JSON.parseObject(bizContent);
                String balance =  jsonObj2.getString("balance");
                return Long.parseLong(handlerUtil.getFen(balance));                
            }else{ throw new PayException(resultStr);}
            
        } catch (Exception e){
            log.error("[币宝代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[币宝代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        if(!isQuery){
            JSONObject jsonObj = JSON.parseObject(resultStr);
            JSONObject jsonObj2 = null;
            if (HandlerUtil.valJsonObj(jsonObj,"code","00000")) {
              //业务数据
                try {
                    jsonObj2 = JSON.parseObject(PayUtils.decrypt(jsonObj.getString(cipher),channelWrapper.getAPI_KEY()));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    log.error("[币宝代付][代付请求]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(resultStr),e);
                    throw new PayException( String.format("[币宝代付][代付请求]出错,错误:%s",e.getMessage()));
                }
                //status 代付状态 Int Y 该笔代付状态： 0-处理中 1-支付成功 2-支付失败，己退汇 该返回状态为最终银行处理交易 结果。
//                if( HandlerUtil.valJsonObj(jsonObj2,"status","0","1")) return PayEumeration.DAIFU_RESULT.PAYING;
                
                if( HandlerUtil.valJsonObj(jsonObj2,"status","0")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","2")) return PayEumeration.DAIFU_RESULT.ERROR;
            } else {
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            throw new  PayException(resultStr);
        }else{
          //Json格式转换Map
            Map<String,String> responseParams = (Map<String,String> )JSONObject.parseObject(resultStr, Map.class);
            String my_sign = responseParams.get(sign);
            responseParams.remove("sign");

            if(responseParams.get("code").equals("00000")) {
                JSONObject jsonObj2 = null;
                //业务数据
                try {
                    jsonObj2 = JSON.parseObject(PayUtils.decrypt(responseParams.get(cipher),channelWrapper.getAPI_KEY()));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    log.error("[币宝代付][代付查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(resultStr),e);
                    throw new PayException( String.format("[币宝代付][代付查询]出错,错误:%s",e.getMessage()));
                }
                //status 代付状态 Int Y 该笔代付状态： 0-处理中 1-支付成功 2-支付失败，己退汇 该返回状态为最终银行处理交易 结果。
                if( HandlerUtil.valJsonObj(jsonObj2,"status","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                
//                if( HandlerUtil.valJsonObj(jsonObj2,"status","0","2")) return PayEumeration.DAIFU_RESULT.ERROR;
                
                if( HandlerUtil.valJsonObj(jsonObj2,"status","0")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","2")) return PayEumeration.DAIFU_RESULT.ERROR;
            } else {
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            return PayEumeration.DAIFU_RESULT.ERROR;
        }

    }


    public static void main(String[] args) {

        /** 公共参数 **/
        Map<String,String> params = new HashMap<String,String>();
        //版本代码
        params.put("version", Config.VERSION);
        //商户号
        params.put("merchantId", Config.MERCHANTID);
        //编码
        params.put("charset", Config.CHARSET);
        //签名方式 
        params.put("signType", Config.SIGNTYPE);
        
        /** 业务参数 **/
//        Map<String,String> contentMap = WebUtils.getParameterMap(request);
        Map<String,String> contentMap = new TreeMap<String, String>(){
            {
                put("acctName","陈延章");
                put("acctNo","6222032107001060101");
                put("transAmt","10");
                put("bankCode","CCB");
                put("acctType","1");
                put("province","");
                put("city","");
                put("branchBankName","");
                
            }
        };
        
        //商户请求号(必须保证商户系统唯一) 
        contentMap.put("reqNo", String.valueOf(System.currentTimeMillis()));
        //请求时间  
        contentMap.put("reqTime",new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        //异步通知地址
        contentMap.put("notifyUrl", Config.notifyUrl);
        
        //加密业务数据
        String contentJSON =  JSONObject.toJSONString(contentMap);

        String my_cipher;
        try {
            my_cipher = PayUtils.encrypt(contentJSON,Config.PLATFORM_PUBLIC_KEY);
            //加密业务数据放入公共参数
            params.put("cipher", my_cipher);
            String my_sign = PayUtils.sign(params,Config.PRIVATE_KEY);
            params.put("sign", my_sign);
            //签名
            System.out.println("签名结果："+my_sign);
            System.out.println(params);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
     
        
//      String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
      String resultStr = RestTemplateUtil.postForm(Config.API_PAY, params,"utf-8");
      System.out.println("代付请求地址==>"+Config.API_PAY);
      System.out.println("代付请求返回==>"+resultStr);
      System.out.println("代付请求参数==>"+JSON.toJSONString(params));
        

      System.out.println("---------返回结果----------");
      System.out.println(resultStr);

      //Json格式转换Map
      Map<String,String> responseParams = (Map<String,String> )JSONObject.parseObject(resultStr, Map.class);
      
      //验签
      String my_sign = responseParams.get("sign");
      //移出签名信息
      responseParams.remove("sign");
      try {
        if(PayUtils.verify(responseParams,my_sign,Config.PLATFORM_PUBLIC_KEY)) {
              System.out.println("验签成功");
              if(responseParams.get("code").equals("00000")) {
                  //业务数据
                  String bizContent = PayUtils.decrypt(responseParams.get("cipher"),Config.PRIVATE_KEY);
                  
                  System.out.println("---------受理返回----------");
                  System.out.println(bizContent);

              } else {
                  System.err.append("受理失败，返回:"+responseParams.get("msg"));
              }
          } else {
              System.err.append("验签失败");
          }
    } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
  



    }
    



}