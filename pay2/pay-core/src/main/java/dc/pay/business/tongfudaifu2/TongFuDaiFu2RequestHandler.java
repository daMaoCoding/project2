package dc.pay.business.tongfudaifu2;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Cobby
 * May 23, 2019
 */
@RequestDaifuHandler("TONGFUDAIFU2")
public final class TongFuDaiFu2RequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongFuDaiFu2RequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  fxid         = "fxid";       //    商务号      是    唯一号，由通付提供
     private static final String  fxaction     = "fxaction";   //    商户查询动作 是    商户查询动作，这里填写【repay】
     private static final String  fxnotifyurl  = "fxnotifyurl";//    异步返回地址 是    异步接收支付结果通知的回调地址
     private static final String  fxbody       = "fxbody";     //    订单信息域   是    提交代付订单信息参考下方订单信息域（json格式字符串）
//   private static final String  fxsign       = "fxsign";     //    签名【md5(商务号+商户查询动作+订单信息域（json格式字符串）+商户秘钥)】

     private static final String  fxddh        = "fxddh";      //  商户订单号     是    仅允许字母或数字类型,不超过22个字符，不要有中文
     private static final String  fxdate       = "fxdate";     //  交易时间       是    格式YYYYMMDDhhmmss
     private static final String  fxfee        = "fxfee";      //  金额           是    代付金额 单位0.01元
//   private static final String  fxbody       = "fxbody";     //  收款账户       是    收款人的账户
     private static final String  fxname       = "fxname";     //  开户名         是    收款人的开户名
     private static final String  fxaddress    = "fxaddress";  //  开户行         是    收款人的开户行，例如中国银行
//   private static final String  fxzhihang    = "fxzhihang";  //  开户行所在支行  否    收款人的开户地址所在支行
//   private static final String  fxsheng      = "fxsheng";    //  开户行所在省    否    收款人的开户地址所在省
//   private static final String  fxshi        = "fxshi";      //  开户行所在市    否    收款人的开户地址所在市
//   private static final String  fxlhh        = "fxlhh";      //  开户卡的联行号  否    开户卡对应银行联行号



    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {

                //组装参数

                Map<String,String> payParamFxbody  = new HashMap<>();
                      payParamFxbody.put(fxddh,channelWrapper.getAPI_ORDER_ID());
                      payParamFxbody.put(fxdate,DateUtil.formatDateTimeStrByParam("YYYYMMDDhhmmss"));
                      payParamFxbody.put(fxfee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                      payParamFxbody.put(fxbody,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                      payParamFxbody.put(fxname, channelWrapper.getAPI_CUSTOMER_NAME());
                      payParamFxbody.put(fxaddress, channelWrapper.getAPI_CUSTOMER_BANK_NAME());


                payParam.put(fxid,channelWrapper.getAPI_MEMBERID());
                payParam.put(fxaction,"repay");
                payParam.put(fxnotifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.put(fxbody,"["+JSON.toJSONString(payParamFxbody)+"]");




                //签名【md5(商务号+商户查询动作+订单信息域（json格式字符串）+商户秘钥)】
                String paramsStr = String.format("%s%s%s%s",
                    payParam.get(fxid),
                    payParam.get(fxaction),
                    payParam.get(fxbody),
                channelWrapper.getAPI_KEY());
                String pay_md5sign =  HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                //发送请求获取结果
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"UTF-8");
                String resultStr1 = UnicodeUtil.unicodeToString(resultStr); //  编码后不能解析中文
                        details.put(RESPONSEKEY, resultStr1);//强制必须保存下第三方结果
//                      addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询 (有回调无需自动查询)

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
       if(1==2) throw new PayException("[通付代付][代付][查询订单状态]该功能未完成。");
        try {
//            fxid    商务号    是    唯一号，由通付提供
//            fxaction    商户查询动作    是    商户查询动作，这里填写【repayquery】
//            fxbody    订单信息域    是    提交代付订单信息参考下方订单信息域（json格式字符串）
//            fxsign    签名【md5(商务号+商户查询动作+订单信息域（json格式字符串）+商户秘钥)】    是    通过签名算法计算得出的签名值。
//            fxbody订单信息域（二维数组）
            Map<String,String> payParamFxbody  = new HashMap<>();
                payParamFxbody.put(fxddh,channelWrapper.getAPI_ORDER_ID());
            //组装参数
            payParam.put(fxid,channelWrapper.getAPI_MEMBERID());
            payParam.put(fxaction,"repayquery");
            payParam.put(fxbody,"["+JSON.toJSONString(payParamFxbody)+"]");


            //签名【md5(商务号+商户查询动作+订单信息域（json格式字符串）+商户秘钥)】
            String paramsStr = String.format("%s%s%s%s",
                    payParam.get(fxid),
                    payParam.get(fxaction),
                    payParam.get(fxbody),
                    channelWrapper.getAPI_KEY());
            String pay_md5sign =  HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"UTF-8");
            String resultStr1 = UnicodeUtil.unicodeToString(resultStr);  //  编码后不能解析中文
            //发送请求获取结果
            details.put(RESPONSEKEY, resultStr1);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
//        参数    参数含义    必须    说明
//        fxid    商务号    是    唯一号，由通付提供
//        fxdate    查询时间    是    时间格式 YYYMMDDhhmmss
//        fxaction    商户查询动作    是    商户查询动作，这里填写【money】
//        fxsign    签名【md5(商务号+查询时间+商户查询动作+商户秘钥)】    是    通过签名算法计算得出的签名值。

        try {
            //组装参数
            payParam.put(fxid,channelWrapper.getAPI_MEMBERID());
            payParam.put(fxdate,DateUtil.formatDateTimeStrByParam("YYYMMDDhhmmss"));
            payParam.put(fxaction,"money");

            //签名【md5(商务号+查询时间+商户查询动作+商户秘钥)】
            String paramsStr = String.format("%s%s%s%s",
                    payParam.get(fxid),
                    payParam.get(fxdate),
                    payParam.get(fxaction),
                    channelWrapper.getAPI_KEY());
            String pay_md5sign =  HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"UTF-8");

            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            //余额查询状态【0失败】【1成功】
            if(HandlerUtil.valJsonObj(jsonObj,"fxstatus","1") && jsonObj.containsKey("fxmoney") ){
                String balance = jsonObj.getString("fxmoney");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[通付代付2][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[通付代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);

        String fxbodyStr = jsonObj.getString(fxbody);
        JSONArray fxbodyStrObj = JSON.parseArray(fxbodyStr);
        int size = fxbodyStrObj.size();
        if (size==1){
            String replace = fxbodyStr.replace("[", "").replace("]", "");
            jsonObj = JSON.parseObject(replace);
        }
        //代付申请状态【0申请失败】【1申请成功】
        if(!isQuery){
            if (HandlerUtil.valJsonObj(jsonObj, "fxstatus", "1")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "fxstatus", "0")) return PayEumeration.DAIFU_RESULT.ERROR;
            //if (HandlerUtil.valJsonObj(jsonObj, "respCode", "00")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            return PayEumeration.DAIFU_RESULT.ERROR;  //XP：除了返回99处理中，返回任何其他的，都代表失败，可以再次重复出款，而不会造成重复出款
        }else { // 【-1订单不存在】【0正常申请】【1已打款】【2冻结】【3已取消】
            if( resultStr.contains("找不到交易"))return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "fxstatus", "1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (HandlerUtil.valJsonObj(jsonObj, "fxstatus", "0")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "fxstatus", "-1","3","2")) return PayEumeration.DAIFU_RESULT.ERROR;
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            new PayException(resultStr);
        }
       return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}