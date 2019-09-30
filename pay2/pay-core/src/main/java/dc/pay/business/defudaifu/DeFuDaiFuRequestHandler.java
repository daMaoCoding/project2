package dc.pay.business.defudaifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.yidongzhifu.XmlUtils;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;


/**
 * 
 * @author andrew
 * Jul 2, 2019
 */
@RequestDaifuHandler("DEFUDAIFU")
public final class DeFuDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DeFuDaiFuRequestHandler.class);

    //名称    类型    是否必填    说明
    //batchAmount    String    M    总金额
    private static final String  batchAmount               = "batchAmount";
    //batchBiztype    String    M    提交批次类型，默认00000
    private static final String  batchBiztype               = "batchBiztype";
    //batchContent    String    M    明细部分，见明细说明
    private static final String  batchContent               = "batchContent";
    //batchCount    String    N    总笔数
    private static final String  batchCount               = "batchCount";
    //batchDate    String    M    提交日期   
    private static final String  batchDate               = "batchDate";
    //batchNo    String（128）    M    批次号，不能重复
    private static final String  batchNo               = "batchNo";
    //batchVersion    String    M    版本号，固定值为00   
    private static final String  batchVersion               = "batchVersion";
    //charset    String    M    输入编码：GBK，UTF-8
    private static final String  charset               = "charset";
    //merchantId    String    M    商户号 ID 
    private static final String  merchantId               = "merchantId";
    //signType    String    M    签名方：SHA
    private static final String  signType               = "signType";
    //签名  sign    String  是   签名数据，签名规则见附录
//    private static final String  sign               = "sign";

    //响应参数
    //名称    类型    说明
    //respCode    String    错误代码
    private static final String  respCode               = "respCode";
    //respMessage    String    错误消息
//    private static final String  respMessage               = "respMessage";

    //查询
    //名称    类型    是否必填    说明
    //batchDate    String    M    批次上传提交日期
//    private static final String  batchDate               = "batchDate";
    //batchNo    String（128）    M    批次号
//    private static final String  batchNo               = "batchNo";
    //batchVersion    String    M    版本号：固定值00
//    private static final String  batchVersion               = "batchVersion";
    //charset    String    M    输入编码
//    private static final String  charset               = "charset";
    //merchantId    String    M    商户号 ID
//    private static final String  merchantId               = "merchantId";
    //sign    String    M    签名
//    private static final String  sign               = "sign";
    //signType    String    M    签名方式：SHA
//    private static final String  signType               = "signType";

    //余额查询接口
    //请求参数    名称    类型    是否必填    说明
    //customerNo    String    M    我司分配的商户号ID
    private static final String  customerNo               = "customerNo";
    //signType    String    M    签名方式 ：SHA
//    private static final String  signType               = "signType";
//    //sign    String    M    签名
//    private static final String  sign               = "sign";

    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(batchAmount,handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(batchBiztype,"00000");
                payParam.put(batchCount,"1");
                payParam.put(batchDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                payParam.put(batchNo,channelWrapper.getAPI_ORDER_ID());
                payParam.put(batchVersion,"00");
                payParam.put(charset,"UTF-8");
                payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
                String my_batchContent = channelWrapper.getAPI_ORDER_ID() + "," + channelWrapper.getAPI_CUSTOMER_BANK_NUMBER() + "," + channelWrapper.getAPI_CUSTOMER_NAME() + 
                        "," + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0] + "," + channelWrapper.getAPI_CUSTOMER_BANK_BRANCH() + "," + channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH() + 
                        ",私," + handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) + ",CNY," +
                         channelWrapper.getAPI_CUSTOMER_BANK_BRANCH() + "," + channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH() + 
                         ",,,,," + channelWrapper.getAPI_ORDER_ID() + ",name";
                payParam.put(batchContent,my_batchContent);
                
                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder signSrc = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i))) && !signType.equals(paramKeys.get(i))) {
                        signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                    }
                }
                //去除最后一个&符
                signSrc.deleteCharAt(signSrc.length()-1);
                signSrc.append(channelWrapper.getAPI_KEY());
                String paramsStr = signSrc.toString();
                pay_md5sign = Sha1Util.getSha1(paramsStr).toUpperCase();

                payParam.put(signType,"SHA");
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                String url = channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]+channelWrapper.getAPI_MEMBERID()+"-"+channelWrapper.getAPI_ORDER_ID();
                        
                //发送请求获取结果
//                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
                String resultStr = RestTemplateUtil.postForm(url, payParam,"utf-8");

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
       if(1==2) throw new PayException("[德付代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(batchNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(batchDate, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMdd"));
            payParam.put(batchVersion,"00");
            payParam.put(charset,"UTF-8");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            
            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i))) && !signType.equals(paramKeys.get(i))) {
                    signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            //去除最后一个&符
            signSrc.deleteCharAt(signSrc.length()-1);
            signSrc.append(channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            pay_md5sign = Sha1Util.getSha1(paramsStr).toUpperCase();

            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
            payParam.put(signType,"SHA");

            String url = channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1]+channelWrapper.getAPI_MEMBERID()+"-"+channelWrapper.getAPI_ORDER_ID();
            
            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
//            String resultStr = RestTemplateUtil.postForm(url, payParam,"utf-8");
            String resultStr = RestTemplateUtil.sendByRestTemplate(url, payParam, String.class, HttpMethod.GET);
            
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[德付代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(customerNo,channelWrapper.getAPI_MEMBERID());

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i))) && !signType.equals(paramKeys.get(i))) {
                    signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            //去除最后一个&符
            signSrc.deleteCharAt(signSrc.length()-1);
            signSrc.append(channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            pay_md5sign = Sha1Util.getSha1(paramsStr).toUpperCase();

            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
            payParam.put(signType,"SHA");

            //发送请求获取结果
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam, String.class, HttpMethod.GET);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            //名称                类型                说明
            //status                String                succ：代表成功,fail：代表失败
            //reason                String                当status=succ，reason为余额，余额以分为单位；当status=fail，reason为错误原因。
            Map<String, String> rtnMap = XmlUtils.xmlToMap(resultStr);
            if("succ".equals(rtnMap.get("status")) && rtnMap.containsKey("reason") && StringUtils.isNotBlank( rtnMap.get("reason"))  ){
                String balance =  rtnMap.get("reason");
                return Long.parseLong(balance);
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[德付代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[德付代付][代付余额查询]出错,错误:%s",e.getMessage()));
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
            //respCode            respMessage            说明
            //S0001            请求已成功执行            请求已成功执行
            //F9999            服务器系统繁忙            服务器端发生异常
            if( HandlerUtil.valJsonObj(jsonObj,respCode,"S0001")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,respCode,"F9999")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            //返回的代付相应码（不能作为最终交易状态处理）
            //respCode            respMessage            说明
            //S0001            请求已成功执行            请求已成功执行
            //F9999            服务器系统繁忙            服务器端发生异常，请稍后重试。
            //F2001            没有发现该批次信息            无此订单信息
            //注意：代付查询接口，只有返回明确的“失败”状态，订单才能置为失败，其他的相应码只能是处理中，不能作为失败的依据（如响应码：F2001，请人工核实或隔天进行对账核实处理即可）
            if( HandlerUtil.valJsonObj(jsonObj,respCode,"S0001") && StringUtils.isNoneBlank(jsonObj.getString(batchContent))){
                String[] batchContentStr = jsonObj.getString(batchContent).split(",");
                //技术支持 2019/7/1 17:49:15
                //成功， 失败会 文字形式 显示。返回Null  就代表处理中。
                //"batchContent":"1,20190701162212004100,6217004160022335741,王小军,陕西省分行,永寿县支行,中国建设银行,0,3.00,CNY,name,,成功,"
                if ("成功".equals(batchContentStr[12])) {
                    return PayEumeration.DAIFU_RESULT.SUCCESS; 
                //技术支持 2019/7/2 11:17:01                    失败是返回 中文 失败。
                }else if ("失败".equals(batchContentStr[12])) {
                    return PayEumeration.DAIFU_RESULT.ERROR;
                }else if ("Null".equalsIgnoreCase(batchContentStr[12])) {
                    return PayEumeration.DAIFU_RESULT.PAYING;
                } else {
                    return PayEumeration.DAIFU_RESULT.UNKNOW;
                }
            }else if ( HandlerUtil.valJsonObj(jsonObj,respCode,"F2001")) {
                return PayEumeration.DAIFU_RESULT.PAYING;
            }else if ( HandlerUtil.valJsonObj(jsonObj,respCode,"F9999")) {
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            return PayEumeration.DAIFU_RESULT.UNKNOW;
//            throw new PayException(resultStr);
        }

    }








}