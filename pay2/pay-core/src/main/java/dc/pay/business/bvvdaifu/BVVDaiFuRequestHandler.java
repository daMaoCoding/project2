package dc.pay.business.bvvdaifu;

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
import dc.pay.utils.RestTemplateUtil;


/**
 * 
 * @author andrew
 * Sep 10, 2019
 */
@RequestDaifuHandler("BVVDAIFU")
public final class BVVDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BVVDaiFuRequestHandler.class);

    //字段名 变量名 必填  类型  说明
    //用户编号    usercode    是   String  用户编号
    private static final String  usercode               = "usercode";
    //商户订单号   customno    是   String  商户订单号
    private static final String  customno               = "customno";
    //提现金额    money   是   String  
    private static final String  money               = "money";
    //银行名称    bankcode    是   String  参考4.1 银行编码
    private static final String  bankcode               = "bankcode";
    //真实姓名    realname    是   String  
    private static final String  realname               = "realname";
    //身份证 idcard  是   String  
    private static final String  idcard               = "idcard";
    //银行卡号/账户 cardno  是   String  
    private static final String  cardno               = "cardno";
    //发送时间    sendtime    是   String  格式：yyyyMMddHHmmss    //例如：20170502144100
    private static final String  sendtime               = "sendtime";
    //通知地址    notifyurl   是   String  通知回调地址
    private static final String  notifyurl               = "notifyurl";
    //用户IP    buyerip 是   String  
    private static final String  buyerip               = "buyerip";
    //签名串 sign    是   String  签名结果
//    private static final String  sign               = "sign";

    //3.2.5 请求参数列表
    //请求Url: http://api.bvvonline.com/api/query
    //字段名 变量名 必填  类型  说明
    //业务参数
    //用户编号    usercode    是   String  用户编号
//    private static final String  usercode               = "usercode";
    //操作类型    opttype 是   String  1，查询付款订单，固定值
    private static final String  opttype               = "opttype";
    //商户订单号   customno    是   String  商户订单号
//    private static final String  customno               = "customno";
    //请求时间    sendtime    是   String  商户网站提交查询请求,必须为14位正整数数字,格式为:yyyyMMddHHmmss,如:20110707112233
//    private static final String  sendtime               = "sendtime";
    //数字签名    sign    是   String  请参考签名算法
//    private static final String  sign               = "sign";

//    //字段名 变量名 必填  类型  说明
//    //业务参数
//    //用户编号    usercode    是   String  用户编号
//    private static final String  usercode               = "usercode";
//    //操作类型    opttype 是   String  2，查询账户余额，固定值
//    private static final String  opttype               = "opttype";
//    //请求时间    sendtime    是   String  商户网站提交查询请求,必须为14位正整数数字,格式为:yyyyMMddHHmmss,如:20110707112233
//    private static final String  sendtime               = "sendtime";
//    //数字签名    sign    是   String  请参考签名算法
//    private static final String  sign               = "sign";
    


    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(usercode,channelWrapper.getAPI_MEMBERID());
                payParam.put(customno,channelWrapper.getAPI_ORDER_ID());
                payParam.put(money,handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(realname,channelWrapper.getAPI_CUSTOMER_NAME());
//                🙈 2019/9/10 10:38:13
//                我可随便写一个吗？
//                10:43:21
//                随风飞逝 2019/9/10 10:43:21
//                可以的
                payParam.put(idcard,"211000198710089377");
                payParam.put(cardno,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(sendtime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.put(buyerip,channelWrapper.getAPI_Client_IP());
                

                //生成md5
                StringBuffer signSrc= new StringBuffer();
                signSrc.append(payParam.get(usercode)).append("|");
                signSrc.append(payParam.get(customno)).append("|");
                signSrc.append(payParam.get(bankcode)).append("|");
                signSrc.append(payParam.get(cardno)).append("|");
                signSrc.append(payParam.get(idcard)).append("|");
                signSrc.append(payParam.get(money)).append("|");
                signSrc.append(payParam.get(sendtime)).append("|");
                signSrc.append(payParam.get(buyerip)).append("|");
                signSrc.append(channelWrapper.getAPI_KEY());
                //删除最后一个字符
                //signSrc.deleteCharAt(paramsStr.length()-1);
                String paramsStr = signSrc.toString();
//                System.out.println("签名源串=========>"+paramsStr);
                String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                //发送请求获取结果
//                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");
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
       if(1==2) throw new PayException("[BVV代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
              payParam.put(usercode,channelWrapper.getAPI_MEMBERID());
              payParam.put(opttype,"1");
              payParam.put(customno,channelWrapper.getAPI_ORDER_ID());
              payParam.put(sendtime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

              StringBuffer signSrc= new StringBuffer();
              signSrc.append(payParam.get(usercode)).append("|");
              signSrc.append(payParam.get(opttype)).append("|");
              signSrc.append(payParam.get(customno)).append("|");
              signSrc.append(payParam.get(sendtime)).append("|");
              signSrc.append(channelWrapper.getAPI_KEY());
              //删除最后一个字符
              //signSrc.deleteCharAt(paramsStr.length()-1);
              String paramsStr = signSrc.toString();
//              System.out.println("签名源串=========>"+paramsStr);
              String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
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
        if(1==2) throw new PayException("[BVV代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(usercode,channelWrapper.getAPI_MEMBERID());
            payParam.put(opttype,"2");
            payParam.put(sendtime,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString2));


            StringBuffer signSrc= new StringBuffer();
            signSrc.append(payParam.get(usercode)).append("|");
            signSrc.append(payParam.get(opttype)).append("|");
            signSrc.append(payParam.get(sendtime)).append("|");
            signSrc.append(channelWrapper.getAPI_KEY());
            //删除最后一个字符
            //signSrc.deleteCharAt(paramsStr.length()-1);
            String paramsStr = signSrc.toString();
//            System.out.println("签名源串=========>"+paramsStr);
            String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
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
            if(HandlerUtil.valJsonObj(jsonObj,"success","true") && jsonObj.containsKey("data") && StringUtils.isNotBlank( jsonObj.getString("data"))  &&
                    jsonObj.getJSONObject("data").containsKey("balance") && StringUtils.isNotBlank( jsonObj.getJSONObject("data").getString("balance")) ){
                String balance =   jsonObj.getJSONObject("data").getString("balance");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[BVV代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[BVV代付][代付余额查询]出错,错误:%s",e.getMessage()));
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
            
            //说明：result ：返回结果为success为ture表示接口调用成功，false 表示接口调用失败
            if( HandlerUtil.valJsonObj(jsonObj,"success","false")) return PayEumeration.DAIFU_RESULT.ERROR;
            if( "true".equals(jsonObj.getString("success")) && HandlerUtil.valJsonObj(jsonObj,"resultCode","10000")) return PayEumeration.DAIFU_RESULT.PAYING;
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            //调用成功：{"success":true,"resultCode":null,"resultMsg":null,"data":{***},"exception":false}
            if("true".equals(jsonObj.getString("success"))){
                JSONObject jsonObj2 = jsonObj.getJSONObject("data");
                //状态: 3打款成功,4打款失败，其他都是处理中
                if( HandlerUtil.valJsonObj(jsonObj2,"status","4")) return PayEumeration.DAIFU_RESULT.ERROR;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","3")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if(true) return PayEumeration.DAIFU_RESULT.PAYING;
                throw new PayException(resultStr);
            }else if ( HandlerUtil.valJsonObj(jsonObj,"success","false") && StringUtils.isNoneBlank(jsonObj.getString("resultCode"))) {
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            throw new PayException(resultStr);
        }

    }








}