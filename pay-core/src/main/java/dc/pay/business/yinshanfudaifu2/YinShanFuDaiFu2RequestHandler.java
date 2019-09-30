package dc.pay.business.yinshanfudaifu2;

import java.util.List;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;


/**
 * 
 * @author andrew
 * Jun 24, 2019
 */
@RequestDaifuHandler("YINSHANFUDAIFU2")
public final class YinShanFuDaiFu2RequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YinShanFuDaiFu2RequestHandler.class);

    
    //参数名称    参数变量名   类型  必填  说明
    //商户号 pay_merberid    String  是   商户注册签约后，支付平台分配的 唯一标识号
    private static final String  pay_merberid               = "pay_merberid";
    //总金额 pay_amount  String  是   总金额单位为元（支持两位小数）
    private static final String  pay_amount               = "pay_amount";
    //代付订单号   pay_orderno String  是   商户唯一的订单号
    private static final String  pay_orderno               = "pay_orderno";
    //代付时间    pay_ordertime   String  是   字符串格式要求为：yyyy-MM-dd HH:mm:ss例如：2017-01-01 00:00:00
    private static final String  pay_ordertime               = "pay_ordertime";
    //银行编码    pay_bankcode    String  是   
    private static final String  pay_bankcode               = "pay_bankcode";
    //收款人姓名   account_name    String  是   收款人真实姓名
    private static final String  account_name               = "account_name";
    //收款卡号    account_number  String  是   
    private static final String  account_number               = "account_number";
    //签名  sign    String  是   签名数据，签名规则见附录
    private static final String  sign               = "sign";

    //响应参数定义：以 json 格式同步返回响应数据
    //参数名称    参数变量名   类型  必填  说明
    //提 交 数 据 是 否成功   status  String  是   fail失败success成功
//    private static final String  status               = "status";
    //描述  result_msg  String  是   描述
//    private static final String  result_msg               = "result_msg";
    //商 户 唯 一 订单号 pay_orderno String(32)  是   商 户 唯 一 订单号
//    private static final String  pay_orderno               = "pay_orderno";
    //支 付 平 台 订单号 pay_tranid  String  是   支 付 平 台 订单号
//    private static final String  pay_tranid               = "pay_tranid";
    //银 行 打 款 状态  pay_status  String  是   0 未处理，1 银行处理中 2  已打款3  失败(其他都是未处理)
//    private static final String  pay_status               = "pay_status";
    //签名  sign    String  是   签名数据，签名规则见附录
//    private static final String  sign               = "sign";

    //支付接口请求方式：支持 GET 或 POST 支付接口请求地址：
    //http://域名/dfpay_query.do 支付接口参数定义：
    //参数名称    参数变量名   类型  必填  说明
    //商户号 pay_merberid    String  是   支付平台分配商户号
//    private static final String  pay_merberid               = "pay_merberid";
    //商 户 平 台订单号  pay_orderno String  否   商户平台订单号和支付平台订单号二选一
//    private static final String  pay_orderno               = "pay_orderno";
    //支 付 平 台订 单号  pay_tranid  String  否   商户平台订单号和支付平台订单号二选一
//    private static final String  pay_tranid               = "pay_tranid";
    //签名  sign    String  是   签名数据，签名规则见附录
//    private static final String  sign               = "sign";

    //支付接口请求方式：支持 GET 或 POST 支付接口请求地址：
    //http://域名/query_amount.do支付接口参数定义：
    //参数名称    参数变量名   类型  必填  说明
    //商户号 pay_merberid    String  是   支付平台分配商户号
//    private static final String  pay_merberid               = "pay_merberid";
    //查询时间        query_time     String    是   字符串格式要求为：    yyyy-MM-dd HH:mm:ss    例如：2017-01-01 12:45:52
    private static final String  query_time               = "query_time";
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
                payParam.put(pay_merberid,channelWrapper.getAPI_MEMBERID());
                payParam.put(pay_amount,handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(pay_orderno,channelWrapper.getAPI_ORDER_ID());
                payParam.put(pay_ordertime,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                payParam.put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(account_name,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(account_number,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());

                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                sb.append("key=" + channelWrapper.getAPI_KEY());
                String signStr = sb.toString(); //.replaceFirst("&key=","")
                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                //发送请求获取结果
//                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");
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
       if(1==2) throw new PayException("[银闪付代付2][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(pay_merberid,channelWrapper.getAPI_MEMBERID());
            payParam.put(pay_orderno,channelWrapper.getAPI_ORDER_ID());
//            payParam.put(pay_tranid,channelWrapper.getAPI_ORDER_ID());
            
            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append("key=" + channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[银闪付代付2][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(pay_merberid,channelWrapper.getAPI_MEMBERID());
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
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"status","success") && jsonObj.containsKey("amount") && StringUtils.isNotBlank( jsonObj.getString("amount"))  ){
                String balance =  jsonObj.getString("amount");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[银闪付代付2][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[银闪付代付2][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        //代付：点击请求代付 操作
        if(!isQuery){
            if( HandlerUtil.valJsonObj(jsonObj,"status","success")){
                //银 行 打 款 状 态                   pay_status                  String  是   0 未处理，1 银行处理中 2  已打款                3  失败(其他都是未处理)
                if( HandlerUtil.valJsonObj(jsonObj,"pay_status","2")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj,"pay_status","0","1")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj,"pay_status","3")) return PayEumeration.DAIFU_RESULT.ERROR;
                //lee，13:10                0 1 2 3 除外的                都当处理中
                //lee，13:12                0 是未处理                我们系统会自动处理订单的                这个你那边当处理中就行
                return PayEumeration.DAIFU_RESULT.PAYING;
            //{"result_msg":"此订单号找不到对应的订单","status":"fail"}
            //我哪里知道这是为什么？
            //lee，16:13                刚看了下代码，请求代付，如果是status状态是fail说明数据没提交上来，可以当失败处理，查询订单接口以 pay_status为准，如果查询提示订单不存在，需要上我们客服确认订单，再做处理
            }else if (HandlerUtil.valJsonObj(jsonObj,"status","fail") && !jsonObj.containsKey("pay_status")) {
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            //给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
            //因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
//            if(null!=resultStr && resultStr.contains("失败"))  return PayEumeration.DAIFU_RESULT.ERROR;     
            //15:49
            //那就是说：返回这{"result_msg":"此订单号找不到对应的订单","status":"fail"}，我是status=fail来判定，并且以result_msg=此订单号找不到对应的订单来判定，就可以重新下发了？
            //ye，15:50
            //对的
            if(HandlerUtil.valJsonObj(jsonObj,"status","fail") && HandlerUtil.valJsonObj(jsonObj,"result_msg","此订单号找不到对应的订单"))  return PayEumeration.DAIFU_RESULT.ERROR;
            
            //状态                status             String  是   fail 失败            success 成功
            if( HandlerUtil.valJsonObj(jsonObj,"status","success")){
                //银 行 打 款 状 态                pay_status                String  是   0 未处理，1 银行处理中 2  已打款                3  失败 (其他)银行处理中
                if( HandlerUtil.valJsonObj(jsonObj,"pay_status","2")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj,"pay_status","0","1")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj,"pay_status","3")) return PayEumeration.DAIFU_RESULT.ERROR;
                //lee，13:10                0 1 2 3 除外的                都当处理中
                //lee，13:12                0 是未处理                我们系统会自动处理订单的                这个你那边当处理中就行                
                return PayEumeration.DAIFU_RESULT.PAYING;
            //{"result_msg":"此订单号找不到对应的订单","status":"fail"}
            //我哪里知道这是为什么？
            //lee，16:13                刚看了下代码，请求代付，如果是status状态是fail说明数据没提交上来，可以当失败处理，查询订单接口以 pay_status为准，如果查询提示订单不存在，需要上我们客服确认订单，再做处理
            }else if (HandlerUtil.valJsonObj(jsonObj,"status","fail")) {
                return PayEumeration.DAIFU_RESULT.PAYING;
            }
            throw new PayException(resultStr);
        }

    }








}