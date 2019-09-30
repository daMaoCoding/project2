package dc.pay.business.guomeidaifu;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author sunny
 * 05 16, 2019
 */
@RequestDaifuHandler("GUOMEIDAIFU")
public final class GuoMeiDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GuoMeiDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  mer_no        =  "mer_no";       //    商户号	String	是	平台分配的唯一商户编号
     private static final String  mer_order_no  =  "mer_order_no";//    商户订单号	String	是	商户必须保证唯一
     private static final String  acc_type      =  "acc_type";       //   账户类型	Number	是	1:对私 2：对公
     private static final String  acc_no     	=  "acc_no";    //    收款账号	String	是	收款账号
     private static final String  acc_name    	=  "acc_name";   //    收款户名	String	是	收款户名
     private static final String  order_amount  =  "order_amount"; //  金额	Number	是	分为单位；整数
     private static final String  bank_no   	=  "bank_no";  //   开户行号	String	否	开户行号（联行号）；对公必填
     private static final String  bank_name     =  "bank_name";    //   开户行名	String	否	开户行名称；对接时候需询问是否必填
     private static final String  key     =  "key";    //   开户行名	String	否	开户行名称；对接时候需询问是否必填
     private static final String  request_no     =  "request_no";    //  请求流水号	String	是	商户必须保证唯一
     private static final String  request_time     =  "request_time";    //  请求流水号	String	是	商户必须保证唯一
     private static final String  province     =  "province";    //  请求流水号	String	是	商户必须保证唯一
     private static final String  city     =  "city";    //  请求流水号	String	是	商户必须保证唯一
     

    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回 PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {

            //组装参数
            payParam.put(mer_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(order_amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(bank_name, channelWrapper.getAPI_CUSTOMER_BANK_NAME());
            payParam.put(acc_name,channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(acc_no,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(acc_type,"1");
            payParam.put(province,"贵州省");
            payParam.put(city,"贵阳市");

            //生成md5
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            signSrc.append(key+"="+channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/singleOrder";
            //发送请求获取结果
            String resultStr = RestTemplateUtil.postJson(url ,payParam );
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询 (无回调 自动查询)
      
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
       if(1==2) throw new PayException("[国美][代付][查询订单状态]该功能未完成。");
        try {


            //组装参数
        	payParam.put(request_no,java.util.UUID.randomUUID()+"");
            payParam.put(mer_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(request_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));


            //生成md5
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            signSrc.append(key+"="+channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/singleQuery";
            String resultStr = RestTemplateUtil.postJson(url, payParam);
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            //发送请求获取结果
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常(无查余额接口)
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {

        try {
            //组装参数
        	payParam.put(request_no,java.util.UUID.randomUUID()+"");
            payParam.put(mer_no,channelWrapper.getAPI_MEMBERID());
            payParam.put(request_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

            //生成md5
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            signSrc.append(key+"="+channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/balanceQuery";
            String resultStr = RestTemplateUtil.postJson(url, payParam);
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);//请求状态(200：成功，500：系统异常，404：必填项为空)
            if(HandlerUtil.valJsonObj(jsonObj,"query_status","SUCCESS") ){
              //  String balance = jsonObj.getString("balance");
            	JSONArray arrs=jsonObj.getJSONArray("list");
            	if(arrs.isEmpty()){
            		  log.error("[国美代付][代付余额查询]出错,返回结果：{}",resultStr);
            	}
            	JSONObject balaJson=arrs.getJSONObject(0);
            	return Long.parseLong(HandlerUtil.getFen(balaJson.getString("balance")));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
        	
            log.error("[国美代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[Hey代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
//          请求状态(成功:success 失败：FAIL)
        if(!isQuery){
            if (HandlerUtil.valJsonObj(jsonObj, "status", "FAIL")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "SUCCESS")) return PayEumeration.DAIFU_RESULT.PAYING;
            //if (HandlerUtil.valJsonObj(jsonObj, "respCode", "00")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            return PayEumeration.DAIFU_RESULT.ERROR;
        }else {
//          请求状态(status    状态    是    success:请求成功（不代表业务成功），error：请求失败)
//          1 成功   2 失败   3 处理中    4 待处理    5 审核驳回    6 待审核  7 交易不存在  8 未知状态
//          当status=success和refCode=1同时成立时才表示转账成功
            if (HandlerUtil.valJsonObj(jsonObj, "status", "FAIL")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "UNKNOW")) return PayEumeration.DAIFU_RESULT.UNKNOW;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "SUCCESS") && HandlerUtil.valJsonObj(jsonObj, "err_code", "000000") )
                return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (!HandlerUtil.valJsonObj(jsonObj, "err_code", "000000")) return PayEumeration.DAIFU_RESULT.ERROR;
            new PayException(resultStr);
        }
       return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}