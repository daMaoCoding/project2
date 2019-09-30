package dc.pay.business.yunbaodaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;


@RequestDaifuHandler("YUNBAODAIFU")
public final class YunBaoDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YunBaoDaiFuRequestHandler.class);

     //请求代付&查询代付-参数
     private static final String  version = "version";   // 版本号
     private static final String  merchantId = "merchantId";   // 商户号
     private static final String  charset = "charset";   // 字符串编码
     private static final String  signType = "signType";   // 签名算法
     private static final String  cipher = "cipher";   // 业务数据密文
     private static final String  sign = "sign";   // 签名参数
     private static final String  reqNo = "reqNo";   // 商户请求流水号
     private static final String  reqTime = "reqTime";   // 提交时间
     private static final String  transAmt = "transAmt";   // 交易金额
     private static final String  acctName = "acctName";   // 收款人
     private static final String  acctNo = "acctNo";   // 收款人银行账号
     private static final String   acctType = "acctType";       //   1-对私，2-对公
     private static final String  bankCode = "bankCode";   // 开户行代码
     private static final String  notifyUrl = "notifyUrl";   // 异步通知地址
     private static final String  code = "code";
     private static final String  status = "status";





    private static final String  serviceName = "serviceName";       //   服务编码   str(8) 是 openTransferPay
    private static final String  merOrderNo = "merOrderNo";       //   代付单号   str(48) 是 商户代付订单号
    private static final String  currency = "currency";       //   签名   str(256) 是 签名内容




    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        String signRes = "";
        try {

              //业务参数
              Map<String,String> contentMap = Maps.newHashMap();
              contentMap.put(reqNo,channelWrapper.getAPI_ORDER_ID());
              contentMap.put(reqTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
              contentMap.put(transAmt,channelWrapper.getAPI_AMOUNT());
              contentMap.put(acctName,channelWrapper.getAPI_CUSTOMER_NAME());
              contentMap.put(acctNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
              contentMap.put(acctType,"1");
              contentMap.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              contentMap.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              String contentJSON =  JSONObject.toJSONString(contentMap);
              String cipherRes = YunBaoPayUtils.encrypt(contentJSON,channelWrapper.getAPI_PUBLIC_KEY());

                //组装参数
                payParam.put(version,"v1");
                payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
                payParam.put(charset,"UTF-8");
                payParam.put(signType,"RSA");
                payParam.put(cipher,cipherRes);

               //生成签名
                try{
                    signRes = YunBaoPayUtils.sign(payParam,channelWrapper.getAPI_KEY());
                }catch (Exception e){
                   log.error(e.getMessage(),e);
                    details.put(RESPONSEKEY, "[云宝代付]生成签名错误，请检查公钥私钥。");
                    return  PayEumeration.DAIFU_RESULT.ERROR;
                }
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signRes);




                //发送请求获取结果
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam, String.class, HttpMethod.POST);
                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

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
       if(1==2) throw new PayException("[云宝代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(version,"v1");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(charset,"UTF-8");
            payParam.put(signType,"RSA");
            payParam.put(reqNo,channelWrapper.getAPI_ORDER_ID());

            //生成签名
            String signRes = YunBaoPayUtils.sign(payParam,channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signRes);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[云宝代付][代付余额查询]该功能未完成。");

        try {
            //组装参数
            payParam.put(version,"v1");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(cipher,"");
            payParam.put(signType,"RSA");

            //生成签名
            String signRes = YunBaoPayUtils.sign(payParam,channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signRes);


            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,code,"00000") && StringUtils.isNotBlank(jsonObj.getString(cipher)) ){
                String bizContent="";
                if(StringUtils.isNotBlank(jsonObj.getString(cipher))) bizContent = YunBaoPayUtils.decrypt(jsonObj.getString(cipher),channelWrapper.getAPI_KEY());
                JSONObject bizContentJsonObj = JSON.parseObject(bizContent);
                //目前没考虑返回可用金额，还是余额(包含冻结金额)
                String balance = bizContentJsonObj.getString("balance");  //余额
                String freezeAmt = bizContentJsonObj.getString("freezeAmt");  //冻结金额
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[云宝代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[云宝代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        //提交失败返回：  {    "merchantId": "100000006",    "cipher": "",    "sign": "dIm4GGlpC5ISt4h9dHORKDl1QSd61uQ/Gn5F2tIaEGpa2GTO+1P4hoP819u2ZOTN1kzOBa1k9RafAMHZJ8Ihe7DipChLKMhWa+nZRjfCBNXMj+9ZkIVGpxmBtPpeOQROqyOvkd/7+kaL03qUU3ZizdM8qOolDuiqara2LzAXAzDjgEWdnnTPsJUhxX12Qey9ufsDYH3xPzYjDQOFrds7D05tIou6A9WnfYT1Gd/s+Ex2Kq1VtOl+anBK8E7Xp8Qk/mtfrfr0czOOzngkeJkL+RY8ubU3PovjvKiC3pfhIjHgwMJJKJl4+osymMVCOPTNWgfA/zh5g6lK5FENWWz0zQ==",    "code": "20005",    "msg": "余额不足"}
        //提交成功返回：  {    "merchantId": "100000006",    "cipher": "OsYKcfl2792hA/7EDxx9ivIXulL/skzZIKiQm1EanOSfRniDU2wV4Sr4Rs1H623BZi8EladxdwVMi8ia2IALdc43ZhkjJOwiLvI3kxQrPvwkblqfLuznzdLDGs8BoUDP9q7dLPZKUxfpFkDWY2ozf5cUPIeFioJi9zDMRQo9uhq+++QNi1XsGn9McGhYiEc4+8y0WeznyNP1o+nC62wDl1hXFrHlPXQQGBUsFN4LA0Mz3k5MceBk2bhDh6WYIHbLc0XSQFoYb0HEMX0DCw2X2Rrudsyz489aGAvyxQLrz3X33KZDKFA9ab7ykOqCFvYzdkLKTLXD424Dt21SbC4t9Q==",    "sign": "WhseEg5ko3IZbH3/3D20M6l7+CZ+3kczdQ1WKgJ7y9jMZCSimi+2c40lZYtnP60Gh+jKcL0GZb3++LTsNTXFKKqz/d+J36Cxfbn+ZodfcTajCzoeQPJlOBbXV8VJaB3MvY4jgmIkL8trgbTYo3CU2zV0uZAJPWHv//F6xixuecvy2au74FQgXd5n2PxskmniaclfBgkOb7yvVa4sm7WrUDx3EQxAg0FTwigTOYHl1BFh20dnfHcKGIr6SUpQ9Xmb7CE2LDoC6Z0Wqx+P7UNx7tu7UUssdFWZ0H4AkV4iYVGrUH4ga1eBKkOHg5juWXkYyQOaeXw2yCYWTkXLqoY2Rw==",    "code": "00000",    "msg": "受理成功"}
        Map<String,String> responseParams = (Map<String,String> )JSONObject.parseObject(resultStr, Map.class);
        String signResp = responseParams.get(sign);
        responseParams.remove(sign);
        String bizContent="";
        boolean b = false;

        //验证第三方返回签名
        try {
            b=YunBaoPayUtils.verify(responseParams,signResp,channelWrapper.getAPI_PUBLIC_KEY());
        } catch (Exception e) { throw new PayException("验证第三方签名错误，请核对我方后台填写的【公钥】是否正确。"); }

         if(b){
                if(responseParams.get(code).equals("00000")) {
                    //解密第三方返回内容,业务数据
                    //解密以后：{ "reqNo": "20190226142555048132",    "transNo": "10000000011184916",    "transAmt": 100,    "status": 0,    "transTime": "20190226142556"}
                    try {
                        if(StringUtils.isNotBlank(responseParams.get(cipher))) bizContent = YunBaoPayUtils.decrypt(responseParams.get(cipher),channelWrapper.getAPI_KEY());
                    } catch (Exception e) { throw new PayException("解密第三方返回数据错误，请核对我方后台填写的【私钥】是否正确。"); }
                    JSONObject jsonObj = JSON.parseObject(bizContent);
                    if(HandlerUtil.valJsonObj(jsonObj,status,"0")   ) return PayEumeration.DAIFU_RESULT.PAYING;   //第三方明确成功返回结果，0-处理中，1-支付成功，2-支付失败，己退汇
                    if(HandlerUtil.valJsonObj(jsonObj,status,"1")   ) return PayEumeration.DAIFU_RESULT.SUCCESS;
                    if(HandlerUtil.valJsonObj(jsonObj,status,"2")   ) return PayEumeration.DAIFU_RESULT.ERROR;
                    throw new  PayException(resultStr);   //第三返回未知值，订单是否提交成功/查询结果 都是未知
                } else {
                    if(isQuery){//查询
                        if(resultStr.contains("商户流水号不存在")) return PayEumeration.DAIFU_RESULT.ERROR; //查询时候第三方明确指定流水不存在则说明提交未到第三方，订单取消
                        throw new PayException(resultStr); //查询时候第三方未明确指定，订单状态失败，我们未知,抛异常返回unKnow
                    }
                    return PayEumeration.DAIFU_RESULT.ERROR;             //请求代付时候，第三方明确请求不成功，订单状态既为取消
                }
            } else {
                throw new PayException("验证第三方签名错误，请核对我方后台填写的【公钥】是否正确。"+resultStr);  //提交了，但是验证出错了，我们不知道订单是否提交成功。未知状态。
            }
    }








}