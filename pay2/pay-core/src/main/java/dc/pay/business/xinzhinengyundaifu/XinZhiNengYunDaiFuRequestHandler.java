package dc.pay.business.xinzhinengyundaifu;

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
import dc.pay.business.xinzhinengyundaifu.utils.PayUtils;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;


/**
 * 
 * @author andrew
 * May 17, 2019
 */
@RequestDaifuHandler("XINZHINENGYUNDAIFU")
public final class XinZhiNengYunDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinZhiNengYunDaiFuRequestHandler.class);

    
    //字段名 中文名 类型 类型 说明
    //公共参数
    //version 版本号 String(10) Y 固定 v1
    private static final String  version               = "version";
    //merchantId 商户号 String(32) Y 平台分配的商户号
    private static final String  merchantId               = "merchantId";
    //charset 字符串编码 String(10) Y UTF-8,GBK,GB2312 等
    private static final String  charset               = "charset";
    //signType 签名算法 String(12) Y RSA 目前只支持 RSA
    private static final String  signType               = "signType";
    //cipher 业务数据密文 text Y 业务参数加密获得
    private static final String  cipher               = "cipher";
    //sign 签名参数 text Y 公共参数签名获得
    private static final String  sign               = "sign";

    //业务参数 (组成 JSON 字符串，RSA 加密放入 cipher)
    //reqNo 商户请求流水号 String(50) Y 商户请求流水，需保证在商户 端不重复。只能包含字符、数   据、下划线。
    private static final String  reqNo               = "reqNo";
    //reqTime 提交时间 String(20) Y
    private static final String  reqTime               = "reqTime";
    //transAmt 交易金额 Int Y 订单总金额，单位为分
    private static final String  transAmt               = "transAmt";
    //acctName 收款人 String(30) Y
    private static final String  acctName               = "acctName";
    //acctNo 收款人银行账号 String(30) Y
    private static final String  acctNo               = "acctNo";
    //acctType 对公对私类型 Int N 1-对私，2-对公    为空默认对私
    private static final String  acctType               = "acctType";
    //bankCode 开户行代码 String(10) Y 收款人开户行代码，如 ICBC工商银行 参见《附录：银行代码对照表》。
    private static final String  bankCode               = "bankCode";
    //province 开户行省份 String(30) N 预留
//    private static final String  province               = "province";
    //city 开户行城市 String(30) N 预留
//    private static final String  city               = "city";
    //branchBankName 支行名称 String(32) N 预留
//    private static final String  branchBankName               = "branchBankName";
    //notifyUrl 异步通知地址 String(200) 公网能访问到
    private static final String  notifyUrl               = "notifyUrl";




    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                payParam.put(reqNo,channelWrapper.getAPI_ORDER_ID());
                payParam.put(reqTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                payParam.put(transAmt,channelWrapper.getAPI_AMOUNT());
                payParam.put(acctName,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(acctNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(acctType,"1");
                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                
                String my_cipher = PayUtils.encrypt(JSON.toJSONString(payParam),channelWrapper.getAPI_PUBLIC_KEY());

                Map<String,String> param = new TreeMap<>();
                param.put(version,"v1");
                param.put(merchantId,channelWrapper.getAPI_MEMBERID());
                param.put(charset,"UTF-8");
                param.put(signType,"RSA");
                param.put(cipher, my_cipher);
                //生成md5
                String my_sign = PayUtils.sign(param,channelWrapper.getAPI_KEY());

                param.put(sign, my_sign);

                //发送请求获取结果
//                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], param,"utf-8");
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], param, String.class,  HttpMethod.POST, defaultHeaders);
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
       if(1==2) throw new PayException("[新智能云代付][代付][查询订单状态]该功能未完成。");
        try {
            payParam.put(version,"v1");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(charset,"UTF-8");
            payParam.put(signType,"RSA");
            payParam.put(reqNo,channelWrapper.getAPI_ORDER_ID());

            String my_sign = PayUtils.sign(payParam,channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),my_sign);
            
            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,"utf-8");
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam, String.class,  HttpMethod.POST, defaultHeaders);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[新智能云代付][代付余额查询]该功能未完成。");

        try {
            //组装参数
            payParam.put(version,"v1");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(charset, "UTF-8");
            payParam.put(signType,"RSA");
            

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
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam,"utf-8");
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam, String.class,  HttpMethod.POST, defaultHeaders);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            
            if(HandlerUtil.valJsonObj(jsonObj,"code","00000")){
                
                JSONObject jsonObj2 = null;
                try {
                    String bizContent = PayUtils.decrypt(jsonObj.getString(cipher),channelWrapper.getAPI_KEY());
                    jsonObj2 = JSON.parseObject(bizContent);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new  PayException(resultStr);
                }
                
                String balance =  jsonObj2.getString("balance");
                return Long.parseLong(handlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[新智能云代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[新智能云代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        //给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
        //因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
        //代付：点击请求代付 操作
        if(!isQuery){
//            if( HandlerUtil.valJsonObj(jsonObj,"code","00000")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"code","00000")){
                JSONObject jsonObj2 = null;
                try {
                    String bizContent = PayUtils.decrypt(jsonObj.getString(cipher),channelWrapper.getAPI_KEY());
                    jsonObj2 = JSON.parseObject(bizContent);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new  PayException(resultStr);
                }
                //status  代付状态 Int  Y  该笔代付状态： 0-处理中 1-支付成功 2-支付失败，己退汇 该返回状态为最终银行处理交易 结果。
                if( HandlerUtil.valJsonObj(jsonObj2,"status","0")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","2")) return PayEumeration.DAIFU_RESULT.ERROR;
            }
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            if( HandlerUtil.valJsonObj(jsonObj,"code","00000")){
                JSONObject jsonObj2 = null;
                try {
                    String bizContent = PayUtils.decrypt(jsonObj.getString(cipher),channelWrapper.getAPI_KEY());
                    jsonObj2 = JSON.parseObject(bizContent);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new  PayException(resultStr);
                }
                //该笔代付状态：                0-处理中                1-支付成功                2-支付失败，己退汇                该返回状态为最终银行处理交易结果。
                if( HandlerUtil.valJsonObj(jsonObj2,"status","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","0")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj2,"status","2")) return PayEumeration.DAIFU_RESULT.ERROR;
//                if( HandlerUtil.valJsonObj(jsonObj,"pay_status","3")) return PayEumeration.DAIFU_RESULT.UNKNOW;
                throw new PayException(resultStr);
            }
            throw new PayException(resultStr);
        }

    }








}