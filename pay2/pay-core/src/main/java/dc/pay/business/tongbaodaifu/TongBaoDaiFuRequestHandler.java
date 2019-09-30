package dc.pay.business.tongbaodaifu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;


/**
 * 
 * @author andrew
 * Aug 6, 2019
 */
@RequestDaifuHandler("TONGBAODAIFU")
public final class TongBaoDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongBaoDaiFuRequestHandler.class);

    //通宝商户下单
    //接口地址    请求方式    接口说明
    //http://服务器ip/pay.api/api/withdraw   GET/POST    商户代付下单接口
    //请求参数
    //字段名 变量名 必填  类型  描述
    //商户ID    appId   是   String  商户唯一标识
   private static final String appId                ="appId";
    //银行卡号    bankCardNo  是   String  需要代付的银行卡号码
   private static final String bankCardNo                ="bankCardNo";
    //银行卡姓名   bankCardName    是   String  需要代付的银行卡姓名    bankCardName是银行卡名称
   private static final String bankCardName                ="bankCardName";
   //灰太狼 2019/8/2 13:57:44   持卡人姓名
   private static final String bankUserName                ="bankUserName";
    //商户订单号   apiTradeNo  是   String  商户系统内部订单号，要求32个字符内，只能是数字、大小写字母_-|*且在同一个商户号下唯一。
   private static final String apiTradeNo                ="apiTradeNo";
    //代付金额    money   是   String  代付金额（单位：元）
   private static final String money                ="money";
    //回调地址    notifyUrl   否   String  请求回调结果地址
   private static final String notifyUrl                ="notifyUrl";
    //签名  sign    是   String  请求参数RSA2私钥签名
//   private static final String sign                ="sign";

    //通宝商户查询代付记录
    //接口地址    请求方式    接口说明
    //http://服务器ip/pay.api/api/search GET/POST    商户查询代付记录
    //请求参数
    //字段名 变量名 必填  类型  描述
//    //商户ID    appId   是   String  商户唯一标识
//    private static final String  appId               = "appId";
//    //商户订单号   apiTradeNo  否   String  商户系统内部订单号，要求32个字符内，只能是数字、大小写字母_-|*且在同一个商户号下唯一。（与订单号二选一）
//    private static final String  apiTradeNo               = "apiTradeNo";
//    //订单号 tradeNo 否   String  商户平台订单号（与商户订单号二选一）
//    private static final String  tradeNo               = "tradeNo";
//    //签名  sign    是   String  请求参数RSA私钥签名
//    private static final String  sign               = "sign";
    
    
    //签名  sign    String  是   签名数据，签名规则见附录
//    private static final String  sign               = "sign";




    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(appId,channelWrapper.getAPI_MEMBERID());
                payParam.put(bankCardNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(bankUserName,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(bankCardName,channelWrapper.getAPI_CUSTOMER_BANK_NAME());
                payParam.put(apiTradeNo,channelWrapper.getAPI_ORDER_ID());
                payParam.put(money,handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());

//                String pay_md5sign = RSAUtil.rsaSign(Utils.pairListToMap(params), channelWrapper.getAPI_KEY());
                String pay_md5sign = RSAUtil.rsaSign(payParam, channelWrapper.getAPI_KEY());
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                //发送请求获取结果
//                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");
//                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], JSON.toJSONString(payParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE);

                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
//                addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());

                System.out.println("代付请求地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]);
                System.out.println("代付请求返回==>"+resultStr);
                System.out.println("代付请求参数==>"+JSON.toJSONString(payParam));
                
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
       if(1==2) throw new PayException("[通宝代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(appId,channelWrapper.getAPI_MEMBERID());
            payParam.put(apiTradeNo,channelWrapper.getAPI_ORDER_ID());
            
            String pay_md5sign = RSAUtil.rsaSign(payParam, channelWrapper.getAPI_KEY());
            
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            System.out.println("代付查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1]);
            System.out.println("代付查询返回==>"+resultStr);
            System.out.println("代付查询参数==>"+JSON.toJSONString(payParam));
            
//            Map<String,String> jsonToMap = JSONObject.parseObject(resultStr, Map.class);
//            {result={"result":"等待中","apiTradeNo":"20190802150352379603","orderAmount":"500.00","tradeNo":"OR2019080215035290662254","creatTime":"2019-08-02 15:03:53","appId":"201908011238401564634320973","sign":"XXzktaExDG8/fT/HEbYzDMLpf5i5RN+SkmLYgH5rBm+fkc38jDitmA9GZtM2afQbQ1ydqmwschAEclyCWuEvGaDkmMDeBwEHJuSmmICP4KR9wfvWxOeqyBX6IPXmE/Sdd5EMEaVlNn2Td1WavF/SyxSLyf3C5K/glCN7Py8yVzz5lLnynPyAzqdWMghhXIptBE3EX0eMXZmaYzIClvk1pM5eQ8O7cihdBWgWIuOSHqxQsjC0cfmRo8+gIphe9ZqnPbeFUaOSeWGOLioXRl9x18fOyC0gKLTBBQqIX88EWR0/LqFTxDI7ftKvSf+gnvrv1gCJhz1FX3xOMoeOnS0CVQ==","remark":"5.00","updateTime":"2019-08-02 15:03:53","status":"5"}, code=2000, failures=null, message=null, status=0}
            if(StringUtils.isNotBlank(resultStr) ){
                //这两个验证方法一样，我懒得修改了
//                my_result=  RsaUtil.validateSignByPublicKey(signStr, channelWrapper.getAPI_PUBLIC_KEY(), payParam.get(sign), "SHA256WithRSA");
                if (StringUtils.isNotBlank(JSONObject.parseObject(resultStr).getString("result")) && RSAUtil.rsaCheck(JSONObject.parseObject(JSONObject.parseObject(resultStr).getString("result"), Map.class), channelWrapper.getAPI_PUBLIC_KEY())) {
                    return getDaifuResult(JSONObject.parseObject(resultStr).getString("result"),true);
                }else{ throw new PayException(resultStr);}
            } else {
                throw new PayException(EMPTYRESPONSE);
            }
        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[通宝代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(appId,channelWrapper.getAPI_MEMBERID());
//            payParam.put(query_time,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));

            //生成md5
            String pay_md5sign = RSAUtil.rsaSign(payParam, channelWrapper.getAPI_KEY());
            
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
            
//            String pay_md5sign = null;
//            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
//            StringBuilder sb = new StringBuilder();
//            for (int i = 0; i < paramKeys.size(); i++) {
//                if(StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
//                    continue;
//                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
//            }
//            sb.append("key=" + channelWrapper.getAPI_KEY());
//            String signStr = sb.toString(); //.replaceFirst("&key=","")
//            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
//            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);



            //发送请求获取结果
//          String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
          String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam,"utf-8");
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

//                System.out.println("代付余额查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2]);
//                System.out.println("代付余额查询返回==>"+resultStr);
//                System.out.println("代付余额查询参数==>"+JSON.toJSONString(payParam));

            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"code","2000") && jsonObj.containsKey("result") && StringUtils.isNotBlank( jsonObj.getString("result"))  ){
                if (StringUtils.isNotBlank(JSONObject.parseObject(resultStr).getString("result")) && StringUtils.isNotBlank(JSONObject.parseObject(resultStr).getJSONObject("result").getString("userMoney"))) {
                    String balance =  JSONObject.parseObject(resultStr).getJSONObject("result").getString("userMoney");
                    return Long.parseLong(HandlerUtil.getFen(balance));
                }else{ throw new PayException(resultStr);}
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[通宝代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[通宝代付][代付余额查询]出错,错误:%s",e.getMessage()));
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
            //字段名 变量名 类型  描述
            //请求状态    code    String  请求返回的状态码
            //请求消息    message String  请求返回的消息内容
            //返回信息    result      返回信息，如非空，为订单信息
//            if( HandlerUtil.valJsonObj(jsonObj,"code","2000") &&  HandlerUtil.valJsonObj(jsonObj,"result","2000")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"code","2000") && jsonObj.containsKey("result") && StringUtils.isNotBlank(jsonObj.getString("result"))){
                JSONObject jsonObj2 = JSON.parseObject(jsonObj.getString("result"));
                if( HandlerUtil.valJsonObj(jsonObj2,"code","2000")) return PayEumeration.DAIFU_RESULT.PAYING;
            }
            if( HandlerUtil.valJsonObj(jsonObj,"code","4000","4001","4002","4003","4004","4005","4006","4007","4008","4009","4010","4011","4012","4013","4014","4015","4016","4017","4018","5000")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            //status;      // 订单状�?�（1-待支付，2-支付成功�?4-已取消）
            //订单状态值表
            //状态值 状态备注    状态说明
            //5   等待中 订单等待代付中
            //7   失败  订单代付失败
            //8   成功  订单代付成功
            //9   申请失败    申请代付失败
            //10  处理中 订单正在代付
            if( HandlerUtil.valJsonObj(jsonObj,"status","1","5","10")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"status","2","8")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if( HandlerUtil.valJsonObj(jsonObj,"status","4","7","9")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new PayException(resultStr);
        }
    }








}