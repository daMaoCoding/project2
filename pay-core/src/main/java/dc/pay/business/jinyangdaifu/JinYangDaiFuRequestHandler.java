package dc.pay.business.jinyangdaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


@RequestDaifuHandler("JINYANGDAIFU")
public final class JinYangDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinYangDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
    private static final String  amount="amount";    //	出款金额	string
    private static final String  mchtid="mchtid";    //	商户ID	Int32
    private static final String  version="version";    //	版本号	string	8
    private static final String  bankaccountname="bankaccountname";    //	开户姓名	string	10
    private static final String  bankaccountno="bankaccountno";    //	银行卡号	string	12至24
    private static final String  branchname="branchname";    //	支行名称	string	120
    private static final String  bankcode="bankcode";    //	银行代码	string	30
    private static final String  transtime="transtime";    //	出款时间	Int64
    private static final String  transid="transid";    //	交易流水号	string	[A-Za-z0-9]{5,26}
    private static final String  notifyurl="notifyurl";    //	商户通知地址
    private static final String  userip="userip";    //	用户IP
    private static final String  sign="sign";    //	数据签名	string	514







    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
            if(!channelWrapper.getAPI_KEY().contains("&")){
                details.put(RESPONSEKEY, "密钥填写错误，格式：[代付3DESKEY]&[代付MD5KEY],如：ABCD&1234");//强制必须保存下第三方结果
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
                String deskey = channelWrapper.getAPI_KEY().split("&")[0];
                String md5key = channelWrapper.getAPI_KEY().split("&")[1];


                //组装参数
                payParam.put(amount, JinYangDaiFuUtil.Encrypt3DES(HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()),deskey));
                payParam.put(mchtid,channelWrapper.getAPI_MEMBERID());
                payParam.put(version,"v1.0.0.0");
                payParam.put(bankaccountname,JinYangDaiFuUtil.Encrypt3DES(channelWrapper.getAPI_CUSTOMER_NAME(),deskey));
                payParam.put(bankaccountno,JinYangDaiFuUtil.Encrypt3DES(channelWrapper.getAPI_CUSTOMER_BANK_NUMBER(),deskey));
                payParam.put(branchname,JinYangDaiFuUtil.Encrypt3DES(channelWrapper.getAPI_CUSTOMER_BANK_BRANCH(),deskey));
                payParam.put(bankcode,JinYangDaiFuUtil.Encrypt3DES(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG(),deskey));
                payParam.put(transtime,channelWrapper.getAPI_OrDER_TIME());
                payParam.put(transid, channelWrapper.getAPI_ORDER_ID());

                //生成md5
                String reqParamJsonStr = JinYangDaiFuUtil.mapToParamByKeysSort(payParam)+"&key="+md5key;
                String pay_md5sign = JinYangDaiFuUtil.MD5(reqParamJsonStr, "UTF-8").toLowerCase();// 32位
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
                payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );

                //发送请求获取结果
                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

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
       if(1==2) throw new PayException("[金阳代付][代付][查询订单状态]该功能未完成。");
        try {
            if(!channelWrapper.getAPI_KEY().contains("&")){
                throw new PayException("密钥填写错误，格式：[代付3DESKEY]&[代付MD5KEY],如：ABCD&1234");
            }
            String deskey = channelWrapper.getAPI_KEY().split("&")[0];
            String md5key = channelWrapper.getAPI_KEY().split("&")[1];


            //组装参数
            payParam.put(mchtid, channelWrapper.getAPI_MEMBERID());//商户号
            payParam.put(version, "v1.0.0.0");//版本号，固定
            payParam.put(transtime, channelWrapper.getAPI_OrDER_TIME());//出款时间戳
            payParam.put(transid, channelWrapper.getAPI_ORDER_ID());//订单号
            String reqParamJsonStr = JinYangDaiFuUtil.mapToParamByKeysSort(payParam)+"&key="+md5key;


            //生成md5
            String pay_md5sign = JinYangDaiFuUtil.MD5(reqParamJsonStr, "UTF-8").toLowerCase();// 32位
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], payParam);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
         throw new PayException("[金阳代付][代付余额查询]第三方不支持该功能。");
    }




    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);

        if(HandlerUtil.valJsonObj(jsonObj,"code","1")   ){  //第三方明确成功返回结果
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"data","state","0","1","100")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"data","state","2")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if(HandlerUtil.valJsonObjInSideJsonObj(jsonObj,"data","state","3","120")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }  else {
            if(isQuery){
                if(resultStr.contains("商户流水不存在")) return PayEumeration.DAIFU_RESULT.ERROR;  // 查询时候，指定错误内容说明订单号没有被提交到第三方直接返回订单取消。
                throw new PayException(resultStr);        //查询时候第三方未明确指定，订单状态失败，我们未知,抛异常返回unKnow。
            }
            return PayEumeration.DAIFU_RESULT.ERROR;
        }
    }









}