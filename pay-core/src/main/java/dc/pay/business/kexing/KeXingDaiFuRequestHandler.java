package dc.pay.business.kexing;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.yunbaodaifu.YunBaoPayUtils;
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
import java.util.TreeMap;


@RequestDaifuHandler("KEXINGDAIFU")
public final class KeXingDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KeXingDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
      private static final String version = "version";            //	版本号	M	固定值1.0.0
      private static final String transType = "transType";            //	业务类型	M	固定值 PROXY_PAY
      private static final String productId = "productId";            //	产品类型	M	8001    T1 代付 8002    D0代付
      private static final String merNo = "merNo";            //	商户号	M	商户号
      private static final String orderDate = "orderDate";            //	订单日期	M	订单交易日期 yyyyMMdd
      private static final String orderNo = "orderNo";            //	订单号	M	商户平台订单号
      private static final String notifyUrl = "notifyUrl";            //	后台通知地址	M	用户完成支付后,服务器后台通知地址
      private static final String transAmt = "transAmt";            //	交易金额	M	分为单位如 100 代表  1.00元
      private static final String commodityName = "commodityName";            //	产品名称	M	代付产品名称
      private static final String cardNo = "cardNo";            //	银行卡号	M	银行卡号
      private static final String cardName = "cardName";            //	姓名	M	银行预留开卡持卡人姓名
      private static final String signature = "signature";            //	签名字段	M	参考 目录3.3
      private static final String  serialId = "serialId";  //	系统跟踪号	C	系统跟踪号
      private static final String  orderId = "orderId";  //	商户订单号	C	商户订单号
      private static final String  notifyFlag = "notifyFlag";  //	是否补发通知	C	默认不补发 1为补发通知





    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        String pay_md5sign="";
        try {

                //第三方2个账户，先查询哪个账户有余额从哪里出
                long cashAccount=0L;
                try {
                    cashAccount = queryDaifuBalanceAllInOne(Maps.newHashMap(), Maps.newHashMap());
                }catch (Exception e){}


               //组装参数
                payParam.put(version,"1.0.0");
                payParam.put(transType,"PROXY_PAY");
                payParam.put(productId,"8001");
                if(cashAccount>Long.parseLong(channelWrapper.getAPI_AMOUNT())) payParam.put(productId,"8002"); //如果d0账户余额大于本次出款金额使用d0账户出款
                payParam.put(merNo,channelWrapper.getAPI_MEMBERID());
                payParam.put(orderDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
                payParam.put(notifyUrl,  channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.put(transAmt, channelWrapper.getAPI_AMOUNT());
                payParam.put(commodityName, channelWrapper.getAPI_ORDER_ID());
                payParam.put(cardNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER() );
                payParam.put(cardName,channelWrapper.getAPI_CUSTOMER_NAME() );

                //生成签名
                try{
                    pay_md5sign =  Rsa.getSign(payParam,channelWrapper.getAPI_KEY());
                }catch (Exception e){
                    log.error(e.getMessage(),e);
                    details.put(RESPONSEKEY, "[科星代付]生成签名错误，请检查公钥私钥。");
                    return  PayEumeration.DAIFU_RESULT.ERROR;
                }
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                details.put(RESPONSEKEY, resultStr+"，其中8001-d0账户余额（分）："+cashAccount);//强制必须保存下第三方结果
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
       if(1==2) throw new PayException("[科星代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(version,"1.0.0");
            payParam.put(transType,"TRANS_QUERY");
            payParam.put(productId,"0000");
            payParam.put(merNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderId,channelWrapper.getAPI_ORDER_ID());

            //生成md5
            String pay_md5sign =  Rsa.getSign(payParam,channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[科星代付][代付余额查询]该功能未完成。");

        //第三方余额，有4种，取程序常用的D0账户
        //cashAccount	现金账户余额	M	现金账户为商户D0交易余额（分）
        //settlementAccount	结算账户余额	M	结算账户为商户T1账户余额（分）
        //cashDepositAccount	保证金余额	M	保证金账户余额（分）
        //creditAccount	授信账户余额	M	授信账户余额（分）

        try {
            //组装参数
            payParam.put(version,"1.0.0");
            payParam.put(transType,"BALANCE_QUERY");
            payParam.put(productId,"0000");
            payParam.put(merNo,channelWrapper.getAPI_MEMBERID());


            //生成md5
            String pay_md5sign =  Rsa.getSign(payParam,channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            //验证签名
            Map<String, String> tempMap = JSON.parseObject(resultStr,new TypeReference<TreeMap<String, String>>() {});
            boolean verifyed = Rsa.verify(tempMap, channelWrapper.getAPI_PUBLIC_KEY());
            if(!verifyed) throw new PayException("第三方数据验签失败，请检查后台【公钥】配置是否正确。");


            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") ){
               // Long balance = HandlerUtil.addStringNumber(jsonObj.getString("cashAccount"),jsonObj.getString("settlementAccount"));  //cashAccount 是  D0 余额     settlementAccount  是 T1 余额
                Long balance = HandlerUtil.addStringNumber(jsonObj.getString("cashAccount"),"0");  //余额只返回 D0 余额
                return balance;
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[科星代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[科星代付][代付余额查询]出错，错误:%s",e.getMessage()));
        }

    }





    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        //第三方讲，查询用o开头的，请求不用o开头的，0000表示 支付成功   p开头的状态码表示 结果未知(就是上游受理 但是还不知道到账结果） 剩下的 全部 表示失败 （各种不同状态码代表 各种不同失败原因）（2441349890，玩世不恭）

        JSONObject jsonObj = JSON.parseObject(resultStr);
        String respCode ="";
        if(isQuery){
            respCode = jsonObj.getString("oRespCode");
            if(resultStr.contains("原交易不存在")) return PayEumeration.DAIFU_RESULT.ERROR;  //这里第三方结果返回： "respDesc" -> "原交易不存在"，"respCode" -> "T028"，与上面【，查询用o开头的，请求不用o开头的】不一致。
        }else{
            respCode = jsonObj.getString("respCode");
        }
        if(StringUtils.isNotBlank(respCode)){
            Map<String, String> tempMap = JSON.parseObject(resultStr,new TypeReference<TreeMap<String, String>>() {});
            boolean verifyed = Rsa.verify(tempMap, channelWrapper.getAPI_PUBLIC_KEY());
            if(!verifyed) throw new PayException("第三方数据验签失败，【公钥】验证出错。第三方返回"+resultStr);
            if( respCode.equalsIgnoreCase("0000")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if(respCode.startsWith("P")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(respCode.equalsIgnoreCase("9998")) return PayEumeration.DAIFU_RESULT.ERROR;
            return PayEumeration.DAIFU_RESULT.ERROR;
        }
        throw new PayException(resultStr);
    }








}