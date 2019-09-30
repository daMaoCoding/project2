package dc.pay.business.kuaihuitongdaifu;

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
 * Jul 24, 2019
 */
@RequestDaifuHandler("KUAIHUITONGDAIFU")
public final class KuaiHuiTongDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiHuiTongDaiFuRequestHandler.class);

     // 4.3代付接口 1009
     // 数据元名称   数据元标识   数据元格式   请求  响应  数据元取值说明
     // 交易类型    transType   N4  M       1009
    private static final String  transType               = "transType";
     // 机构号 instCode    任意(1,10)    M       平台机构号
    private static final String  instCode               = "instCode";
     // 证件类型    certType    数字(1,2) M       目前只支持0-平台商户号 1-身份证 2-营业执照,9-其他    固定送0
    private static final String  certType               = "certType";
     // 证件号码    certId  任意(8,20)    M       平台商户号
    private static final String  certId               = "certId";
     // 金额  transAmt    数字(1,20)    M       整数，以分为单位
    private static final String  transAmt               = "transAmt";
     // 交易日期    transDate   日期(8,8) M       yyyyMMdd
    private static final String  transDate               = "transDate";
     // 订单号 orderNo 任意(1,20)    M       商户号+订单号+商户日期唯一标示一笔交易
    private static final String  orderNo               = "orderNo";
     // 结算账号    accountId   任意(1,256)   M       入账银行卡号
    private static final String  accountId               = "accountId";
     // 结算户名    accountName 任意(1,30)    M       入账银行卡户名
    private static final String  accountName               = "accountName";
     // 结算银行编号  bankCode    数字(1,20)    C       入账银行总行行号
    private static final String  bankCode               = "bankCode";
     // 后台通知地址URL   backUrl 任意(1,256)   C       平台通知支付结果地址,支持交易中上送或预配置
    private static final String  backUrl               = "backUrl";
     // 签名  sign    任意32)   M   M   MD5签名
//    private static final String  sign               = "sign";
     // 订单状态    orderStatus 数字(1)       M   0-待处理 1-成功 2-失败其它-待确认
//    private static final String  orderStatus               = "orderStatus";
     // 通讯应答码   ret_code    任意(1,20)        M   0000-成功，非0000-失败，失败原因看通讯描述。
//    private static final String  ret_code               = "ret_code";
     // 通讯描述    ret_msg 任意(1,1024)      M   
//    private static final String  ret_msg               = "ret_msg";
    
     //4.2支付查询接口 2000
     //数据元名称   数据元标识   数据元格式   请求  响应  数据元取值说明
     //交易类型    transType   N4  M       2000 
//    private static final String  transType               = "transType";
     //机构号 instCode    任意(1,10)    M       平台机构号
//    private static final String  instCode               = "instCode";
     //证件类型    certType    数字(1,2) M       目前只支持0-平台商户号 1-身份证 2-营业执照,9-其他    固定送0
//    private static final String  certType               = "certType";
     //证件号码    certId  任意(8,20)    M       平台商户号
//    private static final String  certId               = "certId";
     //交易日期    transDate   日期(8,8) M       原订单交易日期yyyyMMdd
//    private static final String  transDate               = "transDate";
     //订单号 orderNo 任意(1,20)    M       原订单号
//    private static final String  orderNo               = "orderNo";
     //签名  sign    任意32)   M   M   MD5签名
    private static final String  sign               = "sign";
     //金额  transAmt    数字(1,20)        M   整数，以分为单位
//    private static final String  transAmt               = "transAmt";
     //实际金额    actualAmount    数字(1,20)        C   整数，以分为单位
//    private static final String  actualAmount               = "actualAmount";
     //订单状态    orderStatus 数字(1,1)     M   ret_code返回值0000时返回    0 待处理    1 成功    2 失败    3待确认    6 待回调
//    private static final String  orderStatus               = "orderStatus";
     //通讯应答码   ret_code    任意(1,20)        M   0000-成功，非0000-失败，失败原因看通讯描述。
//    private static final String  ret_code               = "ret_code";
//     通讯描述    ret_msg 任意(1,1024)      M   一般是错误信息说明
//    private static final String  ret_msg               = "ret_msg";
    
    //4.2余额查询接口2003
    //数据元名称   数据元标识   数据元格式   请求  响应  数据元取值说明
    //交易类型    transType   N4  M       2003
//    private static final String  transType               = "transType";
    //机构号 instCode    任意(1,10)    M       平台机构号
//    private static final String  instCode               = "instCode";
    //证件类型    certType    数字(1,2) M       目前只支持0-平台商户号 1-身份证 2-营业执照,9-其他    固定送0
//    private static final String  certType               = "certType";
    //证件号码    certId  任意(8,20)    M       平台商户号
//    private static final String  certId               = "certId";
    //签名  sign    任意32)   M   M   MD5签名
//    private static final String  sign               = "sign";
    //总余额 totalBalance    数字(1,20)        M   整数，以分为单位
//    private static final String  totalBalance               = "totalBalance";
    //可用金额    balance 数字(1,20)        M   整数，以分为单位
//    private static final String  balance               = "balance";
    //冻结金额    unBalance   数字(1,20)        M   整数，以分为单位
//    private static final String  unBalance               = "unBalance";
    //订单状态    orderStatus 数字(1)       M   1-成功 2-失败其它-待确认
//    private static final String  orderStatus               = "orderStatus";
    //交易流水    tranSerno   任意(1,30)        M   交易流水号
//    private static final String  tranSerno               = "tranSerno";
    //通讯应答码   ret_code    任意(1,20)        M   0000-成功，非0000-失败，失败原因看通讯描述。
//    private static final String  ret_code               = "ret_code";
    //通讯描述    ret_msg 任意(1,1024)      M   
//    private static final String  ret_msg               = "ret_msg";



    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[快汇通代付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号certId&机构号instCode" );
            throw new PayException("[快汇通代付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号certId&机构号instCode" );
        }
        try {
                //组装参数
                payParam.put(transType,"1009");
                payParam.put(instCode,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                payParam.put(certType,"0");
                payParam.put(certId,channelWrapper.getAPI_MEMBERID().split("&")[0]);
                payParam.put(transAmt,channelWrapper.getAPI_AMOUNT());
                payParam.put(transDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
                payParam.put(accountId,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(accountName,channelWrapper.getAPI_CUSTOMER_NAME());
                
                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(backUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());

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
                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");

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
       if(1==2) throw new PayException("[快汇通代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(transType,"2000");
            payParam.put(instCode,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(certType,"0");
            payParam.put(certId,channelWrapper.getAPI_MEMBERID().split("&")[0]);
//            payParam.put(transDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            payParam.put(transDate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMdd"));
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
            
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
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[快汇通代付][代付余额查询]该功能未完成。");

        try {
            //组装参数
            payParam.put(transType,"2003");
            payParam.put(instCode,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(certType,"0");
            payParam.put(certId,channelWrapper.getAPI_MEMBERID().split("&")[0]);
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
//            if(HandlerUtil.valJsonObj(jsonObj,"ret_code","0000") && HandlerUtil.valJsonObj(jsonObj,"orderStatus","1") && jsonObj.containsKey("balance") && StringUtils.isNotBlank( jsonObj.getString("balance"))  ){
            if(HandlerUtil.valJsonObj(jsonObj,"ret_code","0000") && jsonObj.containsKey("balance") && StringUtils.isNotBlank( jsonObj.getString("balance"))  ){
                String balance =  jsonObj.getString("balance");
                return Long.parseLong(balance);
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[快汇通代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[快汇通代付][代付余额查询]出错,错误:%s",e.getMessage()));
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
//            订单状态    orderStatus 数字(1)       M   0-待处理 1-成功 2-失败  其它-待确认
//            通讯应答码   ret_code    任意(1,20)        M   0000-成功，非0000-失败，失败原因看通讯描述。
//            通讯描述    ret_msg 任意(1,1024)      M   
            if ( HandlerUtil.valJsonObj(jsonObj,"ret_code","0000")) {
                if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","0")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","2")) return PayEumeration.DAIFU_RESULT.ERROR;
                //其它-待确认
                if( !HandlerUtil.valJsonObj(jsonObj,"orderStatus","0") && !HandlerUtil.valJsonObj(jsonObj,"orderStatus","1") && !HandlerUtil.valJsonObj(jsonObj,"orderStatus","2")) return PayEumeration.DAIFU_RESULT.PAYING;
//            }else if ( !HandlerUtil.valJsonObj(jsonObj,"ret_code","0000")) {
//                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            //订单状态    orderStatus 数字(1,1)     M   ret_code返回值0000时返回            0 待处理            1 成功            2 失败            3待确认            6 待回调
            //通讯应答码   ret_code    任意(1,20)        M   0000-成功，非0000-失败，失败原因看通讯描述。
            //通讯描述    ret_msg 任意(1,1024)      M   一般是错误信息说明
            if( HandlerUtil.valJsonObj(jsonObj,"ret_code","0000")){
                if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","0","3", "6")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","2")) return PayEumeration.DAIFU_RESULT.ERROR;
//            }else if ( !HandlerUtil.valJsonObj(jsonObj,"ret_code","0000")) {
//                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            throw new PayException(resultStr);
        }

    }








}