package dc.pay.business.pinganzhifu;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.xintiantianzhifu.Encrypter;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mikey
 * Jun 24, 2019
 */
@RequestDaifuHandler("PINGANDAIFU")
public final class PingAnDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PingAnDaiFuRequestHandler.class);


    private static final String merchantCode = "merchantCode";        //商户编号            
    private static final String orderNumber = "orderNumber";        //订单号
    private static final String tranCode    = "tranCode";            //接口类型
    private static final String totalAmount = "totalAmount";        //订单金额
    private static final String accountNo   = "accountNo";            //收款卡号
    private static final String accountName = "accountName";        //收款户名
    private static final String bankName    = "bankName";            //收款方银行名称
    private static final String bankCode    = "bankCode";            //银行代码
    private static final String callback    = "callback";            //异步通知地址

    private static final String oriOrderNumber = "oriOrderNumber";    //订单号


    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
            //组装参数
            payParam.put(merchantCode, channelWrapper.getAPI_MEMBERID());
            payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
            payParam.put(tranCode, "100");//tranCode    100    代付交易
            payParam.put(totalAmount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(accountNo, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(accountName, channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(callback, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(bankName, channelWrapper.getAPI_CHANNEL_BANK_NAME());
            payParam.put(bankCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            
            //组成密文
            /*
                1 生成16位随机字符串 为AES秘钥  
                2 base64编码 AES密钥加密请求报文 （表格参数） 放入 Context字段
                3 RSA系统公钥加密AES密钥 放入 encrtpKey字段  
                4 RSA商户私钥签名请求报文  放入 signData 字段
                5 merchantCode 代理商的ID
            */
            Map<String, String> headParam = new HashMap<String, String>();
            //生成16位随机字符串 为AES秘钥  
            String aesKey = handlerUtil.getRandomStr(16);
            //base64编码 AES密钥加密请求报文 （表格参数） 放入 Context字段
            String encryptData = new AES128ECB().Encrypt(JSON.toJSONString(payParam), aesKey);            
            headParam.put("Context", encryptData);
            //RSA系统公钥加密AES密钥 放入 encrtpKey字段  
            String encrtptKey = RsaUtil.encryptToBase64(aesKey, channelWrapper.getAPI_PUBLIC_KEY());    
            headParam.put("encrtpKey", encrtptKey);
            //RSA商户私钥签名请求报文  放入 signData 字段
            String signMd5 = RsaUtil.signByPrivateKey(JSON.toJSONString(payParam),channelWrapper.getAPI_KEY(),"SHA1withRSA");    // 签名
            headParam.put("signData", signMd5);
            //merchantCode 代理商的ID
            headParam.put("merchantCode", channelWrapper.getAPI_MEMBERID());
//            addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询  请求有可能 返回空 先添加定时任务
            String url = channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0];
            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplate(url, headParam, String.class, HttpMethod.POST);
            resultStr = this.decryptJsonObject(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果


            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,false);
            }else{ throw new PayException(EMPTYRESPONSE);}

            // 结束

        } catch (Exception e) {
            log.error("[平安代付][请求代付]出错,错误消息：{},参数：{}", e.getMessage(), JSON.toJSONString(payParam), e);
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
        try {
            /*
            变量名                域名        类型            必填        说明
            oriOrderNumber    订单号        String(32)    是        标识唯一订单 
            tranCode        接口类型    String(6)    是        调用接口类型：101
            merchantCode    商户号        STRING(15)    是        商户号
            */

            //组装参数
            payParam.put(oriOrderNumber, channelWrapper.getAPI_ORDER_ID());
            payParam.put(tranCode, "101");    //调用接口类型：101
            payParam.put(merchantCode, channelWrapper.getAPI_MEMBERID());

            //组成密文
            /*
                1 生成16位随机字符串 为AES秘钥  
                2 base64编码 AES密钥加密请求报文 （表格参数） 放入 Context字段
                3 RSA系统公钥加密AES密钥 放入 encrtpKey字段  
                4 RSA商户私钥签名请求报文  放入 signData 字段
                5 merchantCode 代理商的ID
            */
            Map<String, String> headParam = new HashMap<String, String>();
            //生成16位随机字符串 为AES秘钥  
            String aesKey = handlerUtil.getRandomStr(16);
            //base64编码 AES密钥加密请求报文 （表格参数） 放入 Context字段
            String encryptData = new AES128ECB().Encrypt(JSON.toJSONString(payParam), aesKey);            
            headParam.put("Context", encryptData);
            //RSA系统公钥加密AES密钥 放入 encrtpKey字段  
            String encrtptKey = RsaUtil.encryptToBase64(aesKey, channelWrapper.getAPI_PUBLIC_KEY());    
            headParam.put("encrtpKey", encrtptKey);
            //RSA商户私钥签名请求报文  放入 signData 字段
            String signMd5 = RsaUtil.signByPrivateKey(JSON.toJSONString(payParam),channelWrapper.getAPI_KEY(),"SHA1withRSA");    // 签名
            headParam.put("signData", signMd5);
            //merchantCode 代理商的ID
            headParam.put("merchantCode", channelWrapper.getAPI_MEMBERID());
            String url = channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0];
            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplate(url, headParam, String.class, HttpMethod.POST);
            resultStr = this.decryptJsonObject(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){
            log.error("[平安代付][查询代付]出错,错误消息：{},参数：{}", e.getMessage(), JSON.toJSONString(payParam), e);
            throw new PayException(e.getMessage());
        }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
            /*
             merchantCode    商户编号
             * */
            //组装参数
            payParam.put(merchantCode,channelWrapper.getAPI_MEMBERID());


            //组成密文
            /*
                1 生成16位随机字符串 为AES秘钥  
                2 base64编码 AES密钥加密请求报文 （表格参数） 放入 Context字段
                3 RSA系统公钥加密AES密钥 放入 encrtpKey字段  
                4 RSA商户私钥签名请求报文  放入 signData 字段
                5 merchantCode 代理商的ID
            */
            Map<String, String> headParam = new HashMap<String, String>();
            //生成16位随机字符串 为AES秘钥  
            String aesKey = handlerUtil.getRandomStr(16);
            //base64编码 AES密钥加密请求报文 （表格参数） 放入 Context字段
            String encryptData = new AES128ECB().Encrypt(JSON.toJSONString(payParam), aesKey);            
            headParam.put("Context", encryptData);
            //RSA系统公钥加密AES密钥 放入 encrtpKey字段

            String encrtptKey = RsaUtil.encryptToBase64(aesKey, channelWrapper.getAPI_PUBLIC_KEY());
            headParam.put("encrtpKey", encrtptKey);
            //RSA商户私钥签名请求报文  放入 signData 字段
            String signMd5 = RsaUtil.signByPrivateKey(JSON.toJSONString(payParam),channelWrapper.getAPI_KEY(),"SHA1withRSA");    // 签名
            headParam.put("signData", signMd5);
            //merchantCode 代理商的ID
            headParam.put("merchantCode", channelWrapper.getAPI_MEMBERID());

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], headParam, "UTF-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            //解密String to Object
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"respType","S") ){
                String balance = HandlerUtil.getFromJsonObject(jsonObj,"balance");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[平安代付][代付余额查询]出错,错误消息：{},参数：{}", e.getMessage(), JSON.toJSONString(payParam), e);
            throw new PayException(e.getMessage());
        }
    }

    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {

        JSONObject jsonObject = JSON.parseObject(resultStr);
        if(!isQuery){
            /*
                S：成功
                E：失败
                R：不确定（处理中）
            */
            if(HandlerUtil.valJsonObj(jsonObject,"respType","S","R")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObj(jsonObject,"respType","E")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }else{
            /*
                S：成功
                E：失败
                R：不确定（处理中）
             */
            if (resultStr.contains("无此订单")) return PayEumeration.DAIFU_RESULT.ERROR;
            if(HandlerUtil.valJsonObj(jsonObject,"respType","S")) {
                if(HandlerUtil.valJsonObj(jsonObject,"oriRespType","S"))  return PayEumeration.DAIFU_RESULT.SUCCESS;
                if(HandlerUtil.valJsonObj(jsonObject,"oriRespType","R"))  return PayEumeration.DAIFU_RESULT.PAYING;
                if(HandlerUtil.valJsonObj(jsonObject,"oriRespType","E"))  return PayEumeration.DAIFU_RESULT.ERROR;
                return PayEumeration.DAIFU_RESULT.PAYING;
            }
            if(HandlerUtil.valJsonObj(jsonObject,"respType","R")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObj(jsonObject,"respType","E")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new PayException(resultStr);
        }
    }


    private String decryptJsonObject(String resultStr) throws PayException {
        //解密
        String jsonObjectStr;
        try {
            JSONObject jsonObject           = JSONObject.parseObject(resultStr);
            Encrypter  encrypter            = new Encrypter(channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_KEY());
            byte[]     decodeBase64KeyBytes = Base64.decodeBase64(jsonObject.get("encrtpKey").toString().getBytes("utf-8"));
            byte[]     merchantAESKeyBytes  = encrypter.RSADecrypt(decodeBase64KeyBytes);
            // 使用base64解码商户请求报文
            byte[] decodeBase64DataBytes = Base64.decodeBase64(jsonObject.get("Context").toString().getBytes("utf-8"));
            byte[] realText = encrypter.AESDecrypt(decodeBase64DataBytes, merchantAESKeyBytes);
            jsonObjectStr = new String(realText, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[平安代付][解密]出错,错误消息：{}", e.getMessage());
            throw new PayException(resultStr);
        }
        return jsonObjectStr;
    }

}