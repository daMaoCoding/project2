package dc.pay.business.caifudaifu;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.sihaiyun.DigestUtil1;
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
@RequestDaifuHandler("CAIFUDAIFU")
public final class CaiFuDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CaiFuDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  p0_Cmd        =  "p0_Cmd";       //    业务类型 Max(20) 固定值” TransPay”
     private static final String  p1_MerId      =  "p1_MerId";//   商户编号 Max(18) 商户在彩付系统的 唯一身
     private static final String  p2_Order      =  "p2_Order";       //  商户订单号
     private static final String  p3_CardNo     =  "p3_CardNo";    //    卡号 Max(30) 代付银行卡号 4
     private static final String  p4_BankName   =  "p4_BankName";   //    银行名称 Max(30) 代付银行名称
     private static final String  p5_AtName     =  "p5_AtName"; //  账户名 Max(30)
     private static final String  p6_Amt   	    =  "p6_Amt";  //   金额 Max(30) 单位:元，精确到分. 7
     private static final String  pb_CusUserId  =  "pb_CusUserId";    //   商户用户 id Max(20) 非必填
     private static final String  pc_NewType    =  "pc_NewType";    //   代付类型 Max(10) 对私：PRIVATE；对公：
     private static final String  pd_BranchBankName  =  "pd_BranchBankName";    //  支行名称 Max(100) 代付类型为对公时必填
     private static final String  pg_Url        =  "pg_Url";    //  回调 url Max(100)
     private static final String  hmac     		=  "hmac";    //  请求流水号	String	是	商户必须保证唯一
     

    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回 PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {

            //组装参数
            payParam.put(p1_MerId,channelWrapper.getAPI_MEMBERID());
            payParam.put(p2_Order,channelWrapper.getAPI_ORDER_ID());
            payParam.put(p6_Amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(p4_BankName, channelWrapper.getAPI_CUSTOMER_BANK_NAME());
            payParam.put(p5_AtName,channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(p3_CardNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(pc_NewType,"PRIVATE");
            payParam.put(p0_Cmd,"TransPay");
            payParam.put(pg_Url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());

            //生成md5
            String signSrc=String.format("%s%s%s%s%s%s%s%s%s", 
            		payParam.get(p0_Cmd),	
            		payParam.get(p1_MerId),	
            		payParam.get(p2_Order),	
            		payParam.get(p3_CardNo),	
            		payParam.get(p4_BankName),	
            		payParam.get(p5_AtName),	
            		payParam.get(p6_Amt),	
            		payParam.get(pc_NewType),
            		payParam.get(pg_Url)
            );
            String paramsStr = signSrc.toString();
            String signMD5 =DigestUtil1.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL() ,payParam,null );
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
       if(1==2) throw new PayException("[彩付][代付][查询订单状态]该功能未完成。");
        try {


            //组装参数
        	payParam.put(p1_MerId,channelWrapper.getAPI_MEMBERID());
            payParam.put(p2_Order,channelWrapper.getAPI_ORDER_ID());
            payParam.put(p0_Cmd,"TransQuery");


            //生成md5
            String signSrc=String.format("%s%s%s", 
            		payParam.get(p0_Cmd),	
            		payParam.get(p1_MerId),	
            		payParam.get(p2_Order)	
            );
            String paramsStr = signSrc.toString();
            String  signMD5 =DigestUtil1.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
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
            payParam.put(p1_MerId,channelWrapper.getAPI_MEMBERID());
            payParam.put(p0_Cmd,"QueryMerchantBalance");

            //生成md5
            String signSrc=String.format("%s%s", 
            		payParam.get(p0_Cmd),	
            		payParam.get(p1_MerId)
            );
            String paramsStr = signSrc.toString();
            String signMD5 =DigestUtil1.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),signMD5);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);//请求状态(200：成功，500：系统异常，404：必填项为空)
            if(HandlerUtil.valJsonObj(jsonObj,"r1_Code","1") ){
            	return Long.parseLong(HandlerUtil.getFen(jsonObj.getString("r10_Amount")));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
        	
            log.error("[彩付代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[Hey代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
    	
        if(!isQuery){
        	resultStr=resultStr.replace("\n", ",");
        	String[] resultArr=resultStr.split(",");
            if (!(resultArr[2].indexOf("0000")!=-1)) return PayEumeration.DAIFU_RESULT.ERROR;
            if (resultArr[2].indexOf("0000")!=-1) return PayEumeration.DAIFU_RESULT.PAYING;
            //if (HandlerUtil.valJsonObj(jsonObj, "respCode", "00")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            return PayEumeration.DAIFU_RESULT.ERROR;
        }else {
        	JSONObject jsonObj = JSON.parseObject(resultStr);
            if (HandlerUtil.valJsonObj(jsonObj, "r1_Code", "9999")) return PayEumeration.DAIFU_RESULT.UNKNOW;
            if (HandlerUtil.valJsonObj(jsonObj, "r1_Code", "0000")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (HandlerUtil.valJsonObj(jsonObj, "r1_Code", "3003","3004")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (!HandlerUtil.valJsonObj(jsonObj, "r1_Code", "3003","3004","0000","9999")) return PayEumeration.DAIFU_RESULT.ERROR;
            new PayException(resultStr);
        }
       return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}