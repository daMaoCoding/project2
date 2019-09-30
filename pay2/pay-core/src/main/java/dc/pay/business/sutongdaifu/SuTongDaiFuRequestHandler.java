package dc.pay.business.sutongdaifu;

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


@RequestDaifuHandler("SUTONGDAIFU")
public final class SuTongDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SuTongDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  merchant_code  ="merchant_code";     //  商户号         varchar(8) 是 商户注册签约后，⽀付平台分配的
     private static final String  order_amount  ="order_amount";     //  代付⾦额       decimal(14,2) 是
     private static final String  trade_no  ="trade_no";     //  代付订单号     varchar(100) 是
     private static final String  trade_time  ="trade_time";     //  代付时间       date 是  yyyy-MM-dd HH:mm:ss
     private static final String  bank_code  ="bank_code";     //  银⾏编码       varchar(60) 是 参考3.2  银⾏代码
     private static final String  account_name  ="account_name";     //  收款⼈姓名     varchar(60) 是
     private static final String  account_number  ="account_number";     //  收款卡号       varchar(30) 是 银⾏卡号
     private static final String  notify_url  ="notify_url";     //  异步通知地址   varchar(200) 是 不可包含?``& 等符号
     private static final String  sign  ="sign";     //  签名          char(32) 是 签名数据，签名规则⻅附录
     private static final String   query_time="query_time"; //  查询时间 date  yyyy-MM-dd HH:mm:ss
     private static final String   now_date="now_date";
     private static final String   order_no="order_no";
     private static final String return_data="return_data";
     private static final String bank_status="bank_status";
     private static final String trade_status="trade_status";
     private static final String state="state";
     private static final String data="data";


    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(merchant_code,channelWrapper.getAPI_MEMBERID());
                payParam.put(order_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(trade_no,channelWrapper.getAPI_ORDER_ID());
                payParam.put(trade_time,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
                payParam.put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(account_name,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(account_number,  channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());


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
                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
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
       if(1==2) throw new PayException("[速通代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(merchant_code,channelWrapper.getAPI_MEMBERID());
            payParam.put(now_date,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
            payParam.put(order_no,channelWrapper.getAPI_ORDER_ID());

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
        if(1==2) throw new PayException("[速通代付][代付余额查询]该功能未完成。");

        try {
            //组装参数
            payParam.put(merchant_code,channelWrapper.getAPI_MEMBERID());
            payParam.put(query_time,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));


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
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"state","true") ){
                String balance = jsonObj.getString("money");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[速通代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[速通代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }




    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        JSONObject dataObj = null;
        String statusKeyName="";
        if(HandlerUtil.valJsonObj(jsonObj,state,"true")   ){  //第三方明确成功返回结果
            statusKeyName =isQuery?trade_status:bank_status;
            dataObj =isQuery?jsonObj.getJSONObject(data):jsonObj.getJSONArray(return_data).getJSONObject(0);
            if(HandlerUtil.valJsonObj(dataObj,statusKeyName,"1","8","32","16")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObj(dataObj,statusKeyName,"128")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if(HandlerUtil.valJsonObj(dataObj,statusKeyName,"130")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }else if(HandlerUtil.valJsonObj(jsonObj,state,"false")  ){ //第三方明确失败，返回结果
              if(isQuery){
                  if(resultStr.contains("暂无数据")) return PayEumeration.DAIFU_RESULT.ERROR;  // 查询时候，指定错误内容说明订单号没有被提交到第三方直接返回订单取消。
                  throw new PayException(resultStr);        //查询时候第三方未明确指定，订单状态失败，我们未知,抛异常返回unKnow。
              }
              return PayEumeration.DAIFU_RESULT.ERROR;             //请求代付时候，第三方明确请求不成功，订单状态既为取消
        }  else {throw new PayException(resultStr); }
    }








}