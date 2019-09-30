package dc.pay.business.rongjinfudaifu;


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

import org.apache.commons.lang.NumberUtils;
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
@RequestDaifuHandler("RONGJINFUDAIFU")
public final class RongJinFuDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongJinFuDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  mer_id        =  "mer_id";       //   合作商户编号（支付平台分配）
     private static final String  pay_type  	=  "pay_type";//  固定为0
     private static final String  order_id      =  "order_id";       //   必须唯一，不超过30字符（商户系统生成）
     private static final String  order_amt     =  "order_amt";    //   订单金额不可为空，取值范围，单位：元
     private static final String  acct_name    	=  "acct_name";   //   收款人姓名
     private static final String  acct_id  		=  "acct_id"; // 收款帐号，银行卡帐号
     private static final String  acct_type   	=  "acct_type";  //   0-借记卡，1-贷记卡，2-对公账号
     private static final String  bank_code     =  "bank_code";    //  银行缩写参考附录4的支持银行的英文缩写
     private static final String  bank_branch   =  "bank_branch";    //  银行支行名称
     private static final String  cerd_id     	=  "cerd_id";    //  银行预留持卡人身份证号码
     private static final String  phone_no      =  "phone_no";    //  银行预留持卡人手机号
     private static final String  time_stamp    =  "time_stamp";    // 提交时间戳(格式为yyyyMMddHHmmss 4位年+2位月+2位日+2位时+2位分+2位秒)
     private static final String  sign     		=  "sign";    //  请求流水号	String	是	商户必须保证唯一
     private static final String  key     		=  "key";    //  请求流水号	String	是	商户必须保证唯一
     

    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回 PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {

            //组装参数
            payParam.put(mer_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(order_id,channelWrapper.getAPI_ORDER_ID());
            payParam.put(order_amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(time_stamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(acct_name,channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(acct_id,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(pay_type,"0");
            payParam.put(acct_type,"0");
            payParam.put(bank_branch,channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH());
            payParam.put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(cerd_id,"621700416002233574");
            payParam.put(phone_no,"13333556644");

            //生成md5
            String signSrc=String.format("%s%s%s%s%s%s%s%s", 
            		mer_id+"="+payParam.get(mer_id)	+"&",
            		pay_type+"="+payParam.get(pay_type)	+"&",
            		order_id+"="+payParam.get(order_id)	+"&",
            		order_amt+"="+payParam.get(order_amt)	+"&",
            		acct_name+"="+payParam.get(acct_name)	+"&",
            		acct_id+"="+payParam.get(acct_id)	+"&",
            		time_stamp+"="+payParam.get(time_stamp)	+"&",
            		key+"=" +HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()).toLowerCase()
            );
            String paramsStr = signSrc.toString();
            String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/Pay/GateWayPement.shtml";
            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(url ,payParam,null);
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
       if(1==2) throw new PayException("[融金付][代付][查询订单状态]该功能未完成。");
        try {

            //组装参数
        	payParam.put(mer_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(order_id,channelWrapper.getAPI_ORDER_ID());
            payParam.put(time_stamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

            //生成md5
            String signSrc=String.format("%s%s%s%s", 
            		mer_id+"="+payParam.get(mer_id)	+"&",
            		order_id+"="+payParam.get(order_id)	+"&",
            		time_stamp+"="+payParam.get(time_stamp)	+"&",
            		key+"=" +HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()).toLowerCase()
            );
            String paramsStr = signSrc.toString();
            String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/Pay/PementQuery.shtml";
            String resultStr = RestTemplateUtil.postForm(url, payParam,null);
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
        	payParam.put(mer_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(pay_type,"0");
            payParam.put(time_stamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

            //生成md5
            String signSrc=String.format("%s%s%s%s", 
            		mer_id+"="+payParam.get(mer_id)	+"&",
            		pay_type+"="+payParam.get(pay_type)	+"&",
            		time_stamp+"="+payParam.get(time_stamp)	+"&",
            		key+"=" +HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()).toLowerCase()
            );
            String paramsStr = signSrc.toString();
            String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/Pay/PementAmountQuery.shtml";
            String resultStr = RestTemplateUtil.postForm(url, payParam,null);
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);//请求状态(200：成功，500：系统异常，404：必填项为空)
            if(HandlerUtil.valJsonObj(jsonObj,"status_code","0") ){
                String balance = jsonObj.getString("cur_balance");
            	if(NumberUtils.isNumber(balance)){
            		return Long.parseLong(HandlerUtil.getFen(balance));
            	}else{
            		 throw new PayException(resultStr);
            	}
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
//          请求状态(成功:0 失败：1)
        if(!isQuery){
            if (!HandlerUtil.valJsonObj(jsonObj, "status_code", "0")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "status_code", "0")) return PayEumeration.DAIFU_RESULT.PAYING;
            return PayEumeration.DAIFU_RESULT.ERROR;
        }else {
        	
        	if(HandlerUtil.valJsonObj(jsonObj, "status_code", "0")){
        		 if (HandlerUtil.valJsonObj(jsonObj, "trade_status", "2")) return PayEumeration.DAIFU_RESULT.ERROR;
                 if (!HandlerUtil.valJsonObj(jsonObj, "trade_status", "0","1","2","3")) return PayEumeration.DAIFU_RESULT.UNKNOW;
                 if (HandlerUtil.valJsonObj(jsonObj, "trade_status", "1")) return PayEumeration.DAIFU_RESULT.PAYING;
                 if (HandlerUtil.valJsonObj(jsonObj, "trade_status", "0"))
                     return PayEumeration.DAIFU_RESULT.SUCCESS;
                 new PayException(resultStr);
        	}else if(HandlerUtil.valJsonObj(jsonObj, "status_code", "15")||(jsonObj.containsKey("status_msg")&&jsonObj.getString("status_msg").equals("此订单不存在"))){
        		return PayEumeration.DAIFU_RESULT.ERROR;
        	}else{
        		 new PayException(resultStr);
        	}
           
        }
       return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}