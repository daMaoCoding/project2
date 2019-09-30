package dc.pay.business.wuxingdaifu;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import net.dongliu.requests.RawResponse;


@RequestDaifuHandler("WUXINGDAIFU")
public final class WuXingDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WuXingDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String   pay = " pay";
     private static final String  amount = "amount";    // string 是 订单金额, 单位元，最小保留2为小数
     private static final String  notifyUrl = "notifyUrl";    // string 是 通知地址，不能超过512字节
     private static final String  orderId = "orderId";    // string 是 商户订单ID，不要重复，10-32位
     private static final String  remark = "remark";    // string 是 备注，银行转帐备注信息
     private static final String  toBankAccName = "toBankAccName";    // string 是 银行账户实名
     private static final String  toBankAccNumber = "toBankAccNumber";    // string 是 银行账户帐号
     private static final String  toBankCode = "toBankCode";    // string 是 银行编码，详情见支持银行编码小节
     private static final String  version = "version";    // string 是 版本号，默认值：1.00
     private static final String  signType = "signType";    // string 是 签名类型，hmacsha256
     private static final String  signTime = "signTime";    // string 是 签名时间, 格式yyyyMMddHHmmss
     private static final String  sign = "sign";    // string 是 签名
     private static final String  orderType = "orderType";  // string 是 订单类型: pay
     private static final String  balance = "balance";
     private static final String  frozenMoney = "frozenMoney";

     private static final String msg = "msg";
     private static final String failMsg = "签名错误,订单号重复,订单记录不存在,订单重复请求,签名时间不正确,ip验证不通过,余额不足";






    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
            if(!channelWrapper.getAPI_KEY().contains("&")){
                details.put(RESPONSEKEY, "密钥填写错误，格式：[秘钥]&[Token],如：ABCD&1234");
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
                //组装参数
                payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.put(orderId,channelWrapper.getAPI_ORDER_ID());
                payParam.put(remark,channelWrapper.getAPI_ORDER_ID());
                payParam.put(toBankAccName,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(toBankAccNumber,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(toBankCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(version, "1.00");
                payParam.put(signType, "hmacsha256");
                payParam.put(signTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

                //生成md5
                String pay_md5sign = null;
                try {
                    pay_md5sign = WuXingDaiFuUtil.sign(payParam,channelWrapper.getAPI_KEY().split("&")[0]);
                }catch (Exception e){
                    details.put(RESPONSEKEY, e.getMessage());
                    return PayEumeration.DAIFU_RESULT.ERROR;
                }
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


               RawResponse ret = WuXingDaiFuUtil.post(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0],payParam,channelWrapper.getAPI_KEY().split("&")[1]).send();
                if(null!=ret ){
                    if (ret.getStatusCode() != 200) {
                        ErrorResp resp = ret.readToJson(ErrorResp.class);
                        details.put(RESPONSEKEY, JSON.toJSONString(resp, false));//强制必须保存下第三方结果
                        return PayEumeration.DAIFU_RESULT.ERROR;
                    } else {
                        details.put(RESPONSEKEY,ret.readToText());//强制必须保存下第三方结果
                        return PayEumeration.DAIFU_RESULT.PAYING;
                    }
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
       if(1==2) throw new PayException("[五星代付][代付][查询订单状态]该功能未完成。");
        try {
            if(!channelWrapper.getAPI_KEY().contains("&")){
                details.put(RESPONSEKEY, "密钥填写错误，格式：[秘钥]&[Token],如：ABCD&1234");
                return PayEumeration.DAIFU_RESULT.UNKNOW;
            }

            //组装参数
            payParam.put(orderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderType, pay);
            payParam.put(signType, "hmacsha256");
            payParam.put(signTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

            //生成md5
            String pay_md5sign = null;
            try {
                pay_md5sign = WuXingDaiFuUtil.sign(payParam,channelWrapper.getAPI_KEY().split("&")[0]);
            }catch (Exception e){
                details.put(RESPONSEKEY, e.getMessage());
                return PayEumeration.DAIFU_RESULT.ERROR;
            }
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            RawResponse ret = WuXingDaiFuUtil.post(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1],payParam,channelWrapper.getAPI_KEY().split("&")[1]).send();
            if(null!=ret ){
                String  resultStr = ret.readToText();
                details.put(RESPONSEKEY,resultStr);//强制必须保存下第三方结果
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[五星代付][代付余额查询]该功能未完成。");
        try {
            //组装参数-无
            payParam.put(signTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));

            //生成md5
            String pay_md5sign = null;
            try {
                pay_md5sign = WuXingDaiFuUtil.sign(payParam,channelWrapper.getAPI_KEY().split("&")[0]);
            }catch (Exception e){
                details.put(RESPONSEKEY, e.getMessage());
                throw new PayException(e.getMessage());
            }
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            RawResponse ret = WuXingDaiFuUtil.post(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2],payParam,channelWrapper.getAPI_KEY().split("&")[1]).send();
            if(null!=ret){
                String  resultStr = ret.readToText();
                if( ret.getStatusCode() != 200){throw new PayException(resultStr);}
                JSONObject resultJson = JSON.parseObject(resultStr);
                details.put(RESPONSEKEY,resultStr);//强制必须保存下第三方结果
                String balanceStr = HandlerUtil.getFen(resultJson.getString(balance));
                String frozenMoneyStr = HandlerUtil.getFen(resultJson.getString(frozenMoney));
                return HandlerUtil.addStringNumber(balanceStr,frozenMoneyStr);
            }else{ throw new PayException(EMPTYRESPONSE);}
        } catch (Exception e){
            log.error("[五星代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[五星代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }
    }



    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        if(null==jsonObj) throw new PayException("查询结果空，请稍后再试或联系第三方。");
        if(isQuery && resultStr.contains("订单号不存在")) return PayEumeration.DAIFU_RESULT.ERROR;
//        第三方会返回异常：{"code":1005,"msg":"订单重复请求"}
        if(HandlerUtil.valJsonObj(jsonObj,"code","1005")) return PayEumeration.DAIFU_RESULT.PAYING;
        if(HandlerUtil.valJsonObj(jsonObj,"status","S")) return PayEumeration.DAIFU_RESULT.SUCCESS;
        if(HandlerUtil.valJsonObj(jsonObj,"status","F")) return PayEumeration.DAIFU_RESULT.ERROR;
        if(HandlerUtil.valJsonObj(jsonObj,"status","P")) return PayEumeration.DAIFU_RESULT.PAYING;
        //if(jsonObj.containsKey(msg) && failMsg.contains(jsonObj.getString(msg)))  return PayEumeration.DAIFU_RESULT.ERROR;
        throw new  PayException(resultStr);
    }


}