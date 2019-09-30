package dc.pay.business.shendedaifu;


import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;

import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.bsdaifu.SecurityUtils;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;

/**
 * @author sunny
 * 05 16, 2019
 */
@RequestDaifuHandler("SHENDEDAIFU")
public final class ShenDeDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShenDeDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  signMethod        	=  "signMethod";       //   系统分配的机构号，全局唯一，系统分配
     private static final String  sendTime  			=  "sendTime";// 金额	Decimal	是	是	单位（元）
     private static final String  merchantId      		=  "merchantId";       //   订单提交时间	String	是	是	时间戳格式(毫秒)，与服务器时间不能相差24小时
     private static final String  merOrderId     		=  "merOrderId";    //  回调地址	String	是	是	
     private static final String  accNo  				=  "accNo";   // 银行编码	String	是	是
     private static final String  customerNm     		=  "customerNm"; // 持卡人姓名
     private static final String  bankId   				=  "bankId";  //   收款账号支行名称
     private static final String  txnAmt    			=  "txnAmt";    //  分行名称	String	否	否
     private static final String  backUrl    			=  "backUrl";    //  分行名称	String	否	否
     private static final String  subject    			=  "subject";    //  分行名称	String	否	否
     private static final String  body    				=  "body";    //  分行名称	String	否	否
     private static final String  ppFlag    			=  "ppFlag";    //  分行名称	String	否	否
     private static final String  gateway    			=  "gateway";    //  分行名称	String	否	否
     
     private static final String  sign     			=  "sign";    //  请求流水号	String	是	商户必须保证唯一
     private static final String  key     			=  "key";    //  请求流水号	String	是	商户必须保证唯一
     

    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回 PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {

            //组装参数
        	payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(merOrderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(txnAmt,channelWrapper.getAPI_AMOUNT());
            payParam.put(customerNm,channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(accNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(bankId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(signMethod,"MD5");
            payParam.put(sendTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(backUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(ppFlag,"01");
            payParam.put(gateway,"daifu");

            //生成md5
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            paramKeys.remove(signMethod);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            //最后一个&转换成#
            signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
            signSrc.append(channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMD5 = java.util.Base64.getEncoder().encodeToString(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().getBytes());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            
            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/resolve/daifu";
            payParam.put(subject, java.util.Base64.getEncoder().encodeToString(channelWrapper.getAPI_ORDER_ID().getBytes()));
            payParam.put(body, java.util.Base64.getEncoder().encodeToString(channelWrapper.getAPI_ORDER_ID().getBytes()));
            String resultStr = RestTemplateUtil.postForm(url ,payParam,null);
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            //addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询 (无回调 自动查询)
      
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
       if(1==2) throw new PayException("[申德代付][代付][查询订单状态]该功能未完成。");
        try {

            //组装参数
        	payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
        	payParam.put(merOrderId,channelWrapper.getAPI_ORDER_ID());
        	payParam.put(signMethod,"MD5");
        	payParam.put(sendTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

            //生成md5
        	List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            paramKeys.remove(signMethod);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            //最后一个&转换成#
            signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
            signSrc.append(channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMD5 = java.util.Base64.getEncoder().encodeToString(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().getBytes());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/resolve/order/status";
            String resultStr = RestTemplateUtil.postForm(url, payParam,null);
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            //解析返回结果
            JSONObject responseJsonObject=null;
    		try {
    			responseJsonObject = JSONObject.parseObject(resultStr);
    		} catch (Exception e) {
    			e.printStackTrace();
    			throw new PayException(resultStr); 
    		}
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
        	payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
        	payParam.put(signMethod,"MD5");
        	payParam.put(sendTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

            //生成md5
        	List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            paramKeys.remove(signMethod);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            //最后一个&转换成#
            signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
            signSrc.append(channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMD5 = java.util.Base64.getEncoder().encodeToString(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().getBytes());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/resolve/balance";
            String resultStr = RestTemplateUtil.postForm(url, payParam,null);
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            String data="";
    		try {
    			JSONObject responseJsonObject = JSONObject.parseObject(resultStr);
    			JSONObject dataJsonObject=responseJsonObject.getJSONObject("balance");
                if(HandlerUtil.valJsonObj(responseJsonObject,"code","0") ){
                    String balance = dataJsonObject.getString("availableBalance");
                	if(NumberUtils.isNumber(balance)){
                		return Long.parseLong(balance);
                	}else{
                		 throw new PayException(resultStr);
                	}
                	
                }
    		} catch (Exception e) {
    			log.error("[申德代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
                throw new PayException( String.format("[申德代付][代付余额查询]出错,错误:%s",e.getMessage()));
    		}
    		
    		return Long.parseLong("0");
        } catch (Exception e){
        	
            log.error("[申德代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[申德代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
//          请求状态(成功:200 失败：1)
        if(!isQuery){
        	if (HandlerUtil.valJsonObj(jsonObj, "code", "1111")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (!HandlerUtil.valJsonObj(jsonObj, "code", "1111")) return PayEumeration.DAIFU_RESULT.ERROR;
            return PayEumeration.DAIFU_RESULT.ERROR;
        }else {
        	 if(HandlerUtil.valJsonObj(jsonObj, "code", "0")){
        		 if (HandlerUtil.valJsonObj(jsonObj, "transStatus", "1"))return PayEumeration.DAIFU_RESULT.SUCCESS;
    	         if (HandlerUtil.valJsonObj(jsonObj, "transStatus", "3")) return PayEumeration.DAIFU_RESULT.PAYING;
    	         if (!HandlerUtil.valJsonObj(jsonObj, "transStatus", "2")) return PayEumeration.DAIFU_RESULT.ERROR;
        	 }else{
        		 //订单不存在，请求超时的时候，自动查询
        		 if(HandlerUtil.valJsonObj(jsonObj, "code", "2002")){
        			 return PayEumeration.DAIFU_RESULT.ERROR; 
        		 }
        		 return PayEumeration.DAIFU_RESULT.UNKNOW; 
        	 }
	         new PayException(resultStr);
        }
       return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}