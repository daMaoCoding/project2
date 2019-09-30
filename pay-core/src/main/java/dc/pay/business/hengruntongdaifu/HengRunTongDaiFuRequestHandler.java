package dc.pay.business.hengruntongdaifu;

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


@RequestDaifuHandler("HENGRUNTONGDAIFU")
public final class HengRunTongDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HengRunTongDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String   inputCharset = "inputCharset";  //  字符集编码
     private static final String   merchantId = "merchantId";  //  商户号
     private static final String   payAmount = "payAmount";  //  总金额
     private static final String   transId = "transId";  //  代付订单
     private static final String   payTime = "payTime";  //  代付时间
     private static final String   bankCode = "bankCode";  //  银行代码
     private static final String   cardName = "cardName";  //  收款人姓名
     private static final String   cardNumber = "cardNumber";  //  收款卡号
     private static final String   queryTime = "queryTime";
     private static final String   sign = "sign";  //  签名




    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(inputCharset,UTF8);
                payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
                payParam.put(payAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(transId,channelWrapper.getAPI_ORDER_ID());
                payParam.put(payTime,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(cardName, URLEncoder.encode(channelWrapper.getAPI_CUSTOMER_NAME(),UTF8));
                payParam.put(cardNumber, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());

                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    if(org.apache.commons.lang.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()))
                        continue;
                    if(cardName.equalsIgnoreCase(paramKeys.get(i).toString())){
                        sb.append(paramKeys.get(i)).append("=").append(URLDecoder.decode(payParam.get(paramKeys.get(i)),UTF8)).append("&");
                        continue;
                    }
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                sb.append("key=" + channelWrapper.getAPI_KEY());
                String signStr = sb.toString(); //.replaceFirst("&key=","")
                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID()); //增加自动查询
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
       if(1==2) throw new PayException("[恒润通代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(inputCharset,UTF8);
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(queryTime,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
            payParam.put(transId,channelWrapper.getAPI_ORDER_ID());

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if(org.apache.commons.lang.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()))
                    continue;
                if(cardName.equalsIgnoreCase(paramKeys.get(i).toString())){
                    sb.append(paramKeys.get(i)).append("=").append(URLDecoder.decode(payParam.get(paramKeys.get(i)),UTF8)).append("&");
                    continue;
                }
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append("key=" + channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
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
        throw new PayException("[恒润通代付][代付余额查询]第三方不支持此功能。");
    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        if(HandlerUtil.valJsonObj(jsonObj,"retCode","0000")   ){  //第三方明确成功返回结果
            if(HandlerUtil.valJsonObj(jsonObj,"status","0","1")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObj(jsonObj,"status","2")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if(HandlerUtil.valJsonObj(jsonObj,"status","3")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }else if(isQuery){
            if(resultStr.contains("商户订单号不存在")||resultStr.contains("无此订单号对应的订单")) return PayEumeration.DAIFU_RESULT.ERROR;  // 查询时候，指定错误内容说明订单号没有被提交到第三方直接返回订单取消。
            throw new PayException(resultStr);        //查询时候第三方未明确指定，订单状态失败，我们未知,抛异常返回unKnow。
        }  else {
            return PayEumeration.DAIFU_RESULT.ERROR;
        }
    }








}