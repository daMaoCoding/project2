package dc.pay.business.xianfutongdaifu;


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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Cobby
 * May 14, 2019
 */
@RequestDaifuHandler("XIANFUTONGDAIFU")
public final class XianFuTongDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XianFuTongDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  merchant_code      =  "merchant_code";    //商户编号    是  公司提供
     private static final String  nonce_str          =  "nonce_str";        //随机字符串  是
     private static final String  channel_no         =  "channel_no";       //通道编号    是  先付通(对私):XIANFENG   先付通(对公):XIANFENG_CONTRARY
     private static final String  accountNo          =  "accountNo";        //银行卡号    是
     private static final String  accountName        =  "accountName";      //持卡人姓名   是
     private static final String  amount             =  "amount";           //金额       是  已元为单位
     private static final String  bankNo             =  "bankNo";           //银行编码    是  填写大写的银行编号，如：ICBC  ABC…
     private static final String  desc               =  "desc";             //订单描述    是
     private static final String  merchant_order_no  =  "merchant_order_no";//商户订单号  是   唯一商户订单号
     private static final String  merchant_no        =  "merchant_no";      //商户订单号  是   唯一商户订单号
//     private static final String  auth_code          =  "auth_code";        //谷歌验证码  否   谷歌验证器生成的验证码    当验证方式为GOOGLE时必填
     private static final String  auth_type          =  "auth_type";        //验证方式    是   IP：IP验证    GOOGLE：谷歌验证码验证
     private static final String  notifyUrl          =  "notifyUrl";        //回调地址    否   http https 的链接
//    private static final String  type          =  "签名    sign    是    string    注释：1.1






    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
            String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
            if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
                log.error("[先付通代付]-[代付]-“代付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道编号( (对私):XIANFENG  (对公):XIANFENG_CONTRARY)" );
                throw new PayException("[先付通代付]-[代付]-“代付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道编号( (对私):XIANFENG  (对公):XIANFENG_CONTRARY)" );
            }
                //组装参数
            payParam.put(merchant_code,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(nonce_str,HandlerUtil.randomStr(8));
            payParam.put(channel_no,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(accountNo, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(accountName, channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(bankNo,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(desc,"desc");
            payParam.put(merchant_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(auth_type, "IP");
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());

               //生成md5
                StringBuilder sb = new StringBuilder((payParam.size() + 1) * 10);
                List<String> keys = new ArrayList<String>(payParam.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    sb.append(key).append("=");
                    sb.append(payParam.get(key));
                    sb.append("&");
                }
//                sb.setLength(sb.length() - 1);
                String preStr = sb.toString()+"key=" +channelWrapper.getAPI_KEY();
                String pay_md5sign =  HandlerUtil.getMD5UpperCase(preStr).toUpperCase();
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/paid/pay";
                //发送请求获取结果
                String resultStr = RestTemplateUtil.postForm(url, payParam,"UTF-8");
                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
//                addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询 (如有回调则无需 自动查询)

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
       if(1==2) throw new PayException("[先付通代付][代付][查询订单状态]该功能未完成。");
        try {


            //组装参数
            payParam.put(merchant_code,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(nonce_str,HandlerUtil.randomStr(8));
            payParam.put(channel_no,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(merchant_no,channelWrapper.getAPI_ORDER_ID());


            //生成md5
            StringBuilder sb = new StringBuilder((payParam.size() + 1) * 10);
            List<String> keys = new ArrayList<String>(payParam.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
//            sb.setLength(sb.length() - 1);
            String preStr = sb.toString()+"key=" +channelWrapper.getAPI_KEY();
            String pay_md5sign =  HandlerUtil.getMD5UpperCase(preStr).toUpperCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/paid/paidQuery";
            String resultStr = RestTemplateUtil.postForm(url, payParam,"UTF-8");

            //发送请求获取结果
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        throw new PayException("[先付通代付][代付余额查询]该功能未完成。");
    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
//          PROCESSING:处理中 COMPLETED:已完成 PAID_FAIL:代付失败
        if(!isQuery){
            if (HandlerUtil.valJsonObj(jsonObj, "orderStatus", "PROCESSING")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "orderStatus", "PAID_FAIL")) return PayEumeration.DAIFU_RESULT.ERROR;
            //if (HandlerUtil.valJsonObj(jsonObj, "orderStatus", "COMPLETED")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            return PayEumeration.DAIFU_RESULT.ERROR;
        }else {
//          PROCESSING:处理中 COMPLETED:已完成 PAID_FAIL:代付失败
            if( resultStr.contains("找不到交易"))return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "orderStatus", "COMPLETED")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (HandlerUtil.valJsonObj(jsonObj, "orderStatus", "PROCESSING")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "orderStatus", "PAID_FAIL")) return PayEumeration.DAIFU_RESULT.ERROR;
            new PayException(resultStr);
        }
       return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}