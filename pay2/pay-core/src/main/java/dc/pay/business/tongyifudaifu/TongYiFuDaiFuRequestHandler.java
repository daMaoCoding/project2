package dc.pay.business.tongyifudaifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;


@RequestDaifuHandler("TONGYIFUDAIFU")
public final class TongYiFuDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongYiFuDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  version = "version";       //   版本号   str(7) 是 目前版本号：V1.0.5
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
     private static final String  currency = "currency";       //   签名   str(256) 是 签名内容



      private static final String  mer_no="mer_no";       //	商户号	String	是	平台分配的唯一商户编号
      private static final String  mer_order_no="mer_order_no";       //	商户订单号	String	是	商户必须保证唯一
      private static final String  acc_type="acc_type";       //	账户类型	Number	是	1:对私 2：对公
      private static final String  acc_no="acc_no";       //	收款账号	String	是	收款账号
      private static final String  acc_name="acc_name";       //	收款户名	String	是	收款户名
      private static final String  bank_name="bank_name";
      private static final String  order_amount="order_amount";       //	金额	Number	是	分为单位；整数
      private static final String  sign="sign";       //	数字签名	String	是
     private static final String  request_no="request_no";  //	请求流水号	String	是	商户必须保证唯一
     private static final String  request_time="request_time";  //	请求时间戳	String	是	时间戳：(格式为yyyyMMddHHmmss 4位年+2位月+2位日+2位时+2位分+2位秒)
     private static final String  order_no="order_no";  //	平台订单号	String	否	平台唯一







    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(mer_no,channelWrapper.getAPI_MEMBERID());
                payParam.put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
                payParam.put(acc_type,"1");
                payParam.put(acc_no,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(acc_name,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(order_amount,channelWrapper.getAPI_AMOUNT());
                payParam.put(bank_name,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());

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
                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
                addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());

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
       if(1==2) throw new PayException("[统一付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(request_no,HandlerUtil.getRandomStrStartWithDate(7));
            payParam.put(request_time,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString2));
            payParam.put(mer_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(mer_order_no,channelWrapper.getAPI_ORDER_ID());

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
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[统一付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(request_no,HandlerUtil.getRandomStrStartWithDate(7));
            payParam.put(request_time,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString2));
            payParam.put(mer_no,channelWrapper.getAPI_MEMBERID());

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
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"query_status","SUCCESS") && jsonObj.containsKey("list") && null!=jsonObj.getJSONArray("list").getJSONObject(0) && jsonObj.getJSONArray("list").getJSONObject(0).containsKey("balance") && StringUtils.isNotBlank( jsonObj.getJSONArray("list").getJSONObject(0).getString("balance"))  ){
                String balance = jsonObj.getJSONArray("list").getJSONObject(0).getString("balance");
                return Long.parseLong(balance);
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[统一付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[统一付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        if(!isQuery){
            if( HandlerUtil.valJsonObj(jsonObj,"status","SUCCESS")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"status","FAIL")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }else{
            if(null!=resultStr && resultStr.contains("记录不存在"))  return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"query_status","SUCCESS")){
                if( HandlerUtil.valJsonObj(jsonObj,"status","SUCCESS")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj,"status","FAIL")) return PayEumeration.DAIFU_RESULT.ERROR;
                if( HandlerUtil.valJsonObj(jsonObj,"status","UNKNOW")) return PayEumeration.DAIFU_RESULT.UNKNOW;
                throw new PayException(resultStr);
            }
            throw new PayException(resultStr);
        }

    }








}