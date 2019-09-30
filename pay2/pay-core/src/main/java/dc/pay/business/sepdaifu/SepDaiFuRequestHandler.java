package dc.pay.business.sepdaifu;

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
import dc.pay.utils.RestTemplateUtil;


/**
 * 
 * @author andrew
 * Sep 20, 2019
 */
@RequestDaifuHandler("SEPDAIFU")
public final class SepDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SepDaiFuRequestHandler.class);

    //2.2.3.2 请求报文
    //签名方法    变量命名    数据长度    处理要求    说明
    //签名方法    signMethod  8   M   取值：MD5
    private static final String  signMethod               = "signMethod";
    //发送时间    sendTime    16  M   格式：yyyyMMddHHmmss
    private static final String  sendTime               = "sendTime";
    //商户号 merchantId  32  M   
    private static final String  merchantId               = "merchantId";
    //商户订单号   merOrderId  32  M   
    private static final String  merOrderId               = "merOrderId";
    //账号  accNo   64  M   银行卡卡号
    private static final String  accNo               = "accNo";
    //证件类型    certifTp    8   C   证件类型 01：身份证
//    private static final String  certifTp               = "certifTp";
    //证件号 certifyId   32  C   证件号
//    private static final String  certifyId               = "certifyId";
    //姓名  customerNm  10  M   姓名
    private static final String  customerNm               = "customerNm";
    //手机号 phoneNo 16  C   
//    private static final String  phoneNo               = "phoneNo";
    //开户支行名   issInsName  16  C   
//    private static final String  issInsName               = "issInsName";
    //银行编号    bankId  8   M   参考附录二
    private static final String  bankId               = "bankId";
    //联行号 accBankNo   32  C   
//    private static final String  accBankNo               = "accBankNo";
    //交易金额    txnAmt  10  M   单位分
    private static final String  txnAmt               = "txnAmt";
    //后台通知地址  backUrl 512 M   
    private static final String  backUrl               = "backUrl";
    //商品标题    subject 32  M   使用base64编码（生成签名算法时勿编码）
    private static final String  subject               = "subject";
    //商品描述    body    128 M   使用base64编码（生成签名算法时勿编码）
    private static final String  body               = "body";
    //对公对私标志  ppFlag  8   M   01：对私
    private static final String  ppFlag               = "ppFlag";
    //商户Ip    sendIp  32  C   
//    private static final String  sendIp               = "sendIp";
    //附加信息    msgExt  512 C   
//    private static final String  msgExt               = "msgExt";
    //保留域1    reserved1   512 C   
//    private static final String  reserved1               = "reserved1";
    //网关  gateway 8   M   代付：daifu
    private static final String  gateway               = "gateway";
    //签名信息    signature   64  M   
    private static final String  signature               = "signature";
    
    //参数名称    参数变量名   类型  必填  说明

    //响应参数定义：以 json 格式同步返回响应数据
    



    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(signMethod,"MD5");
                payParam.put(sendTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
                payParam.put(merOrderId,channelWrapper.getAPI_ORDER_ID());
                payParam.put(accNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(customerNm,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(bankId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(txnAmt,channelWrapper.getAPI_AMOUNT());
                payParam.put(backUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.put(subject,"1");
                payParam.put(body,"1");
                payParam.put(ppFlag,"01");
                payParam.put(gateway,"daifu");

                //生成md5
                String pay_md5sign = null;
                
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    if (!signMethod.equals(paramKeys.get(i)) && !signature.equals(paramKeys.get(i))) {
                        sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                    }
                }
                //删除最后一个字符
                sb.deleteCharAt(sb.length()-1);
                sb.append(channelWrapper.getAPI_KEY());
                String signStr = sb.toString(); //.replaceFirst("&key=","")
//                String signStr = "accNo=6217004160022335741&backUrl=http://66p.badej8888.com:30000/respDaifuWeb/SEPDAIFU_BANK_WEB_DF_CCB/&bankId=01050000&body=1&customerNm=王小军&gateway=daifu&merOrderId=20190918154721318172&merchantId=L1900067318&ppFlag=01&sendTime=20190918154721&subject=1&txnAmt=300764633882bf34f389816f6dc2a0affd0";
//                System.out.println("signStr===>"+signStr);
//                System.out.println("signStr,md5后===>"+HandlerUtil.getMD5UpperCase(signStr).toLowerCase());
//                System.out.println("signStr,md5,base64后1，小写===>"+Base64Utils.encryptBASE64(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes("utf-8")));
//                System.out.println("signStr,md5,base64后1，大写===>"+Base64Utils.encryptBASE64(HandlerUtil.getMD5UpperCase(signStr).getBytes("utf-8")));
//                System.out.println("signStr,md5,base64后2===>"+new String(java.util.Base64.getEncoder().encode(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes())));
                pay_md5sign = new String(java.util.Base64.getEncoder().encode(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes()));//BASE64加密
                
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
                payParam.put(subject,new String(java.util.Base64.getEncoder().encode("1".getBytes())));
                payParam.put(body,new String(java.util.Base64.getEncoder().encode("1".getBytes())));
                
                //发送请求获取结果
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");
//
//                System.out.println("代付请求地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]);
//                System.out.println("代付请求返回==>"+resultStr);
//                System.out.println("代付请求参数==>"+JSON.toJSONString(payParam));
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
       if(1==2) throw new PayException("[sep代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(merOrderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(sendTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(signMethod,"MD5");
            
            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (!signMethod.equals(paramKeys.get(i)) && !signature.equals(paramKeys.get(i))) {
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
          //删除最后一个字符
            sb.deleteCharAt(sb.length()-1);
            sb.append(channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
//            pay_md5sign = Base64Utils.encryptBASE64(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes());//BASE64加密;
            pay_md5sign = new String(java.util.Base64.getEncoder().encode(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes()));//BASE64加密
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
//            System.out.println("代付查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1]);
//            System.out.println("代付查询返回==>"+resultStr);
//            System.out.println("代付查询参数==>"+JSON.toJSONString(payParam));
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[sep代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(sendTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(signMethod,"MD5");

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (!signMethod.equals(paramKeys.get(i)) && !signature.equals(paramKeys.get(i))) {
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
          //删除最后一个字符
            sb.deleteCharAt(sb.length()-1);
            sb.append(channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
//            pay_md5sign = Base64Utils.encryptBASE64(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes());//BASE64加密
            pay_md5sign = new String(java.util.Base64.getEncoder().encode(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes()));//BASE64加密
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

//                System.out.println("代付余额查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2]);
//                System.out.println("代付余额查询返回==>"+resultStr);
//                System.out.println("代付余额查询参数==>"+JSON.toJSONString(payParam));

            JSONObject jsonObj = JSON.parseObject(resultStr);
            //如果有验证，最好调用一下验证方法
            if(HandlerUtil.valJsonObj(jsonObj,"success","1") && HandlerUtil.valJsonObj(jsonObj,"code","0") && 
                    jsonObj.containsKey("balance") && StringUtils.isNotBlank( jsonObj.getString("balance")) &&
                    jsonObj.getJSONObject("balance").containsKey("availableBalance") && StringUtils.isNotBlank(jsonObj.getJSONObject("balance").getString("availableBalance"))){
                String balance =  jsonObj.getJSONObject("balance").getString("availableBalance");
//                return Long.parseLong(balance);
                return Long.parseLong(balance);
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[sep代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[sep代付][代付余额查询]出错,错误:%s",e.getMessage()));
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
            //应答码 code    8   M   code=1111,代表代付进行中，具体代付成功或失败，请在异步回调或者订单查询操作。                    code=1002,代表代付失败
            //眼泪是无色の血液 2019/9/20 13:12:06
            //这样只有9999，1111，1001，0000，其他都可以判断代付失败，代付请求可以这样判断
            //@眼泪是无色の血液 你意思是，请求时，如果success=0，并且code=9999，则请求代付失败。需要重新下发一笔？
            if( HandlerUtil.valJsonObj(jsonObj,"success","0") && HandlerUtil.valJsonObj(jsonObj,"code","1002","9999")) return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"success","1") && HandlerUtil.valJsonObj(jsonObj,"code","1111","1001","0000")) return PayEumeration.DAIFU_RESULT.PAYING;
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            if( HandlerUtil.valJsonObj(jsonObj,"success","1") &&  HandlerUtil.valJsonObj(jsonObj,"code","0")){
                //f(success==1&&code==0){
                //    If(transStatus==1){
                //       交易成功
                //    }
                //    If(transStatus==2){
                //       交易失败 
                //    }
                //    If(transStatus==3){
                //     交易进行中
                //    }
                //    }else{
                //       交易失败
                //    }
                if( HandlerUtil.valJsonObj(jsonObj,"transStatus","2")) return PayEumeration.DAIFU_RESULT.ERROR;
                if( HandlerUtil.valJsonObj(jsonObj,"transStatus","3")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj,"transStatus","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                throw new PayException(resultStr);
            }else if ( HandlerUtil.valJsonObj(jsonObj,"success","0") && HandlerUtil.valJsonObj(jsonObj,"code","1002")) {
                //🙈 2019/9/20 11:08:38
                //你们查询 接口，太弱了吧？
                //圆圆3 2019/9/20 11:08:53
                //一分钟一次
                //所以，查询时不能返回错误给应用端，会造成重复出款
                return PayEumeration.DAIFU_RESULT.ERROR;
            }else if ( HandlerUtil.valJsonObj(jsonObj,"success","0")) {
                //所以，不能返回错误给应用端，会造成重复出款
                return PayEumeration.DAIFU_RESULT.PAYING;
            }
            throw new PayException(resultStr);
        }

    }





}