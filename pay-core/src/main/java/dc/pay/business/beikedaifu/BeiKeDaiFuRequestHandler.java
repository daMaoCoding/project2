package dc.pay.business.beikedaifu;


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
 * Apr 11, 2019
 */
@RequestDaifuHandler("BEIKEDAIFU")
public final class BeiKeDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BeiKeDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
     private static final String  tenantNo      =  "tenantNo"; //  必填 商户ID
     private static final String  tenantOrderNo =  "tenantOrderNo"; //  必填 商户订单号
     private static final String  amount        =  "amount"; //  必填 金额(元)
     private static final String  name          =  "name"; // 必填 银行卡预留姓名
     private static final String  bankNo        =  "bankNo"; //  必填 银行卡号
     private static final String  bankName      =  "bankName"; //  银行名称
     private static final String  notifyUrl     =  "notifyUrl"; //  通知地址
     private static final String  remark        =  "remark"; //  备注
     private static final String  type          =  "type";//  必填  类型（0：支付订单查询1:代付订单查询）
//     private static final String  sign          =  "sign";//  必填  签名






    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {

                //组装参数
            payParam.put(tenantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(tenantOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(name, channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(bankNo, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(bankName,channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(remark,"remark");

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

                String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/channel/repay";
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
       if(1==2) throw new PayException("[贝壳代付][代付][查询订单状态]该功能未完成。");
        try {


            //组装参数
            payParam.put(tenantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(tenantOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(type,"1"); //类型（0：支付订单查询 1:代付订单查询）


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
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/channel/search";
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
        if(1==2) throw new PayException("[贝壳代付][代付余额查询]该功能未完成。");


        try {
            //组装参数
            payParam.put(tenantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(type,"D0");

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
            String pay_md5sign =  HandlerUtil.getMD5UpperCase(preStr.getBytes(GBK)).toUpperCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String url =channelWrapper.getAPI_CHANNEL_BANK_URL()+"/channel/balanceInquiry";
            String resultStr = RestTemplateUtil.postForm(url, payParam,"UTF-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);//请求状态(200：成功，500：系统异常，404：必填项为空)
            if(HandlerUtil.valJsonObj(jsonObj,"status","200") && jsonObj.containsKey("amount") ){
                String balance = jsonObj.getString("amount");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[贝壳代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[贝壳代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
//          请求状态(200：成功，404：必填项为空，405：尚未配置此类型的交易通道，408：商户订单号已存在，409：交易异常，410：余额不足，411：请联系渠道添加IP白名单)
        if(!isQuery){
            if (HandlerUtil.valJsonObj(jsonObj, "status", "200")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "404","405","408","409","410","411","444")) return PayEumeration.DAIFU_RESULT.ERROR;
            //if (HandlerUtil.valJsonObj(jsonObj, "respCode", "00")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            return PayEumeration.DAIFU_RESULT.ERROR;
        }else {
//          请求状态(200：支付成功，500：系统异常，201：支付中，403：未支付，405：通道异常，406：取消)
            if( resultStr.contains("找不到交易"))return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "200")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "201", "403", "405", "500"))
                return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "406", "444")) return PayEumeration.DAIFU_RESULT.ERROR;
            new PayException(resultStr);
        }
       return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}