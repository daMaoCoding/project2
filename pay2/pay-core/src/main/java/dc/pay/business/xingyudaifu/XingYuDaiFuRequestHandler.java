package dc.pay.business.xingyudaifu;


import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;

import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.bsdaifu.SecurityUtils;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;

/**
 * @author sunny
 * @date 10 Aug 2019
 */
@RequestDaifuHandler("XINGYUDAIFU")
public final class XingYuDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XingYuDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  out_trade_no        	=  "out_trade_no";       //   商户订单号	String	是	是
     private static final String  card_no  				=  "card_no";// 金额	Decimal	是	是	单位（元）
     private static final String  account_name      	=  "account_name";       //   订单提交时间	String	是	是	时间戳格式(毫秒)，与服务器时间不能相差24小时
     private static final String  bank_code     		=  "bank_code";    //  回调地址	String	是	是	
     private static final String  bank_name    			=  "bank_name";   // 银行编码	String	是	是
     private static final String  total_fee   			=  "total_fee";  //   持卡人名字	String	是	是
     private static final String  id_no    				=  "id_no";    //  分行名称	String	否	否
     private static final String  id_front   			=  "id_front";    //  订单备注	String	否	否
     private static final String  id_back   			=  "id_back";    //  订单备注	String	否	否
     private static final String  phone   				=  "phone";    //  订单备注	String	否	否
     private static final String  mch_id   				=  "mch_id";    //  订单备注	String	否	否
     private static final String  bank_branch   	    =  "bank_branch";    //  订单备注	String	否	否
     private static final String  cnaps_no   	    	=  "cnaps_no";    //  订单备注	String	否	否
     
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
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(account_name,channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(card_no,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(bank_name,channelWrapper.getAPI_CUSTOMER_BANK_NAME());
            payParam.put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(mch_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(id_no,"526529200005093736");
            payParam.put(id_front,"");
            payParam.put(id_back,"");
            payParam.put(phone,"13212539920");
            payParam.put(phone,"13212539920");
            payParam.put(bank_branch,"");
            payParam.put(cnaps_no,"");

          //生成md5
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
            	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "");
            signSrc.append(channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/api/withdraw2";
            //发送请求获取结果
            dc.pay.business.dufuzhifu.HttpClientUtil client=  new dc.pay.business.dufuzhifu.HttpClientUtil();
            String resultStr = client.doPost(url, payParam, "UTF-8");
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
       if(1==2) throw new PayException("[星宇代付][代付][查询订单状态]该功能未完成。");
        try {

            //组装参数
        	payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
        	payParam.put(mch_id,channelWrapper.getAPI_MEMBERID());

            //生成md5
        	List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
            	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "");
            signSrc.append(channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/api/withdrawstatus";
            //发送请求获取结果
            //String resultStr = RestTemplateUtil.sendByRestTemplate(url, payParam, String.class, HttpMethod.POST);
            
            dc.pay.business.dufuzhifu.HttpClientUtil client=  new dc.pay.business.dufuzhifu.HttpClientUtil();
            String resultStr =client.doPost(url, payParam, "UTF-8");
           // String resultStr = RestTemplateUtil.postJson(url, payParam);
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
    	throw new PayException("[星宇代付][代付余额查询]第三方不支持此功能。");
    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
//          请求状态(成功:200 失败：1)
        if(!isQuery){
            if (!HandlerUtil.valJsonObj(jsonObj, "error_code", "0")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "error_code", "0")) return PayEumeration.DAIFU_RESULT.PAYING;
            return PayEumeration.DAIFU_RESULT.ERROR;
        }else {
        	if(HandlerUtil.valJsonObj(jsonObj, "error_code", "0")){
        		if (HandlerUtil.valJsonObj(jsonObj, "withdraw_status", "0")) return PayEumeration.DAIFU_RESULT.PAYING;
     	        if (HandlerUtil.valJsonObj(jsonObj, "withdraw_status", "1"))return PayEumeration.DAIFU_RESULT.SUCCESS;
     	        if (HandlerUtil.valJsonObj(jsonObj, "withdraw_status", "2")) return PayEumeration.DAIFU_RESULT.ERROR;
     	        if (!HandlerUtil.valJsonObj(jsonObj, "withdraw_status","0","1","2")) return PayEumeration.DAIFU_RESULT.UNKNOW;
        	}else{
        		return PayEumeration.DAIFU_RESULT.ERROR;
        	}
	        new PayException(resultStr);
        }
       return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}