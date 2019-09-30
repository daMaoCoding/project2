package dc.pay.business.fanchaodaifu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 * May 17, 2019
 */
@RequestDaifuHandler("FANCHAODAIFU")
public final class FanChaoDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FanChaoDaiFuRequestHandler.class);

    
    //参数名称    参数变量名   类型  必填  说明
  //2.7.1参数
      //参数名称    参数含义    长度  是否必填    备注
      //service 接口类型    32  是   trade.payment
      private static final String service                ="service";
      //version 版本号 10  是   固定值：1.0
      private static final String version                ="version";
      //merchantId  商户ID    10  是   商户在中心的唯一标识
      private static final String merchantId                ="merchantId";
      //orderNo 商户订单号   40  是   提交的订单号在商户系统中必须唯一
      private static final String orderNo                ="orderNo";
      //tradeDate   查询交易日期  8   是   商户交易日期，格式：yyyyMMdd
      private static final String tradeDate                ="tradeDate";
      //tradeTime   查询交易时间  6   是   商户交易时间，格式：HHmmss
      private static final String tradeTime                ="tradeTime";
      //amount  交易金额    10  是   交易金额 （单位：分）
      private static final String amount                ="amount";
      //clientIp    客户端IP   16  是   
      private static final String clientIp                ="clientIp";
      //bankCode    开户行行别   4   是   四位行别，请参考5.3
      private static final String bankCode                ="bankCode";
      //bankBranchName  支行名称    30  是   支行名称
      private static final String bankBranchName                ="bankBranchName";
      //province    开户省 50  是   开户省
      private static final String province                ="province";
      //city    开户市 50  是   开户市
      private static final String city                ="city";
      //benAcc  收款人账号   60  是   收款人账号
      private static final String benAcc                ="benAcc";
      //benName 收款人账户户名 50  是   收款人账户户名
      private static final String benName                ="benName";
      //accType 账户类型    1   是   1：对私账号    2：对公账号
      private static final String accType                ="accType";
      //bankLinked  联行号 20  否   对私账户不传，对公账户必传
      private static final String bankLinked                ="bankLinked";
      //cellPhone   收款人手机号  20  否   收款人手机号
      private static final String cellPhone                ="cellPhone";
      //identityType    收款人证件类型 2   是   收款人证件类型    01: 身份证    目前只支持身份证
      private static final String identityType                ="identityType";
      //identityNo  收款人证件号码 20  是   收款人证件号码
      private static final String identityNo                ="identityNo";
      //release 放行状态    1   是   0: 人工放行    1: 自动放行
      private static final String release                ="release";
      //notifyUrl   商户接收后台返回结果的地址   200 是   交易成功后，向该网址发送三次成功通知。
      private static final String notifyUrl                ="notifyUrl";
      //attach  商户附加信息  60  否   商户扩展信息，返回时原样返回，此参数如用到中文，请注意转码
      private static final String attach                ="attach";
      //sign    签名数据    32  是   32位小写的组合加密验证串
      private static final String sign                ="sign";

      //totalCount  总笔数 10  否   订单拆分笔数
      private static final String totalCount                ="totalCount";

    //响应参数定义：以 json 格式同步返回响应数据
    

    //支付接口请求方式：支持 GET 或 POST 支付接口请求地址：
    


    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
            
//            //service 接口类型    32  是   trade.payment
//            private static final String                 ="service";
//            //version 版本号 10  是   固定值：1.0
//            private static final String version                ="version";
//            //merchantId  商户ID    10  是   商户在中心的唯一标识
//            private static final String merchantId                ="merchantId";
//            //orderNo 商户订单号   40  是   提交的订单号在商户系统中必须唯一
//            private static final String orderNo                ="orderNo";
//            //tradeDate   查询交易日期  8   是   商户交易日期，格式：yyyyMMdd
//            private static final String                 ="tradeDate";
//            //tradeTime   查询交易时间  6   是   商户交易时间，格式：HHmmss
//            private static final String                 ="tradeTime";
//            //amount  交易金额    10  是   交易金额 （单位：分）
//            private static final String                 ="amount";
//            //clientIp    客户端IP   16  是   
//            private static final String                 ="clientIp";
//            //bankCode    开户行行别   4   是   四位行别，请参考5.3
//            private static final String                 ="bankCode";
//            //bankBranchName  支行名称    30  是   支行名称
//            private static final String                 ="bankBranchName";
//            //province    开户省 50  是   开户省
//            private static final String                 ="province";
//            //city    开户市 50  是   开户市
//            private static final String                 ="city";
//            //benAcc  收款人账号   60  是   收款人账号
//            private static final String                 ="benAcc";
//            //benName 收款人账户户名 50  是   收款人账户户名
//            private static final String                 ="benName";
//            //accType 账户类型    1   是   1：对私账号    2：对公账号
//            private static final String                 ="accType";
//            //bankLinked  联行号 20  否   对私账户不传，对公账户必传
//            private static final String bankLinked                ="bankLinked";
//            //cellPhone   收款人手机号  20  否   收款人手机号
//            private static final String cellPhone                ="cellPhone";
//            //identityType    收款人证件类型 2   是   收款人证件类型    01: 身份证    目前只支持身份证
//            private static final String identityType                ="identityType";
//            //identityNo  收款人证件号码 20  是   收款人证件号码
//            private static final String identityNo                ="identityNo";
//            //release 放行状态    1   是   0: 人工放行    1: 自动放行
//            private static final String                 ="release";
//            //notifyUrl   商户接收后台返回结果的地址   200 是   交易成功后，向该网址发送三次成功通知。
//            private static final String notifyUrl                ="notifyUrl";
//            //attach  商户附加信息  60  否   商户扩展信息，返回时原样返回，此参数如用到中文，请注意转码
//            private static final String attach                ="attach";
//            //sign    签名数据    32  是   32位小写的组合加密验证串
//            private static final String sign                ="sign";
                payParam.put(service,"trade.payment");
                payParam.put(version,"1.0");
                payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
                payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
                payParam.put(tradeDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                payParam.put(tradeTime,DateUtil.formatDateTimeStrByParam("HHmmss"));
                payParam.put(amount,channelWrapper.getAPI_AMOUNT());
                payParam.put(clientIp,channelWrapper.getAPI_Client_IP());
                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(bankBranchName,channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH());
                payParam.put(province,channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
                payParam.put(city,channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH());
                payParam.put(benAcc,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(benName,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(accType,"1");
                payParam.put(identityType,"01");
                //柳萌        441624197902055982  女   38  广东省 河源市 和平县
                payParam.put(identityNo,"441624197902055982");
                payParam.put(release,"1");
                payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                

                Map<String,String> map = new TreeMap<>(payParam);
                map.put("key", channelWrapper.getAPI_KEY());
                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(map);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                //删除最后一个字符
                sb.deleteCharAt(sb.length()-1);
//                sb.append("key=" + channelWrapper.getAPI_KEY());
                String signStr = sb.toString(); //.replaceFirst("&key=","")

                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                //发送请求获取结果
                System.out.println("代付请求地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]);
                System.out.println("代付请求参数==>"+JSON.toJSONString(payParam));
                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");

                System.out.println("代付请求返回==>"+resultStr);
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
       if(1==2) throw new PayException("[凡超代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
//            ※tradeDate是上送交易的日期，並非訂單提交日期
//            参数名称    参数含义    长度  是否必填    备注
//             接口类型    32  是   trade.payment.query
//             版本号 10  是   固定值：1.0
//              商户ID    10  是   商户在中心的唯一标识
//             商户订单号   40  是   商户提交的订单号
//            tradeDate   查询交易日期  8   是   查询交易日期(非订单日期)，格式：yyyyMMdd
//            tradeTime   查询交易时间  6   是   查询交易时间(非订单时间)，格式：HHmmss
//            sign    签名数据    32  是   32位小写的组合加密验证串
            payParam.put(service,"trade.payment.query");
            payParam.put(version,"1.0");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
            // TODO
            payParam.put(tradeDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            payParam.put(tradeTime,DateUtil.formatDateTimeStrByParam("HHmmss"));
            
            
            Map<String,String> map = new TreeMap<>(payParam);
            map.put("key", channelWrapper.getAPI_KEY());
            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(map);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            //删除最后一个字符
            sb.deleteCharAt(sb.length()-1);
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
            System.out.println("代付查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1]);
            System.out.println("代付查询参数==>"+JSON.toJSONString(payParam));
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,"utf-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            System.out.println("代付查询返回==>"+resultStr);
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[凡超代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            
//            2.10.1参数
//            参数名称    参数含义    长度  是否必填    备注
//            service 接口类型    32  是   trade.fund.query
//             版本号 10  是   固定值：1.0
//            merchantId  商户ID    10  是   商户在中心的唯一标识
//            orderNo 交易编号    40  是   每次请求此交易的唯一编号
//            tradeDate   查询交易日期  8   是   交易日期，格式：yyyyMMdd
//            tradeTime   查询交易时间  6   是   交易时间，格式：HHmmss
//            sign    签名数据    32  是   32位小写的组合加密验证串
            payParam.put(service,"trade.fund.query");
            payParam.put(version,"1.0");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(tradeDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            payParam.put(tradeTime,DateUtil.formatDateTimeStrByParam("HHmmss"));
            
            Map<String,String> map = new TreeMap<>(payParam);
            map.put("key", channelWrapper.getAPI_KEY());
            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(map);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            //删除最后一个字符
            sb.deleteCharAt(sb.length()-1);
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);



            //发送请求获取结果
                System.out.println("代付余额查询地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2]);
                System.out.println("代付余额查询参数==>"+JSON.toJSONString(payParam));
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], payParam);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

                System.out.println("代付余额查询返回==>"+resultStr);

            JSONObject jsonObj = JSON.parseObject(resultStr);
            //如果有验证，最好调用一下验证方法
            if(HandlerUtil.valJsonObj(jsonObj,"status","success") && jsonObj.containsKey("amount") && StringUtils.isNotBlank( jsonObj.getString("amount"))  ){
                String balance =  jsonObj.getString("amount");
//                return Long.parseLong(balance);
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[凡超代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[凡超代付][代付余额查询]出错,错误:%s",e.getMessage()));
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
            //返回代码    說明
            //0001    讯息成功      TODO
            //客服003
            //“🙈   11:13:06  查看原文
            //是不是0001外，其他返回代付请求值我都可认为代付请求下发本订单失败。可重新下发？
             //对哦@🙈
            if( HandlerUtil.valJsonObj(jsonObj,"repCode","0101","0102","0103","0104","0105","0106","0107","0108","0109","0110","0111","0112","0113","0114","0115","0116","0117","0118","0119","0120","0121","0122","0123","0124","0125","0126","0127","0128","0129","0131","0132","0133","0134","0135","0136","0137","0138","0139","0140","0141","0201","0202","0203","0204","0205","0206","0207","0208","0209","0210","0211","0212","0213","0214","0215","0216","0301","0601","6101","6102","6103","6104","7001")) return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"repCode","0001")) return PayEumeration.DAIFU_RESULT.PAYING;
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            //resultCode  交易结果    5   否   只有当返回码为讯息成功时才返回            0：未处理            1：处理中            2：已处理            4：汇出退回            5：订单不存在
            //repCode 返回码 16  是   返回码，参考3.2返回码说明
            if( HandlerUtil.valJsonObj(jsonObj,"repCode","0001")){
                //resultCode  交易结果    5   否   只有当返回码为讯息成功时才返回            0：未处理            1：处理中            2：已处理            4：汇出退回            5：订单不存在
                //  TODO
                if( HandlerUtil.valJsonObj(jsonObj,"resultCode","0")) return PayEumeration.DAIFU_RESULT.UNKNOW;
                if( HandlerUtil.valJsonObj(jsonObj,"resultCode","4","5")) return PayEumeration.DAIFU_RESULT.ERROR;
                if( HandlerUtil.valJsonObj(jsonObj,"resultCode","1")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj,"resultCode","2")){
                    //※代付判断成功&失败条件
                    //1. reusltCode=2
                    //2. outCount=0表示失败
                    //3. 检查outAmount和outCount是否等于amount和totalCount，相等表示成功；不等表示失败
                    //  TODO
                    if( HandlerUtil.valJsonObj(jsonObj,"outCount","0")) return PayEumeration.DAIFU_RESULT.ERROR;
                    if( HandlerUtil.valJsonObj(jsonObj,"outAmount",jsonObj.getString(amount)) && HandlerUtil.valJsonObj(jsonObj,"outCount",jsonObj.getString(totalCount))){
                        return PayEumeration.DAIFU_RESULT.SUCCESS;
                    }else {
                        return PayEumeration.DAIFU_RESULT.ERROR;
                    }
                }
                throw new PayException(resultStr);
            // 客服003
            // ※代付判断成功&失败条件
            // 1. reusltCode=2
            // 2. outCount=0表示失败
            // 3. 检查outAmount和outCount是否等于amount和totalCount，相等表示成功；不等表示失败
            // 返回碼是當次請求的狀態(請求成功或失敗)，跟訂單狀態無關
            //保险起见，设置为PAYING
            }else if ( HandlerUtil.valJsonObj(jsonObj,"repCode","0101","0102","0103","0104","0105","0106","0107","0108","0109","0110","0111","0112","0113","0114","0115","0116","0117","0118","0119","0120","0121","0122","0123","0124","0125","0126","0127","0128","0129","0131","0132","0133","0134","0135","0136","0137","0138","0139","0140","0141","0201","0202","0203","0204","0205","0206","0207","0208","0209","0210","0211","0212","0213","0214","0215","0216","0301","0601","6101","6102","6103","6104","7001")) {
                return PayEumeration.DAIFU_RESULT.PAYING;
            }
            throw new PayException(resultStr);
        }

    }








}