package dc.pay.business.htdaifu;

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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


@RequestDaifuHandler("HTDAIFU")
public final class HTDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HTDaiFuRequestHandler.class);

    /**
     *
     *
     *
     *  注意： 未完成，第三方ip一直未绑定成功。
     *
     *
     *
     *
     */



    //请求代付&查询代付-参数
     private static final String  serviceName = "serviceName";       //   服务编码   str(8) 是 openTransferPay
     private static final String  reqTime = "reqTime";       //   请求时间   str(32) 是 商户请求时间
     private static final String  merchantId = "merchantId";       //   商户号   str(12) 是 商户号
     private static final String  busType = "busType";       //   代付类型   str(8) 是 代付类型  PRV-对私，PUB-对公
     private static final String  merOrderNo = "merOrderNo";       //   代付单号   str(48) 是 商户代付订单号
     private static final String  orderAmount = "orderAmount";       //   交易金额   str(16) 是 以元为单位，精确到小数位
     private static final String  bankCode = "bankCode";       //   银行编号   str(16) 是 参照银行编码表
     private static final String  accountName = "accountName";       //   收款人   str(48) 是 收款人姓名
     private static final String  accountCardNo = "accountCardNo";       //   收款账号   str(48) 是 收款人银行账号
     private static final String  clientReqIP = "clientReqIP";       //   客户端IP   str(48) 是
     private static final String  notifyUrl = "notifyUrl";       //   异步通知地址   str(256) 是
     private static final String  signType = "signType";       //   签名方式   str(8) 是  签名方式：MD5或RSA
     private static final String  sign = "sign";       //   签名   str(256) 是 签名内容
     private static final String  currency = "currency";       //   签名   str(256) 是 签名内容




     private static final String  cmd = "cmd";    //	M(20)	接口命令字：DPAYAPI
     private static final String  hmac = "hmac";    //	M(32)	接口加密摘要，按ASC码顺序排列后MD5，不参与验签，参与签名的参数，cmd，version，apporderid，appid
     private static final String  apporderid = "apporderid";    //	M(20)	商户的订单号
     private static final String  appid = "appid";    //	M(20)	交易请求平台商户ID(由平台统一分配给外部商户的)
     private static final String  data = "data";    //	M(800)	加密过的密文，包含的参数如下
     private static final String  version = "version";    //	M(5)	接口版本号2.0



//  private static final String cmd = "cmd";   //	M(20)	接口命令字：DPAYAPI
//  private static final String version = "version";   //	M(5)	接口版本号2.0
//  private static final String appid = "appid";   //	M(20)	交易请求平台商户ID(由平台统一分配给外部商户的)
//  private static final String apporderid = "apporderid";   //	M(20)	商户的订单号
    private static final String userid = "userid";   //	M(32)	账户ID，(由平台统一分配给外部商户的)
    private static final String ordertime = "ordertime";   //	M(14)	交易时间，格式: yyyyMMddHHmmss
    private static final String orderbody = "orderbody";   //	M(20)	订单描述
    private static final String amount = "amount";   //	M(14,2)	金额（元）
    private static final String acctno = "acctno";   //	M(20)	银行卡号
    private static final String acctname = "acctname";   //	M(20)	银行卡持有者姓名
    private static final String bankname = "bankname";   //	M(20)	银行名称
    private static final String bankcode = "bankcode";   //	M(10)	银行编码，见字典
    private static final String notifyurl = "notifyurl";   //	M(50)	异步通知地址

    private static final String DPAYAPI = "DPAYAPI";



    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
            if(!channelWrapper.getAPI_MEMBERID().contains("&")){
                details.put(RESPONSEKEY, "商户号填写错误，格式：[商户ID号]&[账户ID],如：ABCD&1234");//强制必须保存下第三方结果
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            String appidStr = channelWrapper.getAPI_MEMBERID().split("&")[0];
            String useridStr = channelWrapper.getAPI_MEMBERID().split("&")[1];


                //组装参数
                payParam.put(cmd,DPAYAPI);
                payParam.put(version,"2.0");
                payParam.put(appid,appidStr);
                payParam.put(apporderid,channelWrapper.getAPI_ORDER_ID());
                payParam.put(userid,useridStr);
                payParam.put(ordertime,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString2));
                payParam.put(orderbody, channelWrapper.getAPI_ORDER_ID());
                payParam.put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(acctno, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(acctname,channelWrapper.getAPI_CUSTOMER_NAME() );
                payParam.put(bankname,channelWrapper.getAPI_CUSTOMER_BANK_NAME());
                payParam.put(bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() );
                payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );



                //生成md5
                Map<String, String> requestmap = new HashMap<String, String>();
                try{
                    requestmap.put(cmd, DPAYAPI);
                    requestmap.put(version, "2.0");
                    requestmap.put(appid, appidStr);
                    requestmap.put(apporderid, channelWrapper.getAPI_ORDER_ID());
                    String signStr = HTDaifuUtil.createParam(requestmap);  //签名字符串
                    String hmac = HTDaifuUtil.getKeyedDigestUTF8(signStr, channelWrapper.getAPI_KEY()); //签名
                    String content = HTDaifuUtil.createParam(payParam);  //请求体明文
                    String requestStr = UseRSAUtil.encryptByPublicKey(content, channelWrapper.getAPI_PUBLIC_KEY());
                    String dataStr = URLEncoder.encode(requestStr, UTF8);  //请求体转码后的密文
                    requestmap.put(data, dataStr);
                    requestmap.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), hmac);
                }catch (Exception e){
                    log.error("[HT代付]创建签名出错："+e.getMessage());
                    details.put(RESPONSEKEY, "[HT代付]创建签名出错,请核对 商户/密钥/公钥");//强制必须保存下第三方结果
                    return PayEumeration.DAIFU_RESULT.ERROR;
                }

                //发送请求获取结果

               String resultStr = HTDaifuUtil.submitPost(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0],HTDaifuUtil.createParam(requestmap)); //返回
              //  String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0],  HTDaifuUtil.createParam(requestmap), Maps.newHashMap()); //返回

                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
                if(StringUtils.isNotBlank(resultStr) ){
                        return getDaifuResult(JSON.toJSONString(HandlerUtil.urlToMap(resultStr)),false);
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
       if(1==2) throw new PayException("[HT代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(version,"V1.0.5");
            payParam.put(serviceName,"openTransferQuery");
            payParam.put(reqTime,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(merOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(signType,"MD5");

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


            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[HT代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(version,"V1.0.5");
            payParam.put(serviceName,"openAccountQuery");
            payParam.put(reqTime,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(currency,"CNY");
            payParam.put(signType,"MD5");


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


            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"respCode","SUCCESS") ){
                String balance = HandlerUtil.valJsonObjInSideJsonObj(jsonObj, "respBody", "balance");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[HT代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[HT代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","SUCCESS")   ){  //第三方明确成功返回结果
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"respBody","transferStatus","PENDING")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"respBody","transferStatus","SUCCESS")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"respBody","transferStatus","FAILURE")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }else if(HandlerUtil.valJsonObj(jsonObj,"respCode","FAILURE")  ){ //第三方明确失败，返回结果
              if(isQuery){
                  if(resultStr.contains("商户订单号不存在")) return PayEumeration.DAIFU_RESULT.ERROR;  // 查询时候，指定错误内容说明订单号没有被提交到第三方直接返回订单取消。
                  throw new PayException(resultStr);        //查询时候第三方未明确指定，订单状态失败，我们未知,抛异常返回unKnow。
              }
              return PayEumeration.DAIFU_RESULT.ERROR;             //请求代付时候，第三方明确请求不成功，订单状态既为取消
        }  else {throw new PayException(resultStr); }
    }








}