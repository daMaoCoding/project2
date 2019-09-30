package dc.pay.business.laowangdaifu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;


/**
 * 
 * @author andrew
 * Sep 21, 2019
 */
@RequestDaifuHandler("LAOWANGDAIFU")
public final class LaoWangDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LaoWangDaiFuRequestHandler.class);

    //● 请求报文
    //字段名 变量名 必填  示例值 描述
    //商户号 merchNo 是       由分配给商户的商户唯一编码
    private static final String  merchNo               = "merchNo";
    //商户单号    orderNo 是   4392849234723987    商户上送订单号，保持唯一值。
    private static final String  orderNo               = "orderNo";
    //交易金额    amount  是   1000    以分为单位，如1000分
    private static final String  amount               = "amount";
    //币种  currency    是   CNY 目前只支持CNY
    private static final String  currency               = "currency";
    //银行编号    bankCode    是   1001    
    private static final String  bankCode               = "bankCode";
    //银行卡号    bankNo  是       银行卡号
    private static final String  bankNo               = "bankNo";
    //持卡人姓名   acctName    是       持卡人姓名
    private static final String  acctName               = "acctName";
    //省份  provName    是       
    private static final String  provName               = "provName";
    //城市  cityName    是       
    private static final String  cityName               = "cityName";
    //开户分行全称  bankFullName    是       
    private static final String  bankFullName               = "bankFullName";
    //手机号码    phone   是       
    private static final String  phone               = "phone";
    //身份证 cardId  是       
    private static final String  cardId               = "cardId";
    //签名  sign    是       
//    private static final String  sign               = "sign";
    
    private static final String  key               = "key";
    
    
    
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
                payParam.put(merchNo,channelWrapper.getAPI_MEMBERID());
                payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
                payParam.put(amount,channelWrapper.getAPI_AMOUNT());
                payParam.put(currency,"CNY");
                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(bankNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(acctName,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(provName,channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
                payParam.put(cityName,channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH());
                payParam.put(bankFullName,channelWrapper.getAPI_CUSTOMER_BANK_NAME());
                //凉拌黄瓜丝 2019/9/20 18:07:20
                //您的这个可以写死的
                payParam.put(phone,"13678907890");
                payParam.put(cardId,"6228480402564890018");

                Map<String,String> map = new TreeMap<>(payParam);
                map.put(key, channelWrapper.getAPI_KEY());
                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(map);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    sb.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
                }
                //删除最后一个字符
                sb.deleteCharAt(sb.length()-1);
//                sb.append("key=" + channelWrapper.getAPI_KEY());
                String signStr = sb.toString(); //.replaceFirst("&key=","")
                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                //发送请求获取结果
//                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
//                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], JSON.toJSONString(payParam), MediaType.APPLICATION_FORM_URLENCODED_VALUE);
                String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam, String.class, HttpMethod.POST);
//                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], JSON.toJSONString(payParam), MediaType.APPLICATION_JSON_UTF8_VALUE);
//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");
//                System.out.println("代付请求返回==>"+resultStr);
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
       if(1==2) throw new PayException("[老旺代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(merchNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
//            payParam.put(pay_tranid,channelWrapper.getAPI_ORDER_ID());
            
            //生成md5
            Map<String,String> map = new TreeMap<>(payParam);
            map.put(key, channelWrapper.getAPI_KEY());
            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(map);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
            }
            //删除最后一个字符
            sb.deleteCharAt(sb.length()-1);
//            sb.append("key=" + channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);



            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,"utf-8");
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam, String.class, HttpMethod.POST);
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
        if(1==2) throw new PayException("[老旺代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(merchNo,channelWrapper.getAPI_MEMBERID());
//            payParam.put(query_time,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));

            //生成md5
            //生成md5
            Map<String,String> map = new TreeMap<>(payParam);
            map.put(key, channelWrapper.getAPI_KEY());
            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(map);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
            }
            //删除最后一个字符
            sb.deleteCharAt(sb.length()-1);
//            sb.append("key=" + channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

//                System.out.println("代付余额查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2]);
//                System.out.println("代付余额查询返回==>"+resultStr);
//                System.out.println("代付余额查询参数==>"+JSON.toJSONString(payParam));

            JSONObject jsonObj = JSON.parseObject(resultStr);
            //如果有验证，最好调用一下验证方法
//            if(HandlerUtil.valJsonObj(jsonObj,"status","success") && jsonObj.containsKey("amount") && StringUtils.isNotBlank( jsonObj.getString("amount"))  ){
            if(jsonObj.containsKey("balance") && StringUtils.isNotBlank( jsonObj.getString("balance"))  ){
                String balance =  jsonObj.getString("balance");
//                凉拌黄瓜丝
//                这边看代码确认了下 余额查询返回单位是分
//                抱歉 之前记错了
                return Long.parseLong(balance);
//                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[老旺代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[老旺代付][代付余额查询]出错,错误:%s",e.getMessage()));
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
            //交易状态    orderState  是   1   详见“附录2 交易状态编码”
            //状态  状态码
            //待支付-下单成功    0
            //支付成功    1
            //支付失败    2
            //处理中 3
            //关闭  4
            //凉拌黄瓜丝 2019/9/21 10:33:17
            //代付接口只有1代表接受成功
            if( HandlerUtil.valJsonObj(jsonObj,"orderState","0","2","3","4")) return PayEumeration.DAIFU_RESULT.ERROR;
//            if( HandlerUtil.valJsonObj(jsonObj,"orderState","0","3")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"orderState","1")) return PayEumeration.DAIFU_RESULT.PAYING;
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            //交易状态  orderState  是   1   详见“附录2 交易状态编码”
            //状态  状态码
            //待支付-下单成功    0
            //支付成功    1
            //支付失败    2
            //处理中 3
            //关闭  4
            //凉拌黄瓜丝 2019/9/21 10:39:42
            //查询接口 状态码0是处理中 2跟4是失败  1是成功，其他的都是处理中
//            if( HandlerUtil.valJsonObj(jsonObj,"pay_status","3")) return PayEumeration.DAIFU_RESULT.UNKNOW;
            if( HandlerUtil.valJsonObj(jsonObj,"orderState","2","4")) return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"orderState","0","3")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"orderState","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            throw new PayException(resultStr);
        }

    }








}