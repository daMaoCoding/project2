package dc.pay.business.bsdaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.beetl.ext.fn.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequestDaifuHandler("BSDAIFU")
public final class BSDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BSDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
    private static final String  	payKey = "payKey";   //  支付Key
    private static final String  	cardNo = "cardNo";   //  收款人账户号
    private static final String  	cardName = "cardName";   //  收款人账户名
    private static final String  	noticeUrl = "noticeUrl";   //  代付结果通知地址
    private static final String  	orderNo = "orderNo";   //  订单号
    private static final String  	tranTime = "tranTime";   //  交易时间
    private static final String  	tranAmt = "tranAmt";   //  金额
     private static final String  	 merId = "merId";   //合作商户ID
     private static final String  	 encryptData = "encryptData";  //加密后的请求/应答报文
     private static final String  	 signData = "signData";  //签名





    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
                if(!channelWrapper.getAPI_KEY().contains("&") ||channelWrapper.getAPI_KEY().split("&").length!=3 ){
                    details.put(RESPONSEKEY, "密钥格式错误：请填写，支付key&签名密钥&商户RSA私钥，如：ABC&123&MIT****");
                    return PayEumeration.DAIFU_RESULT.ERROR;
                }

                String payKeyV = channelWrapper.getAPI_KEY().split("&")[0];
                String secretV = channelWrapper.getAPI_KEY().split("&")[1];
                String pubKeyV = channelWrapper.getAPI_PUBLIC_KEY();

                //组装参数
                payParam.put(payKey,payKeyV);
                payParam.put(cardNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
                payParam.put(cardName,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(noticeUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL().concat("?order=").concat(channelWrapper.getAPI_ORDER_ID()));
                payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
                payParam.put(tranTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                payParam.put(tranAmt,channelWrapper.getAPI_AMOUNT());

                //生成md5
                String sign = SignUtils.getSign( payParam  , secretV);
                String bt_cipher = SecurityUtils.encrypt(JSON.toJSONString(payParam), pubKeyV);
                HashMap<String, String> postPayParam = Maps.newHashMap();
                postPayParam.put(merId,MD5Util.encode(channelWrapper.getAPI_MEMBERID()));
                postPayParam.put(signData,sign);
                postPayParam.put(encryptData,bt_cipher);


               //发送请求获取结果
                String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], postPayParam);
                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
                if(StringUtils.isNotBlank(resultStr)){
                    return getDaifuResult(details,resultStr,false);
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
       if(1==2) throw new PayException("[BS代付][代付][查询订单状态]该功能未完成。");
        try {
            if(!channelWrapper.getAPI_KEY().contains("&") ||channelWrapper.getAPI_KEY().split("&").length!=3 ){
                details.put(RESPONSEKEY, "密钥格式错误：请填写，支付key&签名密钥&商户RSA私钥，如：ABC&123&MIT****");
                throw new PayException("密钥格式错误：请填写，支付key&签名密钥&商户RSA私钥，如：ABC&123&MIT****");
            }
            String payKeyV = channelWrapper.getAPI_KEY().split("&")[0];
            String secretV = channelWrapper.getAPI_KEY().split("&")[1];
            String prvKeyV = channelWrapper.getAPI_KEY().split("&")[2];
            String pubKeyV = channelWrapper.getAPI_PUBLIC_KEY();

            //组装参数
            payParam.put(payKey,payKeyV);
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());

            //生成md5
            String sign = SignUtils.getSign( payParam  , secretV);
            String bt_cipher = SecurityUtils.encrypt(JSON.toJSONString(payParam), pubKeyV);
            HashMap<String, String> postPayParam = Maps.newHashMap();
            postPayParam.put(merId,MD5Util.encode(channelWrapper.getAPI_MEMBERID()));
            postPayParam.put(signData,sign);
            postPayParam.put(encryptData,bt_cipher);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], postPayParam);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(details,resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[BS代付][代付余额查询]该功能未完成。");
        try {
            if(!channelWrapper.getAPI_KEY().contains("&") ||channelWrapper.getAPI_KEY().split("&").length!=3 ){
                details.put(RESPONSEKEY, "密钥格式错误：请填写，支付key&签名密钥&商户RSA私钥，如：ABC&123&MIT****");
                throw new PayException("密钥格式错误：请填写，支付key&签名密钥&商户RSA私钥，如：ABC&123&MIT****");
            }
            String payKeyV = channelWrapper.getAPI_KEY().split("&")[0];
            String secretV = channelWrapper.getAPI_KEY().split("&")[1];
            String prvKeyV = channelWrapper.getAPI_KEY().split("&")[2];
            String pubKeyV = channelWrapper.getAPI_PUBLIC_KEY();

            //组装参数
            payParam.put(payKey,payKeyV);

            //生成md5
            String sign = SignUtils.getSign( payParam  , secretV);
            String bt_cipher = SecurityUtils.encrypt(JSON.toJSONString(payParam), pubKeyV);
            HashMap<String, String> postPayParam = Maps.newHashMap();
            postPayParam.put(merId,MD5Util.encode(channelWrapper.getAPI_MEMBERID()));
            postPayParam.put(signData,sign);
            postPayParam.put(encryptData,bt_cipher);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], postPayParam);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            try {
                resultStr = SecurityUtils.decrypt(resultStr, prvKeyV);
            } catch (Exception e1) {
                throw new PayException("查询失败，第三方返回: "+resultStr);
            }
            if(StringUtils.isNotBlank(resultStr))   details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") &&  HandlerUtil.valJsonObj(jsonObj,"resultFlag","0") ){
                return HandlerUtil.addStringNumber(jsonObj.getString("balance"),jsonObj.getString("unBalance"));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[BS代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[BS代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }




    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(Map<String,String> details,String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = new JSONObject();
        String decrypt =resultStr;
        try{
            jsonObj = JSON.parseObject(decrypt);
        }catch (Exception e){
            try {
                decrypt = SecurityUtils.decrypt(resultStr, channelWrapper.getAPI_KEY().split("&")[2]);
                jsonObj =JSON.parseObject(decrypt);
            } catch (Exception e1) {
                throw new PayException("请检查后台配置的商户RSA私钥是否正确。"+decrypt);
            }
        }
        if(StringUtils.isNotBlank(decrypt))details.put(RESPONSEKEY,decrypt);
        if(jsonObj==null) return PayEumeration.DAIFU_RESULT.UNKNOW;

        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") && HandlerUtil.valJsonObj(jsonObj,"resultFlag","0")) return PayEumeration.DAIFU_RESULT.SUCCESS;
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") && HandlerUtil.valJsonObj(jsonObj,"resultFlag","1")) return PayEumeration.DAIFU_RESULT.ERROR;
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") && HandlerUtil.valJsonObj(jsonObj,"resultFlag","2")) return PayEumeration.DAIFU_RESULT.PAYING;
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") && HandlerUtil.valJsonObj(jsonObj,"resultFlag","3")) return PayEumeration.DAIFU_RESULT.ERROR;
        if(isQuery){
            throw new PayException(decrypt);        //查询时候第三方未明确指定，订单状态失败，我们未知,抛异常返回unKnow。
        }
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0001","0002","0003","0004","0005")) return PayEumeration.DAIFU_RESULT.UNKNOW;
        return PayEumeration.DAIFU_RESULT.ERROR;
    }








}