package dc.pay.business.pinganzhifu;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.xintiantianzhifu.Encrypter;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * @author Mikey
 * Jun 24, 2019
 */
@Slf4j
@ResponseDaifuHandler("PINGANDAIFU")
public final class PingAnDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    private static final String respType    = "respType";     //    应答类型      String(2)    是    S：成功    E：失败    R：不确定（处理中）
    private static final String totalAmount = "totalAmount";  //    订单金额      STRING(12)    否    订单金额
//  private static final String respMsg     = "respMsg";      //    应答信息      String(128)    是
//  private static final String respCode    = "respCode";     //    应答码        String(6)    是    内部返回码
//  private static final String asyncTime   = "asyncTime";    //    响应时间       STRING(14)    否    YYYY-MM-DD HH:mm:ss
//  private static final String orderNumber = "orderNumber";  //    商户订单号     STRING(32)    否    商户订单号
//  private static final String oriRespCode = "oriRespCode";  //    原交易应答码   STRING(6)    否    内部返回码
//  private static final String oriRespMsg  = "oriRespMsg";   //    原交易应答描述  STRING(128)    否    对应应答码信息描述，包含中文
//  private static final String oriRespType = "oriRespType";  //    原交易应答类型  STRING(2)    否    S：成功    E：失败    R：不确定（处理中）


    //获取订单号，不可空。
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String memberIdRes = API_RESPONSE_PARAMS.get("merchantCode");
        if (StringUtils.isBlank(memberIdRes)) throw new PayException("回调数据获取商户号/加密数据，错误。");

        String apiKey    = handlerUtil.getApiKeyFromReqDaifuMemberId(memberIdRes);
        String publicKey = handlerUtil.getApiPublicKeyFromReqDaifuMemberId(memberIdRes);
        if (StringUtils.isBlank(apiKey)) throw new PayException("获取密钥错误");
        String jsonObjectStr = getdecode(JSON.toJSONString(API_RESPONSE_PARAMS), publicKey, apiKey);
        String ordernumberR  = JSON.parseObject(jsonObjectStr).getString("orderNumber");
        if (StringUtils.isBlank(ordernumberR)) throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[平安代付]-[代付回调]-获取回调订单号：{}", ordernumberR);
        return ordernumberR;
    }

    //计算签名
    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String jsonObjectStr = getdecode(JSON.toJSONString(payParam), channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_KEY());
        String ordernumberR  = JSON.parseObject(jsonObjectStr).getString("orderNumber");
        if (StringUtils.isNotBlank(ordernumberR)) {
                return TRUE;
            }
            return FALSE;
    }

    //验证签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = TRUE.equalsIgnoreCase(signMd5);
        log.debug("4.[平安代付]-[代付回调]-验证第三方签名：{}", result);
        return result;
    }


    //验证金额
    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        String     jsonObjectStr  = getdecode(JSON.toJSONString(api_response_params), channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_KEY());
        JSONObject jsonObject     = JSON.parseObject(jsonObjectStr);
        boolean    checkResult    = false;
        String     responseAmount = HandlerUtil.getFen(jsonObject.getString(totalAmount));

        boolean    checkAmount    = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount) checkResult = true;
        log.debug("3.[平安代付]-[代付回调]-验证回调金额：{}", checkResult);
        return checkResult;
    }


    //验证订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {

        String                     jsonObjectStr = getdecode(JSON.toJSONString(api_response_params), channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_KEY());
        PayEumeration.DAIFU_RESULT orderStatus   = PayEumeration.DAIFU_RESULT.UNKNOW;
        JSONObject                 jsonObject    = JSON.parseObject(jsonObjectStr);
//        1.    respType    应答类型    String(2)    是    S：成功
//        E：失败
//        R：不确定（处理中）
        if (jsonObject.containsKey(respType)) {
            if ("E".equalsIgnoreCase(jsonObject.getString(respType))) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
            if ("R".equalsIgnoreCase(jsonObject.getString(respType))) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
            if ("S".equalsIgnoreCase(jsonObject.getString(respType)))
                orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[平安代付]-[代付回调]-订单状态：{}", orderStatus);
        return orderStatus;
    }


    //响应第三方内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[平安代付]-[代付回调]-响应第三方内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }


    private String getdecode(String resultStr, String API_PUBLIC_KEY, String API_KEY) throws PayException {
        //解密
        String jsonObjectStr;
        try {
            JSONObject jsonObject           = JSONObject.parseObject(resultStr);
            Encrypter  encrypter            = new Encrypter(API_PUBLIC_KEY, API_KEY);
            byte[]     decodeBase64KeyBytes = Base64.decodeBase64(jsonObject.get("encrtpKey").toString().getBytes("utf-8"));
            byte[]     merchantAESKeyBytes  = encrypter.RSADecrypt(decodeBase64KeyBytes);
            // 使用base64解码商户请求报文
            byte[] decodeBase64DataBytes = Base64.decodeBase64(jsonObject.get("Context").toString().getBytes("utf-8"));
            byte[] realText              = encrypter.AESDecrypt(decodeBase64DataBytes, merchantAESKeyBytes);
            jsonObjectStr = new String(realText, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[平安代付][解密]出错,错误消息：{}", e.getMessage());
            throw new PayException(resultStr);
        }
        return jsonObjectStr;
    }


}