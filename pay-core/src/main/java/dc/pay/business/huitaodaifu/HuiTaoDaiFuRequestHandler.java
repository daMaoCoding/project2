package dc.pay.business.huitaodaifu;

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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;


@RequestDaifuHandler("HUITAODAIFU")
public final class HuiTaoDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiTaoDaiFuRequestHandler.class);

     //请求代付&查询代付-参数
     private static final String  Version = "Version";   //版本号  String(10) Y 默认V2.5.1
     private static final String  ServerName = "ServerName";   //服务名称  String(20) Y 详见2.1章节
     private static final String  MerNo = "MerNo";   //商户ID  String(10) Y 平台开通的商户号
     private static final String  ReqTime = "ReqTime";   //发送时间  String(14) Y yyyy-MM-ddHH:mm:ss
     private static final String  SignType = "SignType";   //签名方式  String(5) Y MD5
     private static final String  SignInfo = "SignInfo";   //签名值  String Y 数据签名信息

     private static final String  TransId = "TransId";     //代付订单号  String 是 交易流水ID，对应每笔转账信息
     private static final String  Amount = "Amount";     //代付金额  String 是 总金额单位为元（支持两位小数）
     private static final String  BankCode = "BankCode";     //银行编码  String 是
     private static final String  BusType = "BusType";     //业务类型  String 是 PUB-对公，PRV-对私
     private static final String  AccountName = "AccountName";     //收款人姓名  String 是URLEncoder.encode方法进行编码并指定编码格式UTF-8,注MD5签名时，需要用参数的原始值而不是转义后的值
     private static final String  CardNo = "CardNo";     //收款卡号  String 是
     private static final String  NotifyURL = "NotifyURL";     //回调地址  String 否 异步通知地址
     private static final String  OrderNumber = "OrderNumber";
     private static final String  Currency = "Currency";


    private static final String TradeStatus ="TradeStatus";
    private static final String TransStatus="TransStatus";




    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(Version,"V2.5.1");
                payParam.put(ServerName,"hcTransferPay");
                payParam.put(MerNo,channelWrapper.getAPI_MEMBERID());
                payParam.put(ReqTime,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                payParam.put(SignType,"MD5");
                payParam.put(TransId,channelWrapper.getAPI_ORDER_ID());
                payParam.put(Amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(BankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(BusType,"PRV");
                //payParam.put(AccountName,URLEncoder.encode(channelWrapper.getAPI_CUSTOMER_NAME(),"UTF-8"));
                payParam.put(AccountName,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(CardNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(NotifyURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());


                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    if(org.apache.commons.lang.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || SignInfo.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                        continue;
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                sb.append("key=" + channelWrapper.getAPI_KEY());
                String signStr = sb.toString(); //.replaceFirst("&key=","")
                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


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
       if(1==2) throw new PayException("[汇淘代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(Version,"V2.5.1");
            payParam.put(ServerName,"hcTransferQuery");
            payParam.put(MerNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(ReqTime,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
            payParam.put(SignType,"MD5");
            payParam.put(OrderNumber,channelWrapper.getAPI_ORDER_ID());

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if(org.apache.commons.lang.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || SignInfo.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                    continue;
                if(AccountName.equalsIgnoreCase(paramKeys.get(i).toString())) sb.append(paramKeys.get(i)).append("=").append(URLDecoder.decode(payParam.get(paramKeys.get(i)),"UTF-8")).append("&");
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
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[汇淘代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(Version,"V2.5.1");
            payParam.put(ServerName,"hcBalanceQuery");
            payParam.put(MerNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(ReqTime,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
            payParam.put(SignType,"MD5");
            payParam.put(Currency,"CNY");

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if(org.apache.commons.lang.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || SignInfo.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                    continue;
                if(AccountName.equalsIgnoreCase(paramKeys.get(i).toString())) sb.append(paramKeys.get(i)).append("=").append(URLDecoder.decode(payParam.get(paramKeys.get(i)),"UTF-8")).append("&");
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
            if(HandlerUtil.valJsonObj(jsonObj,"ResCode","RESPONSE_SUCCESS") ){
                String balance = HandlerUtil.valJsonObjInSideJsonObj(jsonObj, "RespContent", "Balance");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[汇淘代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[汇淘代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);

        String statusName = isQuery?TradeStatus:TransStatus;

        if(HandlerUtil.valJsonObj(jsonObj,"ResCode","RESPONSE_SUCCESS")   ){  //第三方明确成功返回结果
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"RespContent",statusName,"transing","PENDING","tradeing")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"RespContent",statusName,"ok","OK")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"RespContent",statusName,"fail","FAIL")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }else {
            if(isQuery){
                if(resultStr.contains("商户订单号不存在")) return PayEumeration.DAIFU_RESULT.ERROR;
                throw new PayException(resultStr);        //查询时候第三方未明确指定，订单状态失败，我们未知,抛异常返回unKnow。
            }
            return PayEumeration.DAIFU_RESULT.ERROR;             //请求代付时候，第三方明确请求不成功，订单状态既为取消
        }
    }








}