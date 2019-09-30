package dc.pay.business.baili1daifu;

/**
 * ************************
 * @author sunny
 */

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;


@RequestDaifuHandler("BAILI1DAIFU")
public final class BaiLi1DaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiLi1DaiFuRequestHandler.class);

     //请求代付&查询代付-参数
     private static final String  out_trade_no = "out_trade_no"; //   要查询的商户结算订单号
     private static final String  partner = "partner";       //   商户号，由平台分配
     private static final String  input_charset = "input_charset";  
     private static final String  sign_type = "sign_type";   //  是	3	签名使用的算法，目前只有MD5


     private static final String  amount_str="amount_str";       //	是	-	代付金额，单位元，可保留两位小数	100.12
     private static final String  bank_account_name="bank_account_name";       //	是	20	收款人银行账户名	张三
     private static final String  bank_account_no="bank_account_no";       //	是	50	收款银行账号	62284853451245345
     private static final String  bank_name="bank_name";       //	是	20	收款银行名
     private static final String  bank_site_name="bank_site_name";       //	bank_site_name	是	255	收款银行支行名
     private static final String  phone_num="phone_num";//是	50	收款人银行预留的手机号	13956485485
     private static final String  identity_code="identity_code";       //	金额	Number	是	分为单位；整数
     private static final String  remark="remark";       //	数字签名	String	是
     private static final String  return_url="return_url";  //	请求流水号	String	是	商户必须保证唯一
     private static final String  service="service";  //	请求时间戳	String	是	时间戳：(格式为yyyyMMddHHmmss 4位年+2位月+2位日+2位时+2位分+2位秒)
     private static final String  agent_time="agent_time";  //	平台订单号	String	否	平台唯一
     private static final String  sign="sign";  







    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                //组装参数
                payParam.put(partner,channelWrapper.getAPI_MEMBERID());
                payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                payParam.put(bank_site_name,channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
                payParam.put(bank_account_no,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(bank_account_name,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(amount_str,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(bank_name,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(phone_num,"13956485485");
                payParam.put(identity_code,"230000198808088888");
                payParam.put(input_charset,"UTF-8");
                payParam.put(remark,channelWrapper.getAPI_ORDER_ID());
                payParam.put(return_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.put(service,"agent_distribution");
                payParam.put(agent_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                payParam.put(sign_type,"MD5");

                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    if(StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                        continue;
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                sb.replace(sb.lastIndexOf("&"), sb.lastIndexOf("&") + 1, "" );	
                sb.append(channelWrapper.getAPI_KEY());
                String signStr = sb.toString(); //.replaceFirst("&key=","")
                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                //发送请求获取结果
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,null);
                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
                //addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());

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
       if(1==2) throw new PayException("[百利1][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(partner,channelWrapper.getAPI_MEMBERID());
            payParam.put(input_charset,"UTF-8");
            payParam.put(sign_type,"MD5");

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if(StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                    continue;
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.replace(sb.lastIndexOf("&"), sb.lastIndexOf("&") + 1, "" );	
            sb.append(channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam,null);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
    	throw new PayException("[百利1][代付余额查询]第三方不支持此功能。");
    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        if(!isQuery){
        	String[] resultArray=resultStr.trim().split("\\|");
            if(resultArray[1].equals("")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(!resultArray[1].equals("")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }else{
//        	状态	说明	-1	交易已取消   0	审核通过，正在处理   1 等待审核   2	审核未通过 3	付款失败 4	付款成功
        	JSONObject jsonObj = JSON.parseObject(resultStr);
            if(!jsonObj.getString("error_code").equals(""))  return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"trade_status","3")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if( HandlerUtil.valJsonObj(jsonObj,"trade_status","0","1","2")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"trade_status","4")) return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"trade_status","-1")) return PayEumeration.DAIFU_RESULT.UNKNOW;
            throw new PayException(resultStr);
        }

    }








}