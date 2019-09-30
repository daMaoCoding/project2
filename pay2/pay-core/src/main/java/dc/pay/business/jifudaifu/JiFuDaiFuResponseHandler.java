package dc.pay.business.jifudaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

@Slf4j
@ResponseDaifuHandler("JIFUDAIFU")
public final class JiFuDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("00000");

    private static final String  cipher="cipher";    // "i7Rgn49ZmNG3JjQ5fpYOnnOpX86o6YR+QSxtKrcQw6QM7BgRtTlNp0FCnU50Lmc0LR+VG58qzFgwWGwoH16zX35XNr6iZ3IPLRKJ/iA4G4KvX2r5OQL5CelIHXeK/+0N2+H26EixY77T8sLNkiI9aR5Qzzmg9lPS6lpegQeilYAce2hP9DDOwqv4/PrEUEciLLDxLSwK+TIxWUhZHl5ffAzdBYnRpXZdpEL07S46Mmcz4YU3UP5WLj5Vf/GP398B0PCP5YEUznV6PH5mHYeATE3JgQKNT50+NgYh/ELks8HbT66nDyFp466Z1PgJIPRuODWGBQBPU7XArh3RtlUWGw==",
    private static final String  msg="msg";    // "受理成功",
    private static final String  sign="sign";    // "cmrgEdNLkGpzmynSDpKQnLaOyYVbMPPdquD3zCOrhJbBiv1XTdwrxxpZ1KzeVkppY78bsIe48tenFg5nTIV4lSG7DM4HXs6Rtk6MEESoZeOlDfYO08lUc/8vWNcVtSOCKEqj313gQwTd/sYtWlOXgdzLYiexpD/r/hZmslIH4/wAqEecmHaw6XMYrmVlcUqdh6J2xcHqo63LML8bf6IR7ku41RAhN8n3+C1JOOlg5XHLjWt7d1DV455vxIQofeg4pi3gruU0mktfck+++tp5CVGRZTruArCrzSz4X+9vt2ET+xcnsRCS/Qi9/kBY3923PtIw68TyDnb141v5pgUGug==",
    private static final String  code="code";    // "00000",
    private static final String  merchantId="merchantId";    // "100000006"

     private static final String  reqNo ="reqNo"; // "20190226145649018132",
     private static final String  transNo ="transNo"; // "10000000011185732",
     private static final String  transAmt ="transAmt"; // 100,
     private static final String  status ="status"; // 1,
     private static final String  transTime ="transTime"; // "20190226145651"






    //获取订单号，不可空。
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String memberIdRes = API_RESPONSE_PARAMS.get(merchantId);
        String cipherRes   = API_RESPONSE_PARAMS.get(cipher);
        if(StringUtils.isBlank(memberIdRes) || StringUtils.isBlank(cipherRes)) throw new PayException("回调数据获取商户号/加密数据，错误。");
        String apiKeyFromMemberId = handlerUtil.getApiKeyFromReqDaifuMemberId(memberIdRes);
        if(StringUtils.isBlank(apiKeyFromMemberId)) throw new PayException("获取密钥错误");
        String bizContent="";
        try {
            bizContent = JiFuPayUtils.decrypt(API_RESPONSE_PARAMS.get("cipher"),apiKeyFromMemberId);
        } catch (Exception e) {
            throw new PayException("解密回调数据获取订单号错误，请检查私钥。");
        }
        if(StringUtils.isBlank(bizContent)) throw new PayException("解密回调数据获取订单号错误，解密结果空。");

        String ordernumberR = JSON.parseObject(bizContent).getString(reqNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[即付代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    //计算签名
    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String signRes = payParam.get(sign);
        payParam.remove(sign);
        try {
            if (signRes != null && JiFuPayUtils.verify(payParam, signRes, channelWrapper.getAPI_PUBLIC_KEY())) {
                return TRUE;
            }
            return FALSE;
        } catch (Exception e) {
            throw new PayException("解密回调数据出错，请核对公钥是否是第三方平台的公钥。");
        }
    }

    //验证签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = TRUE.equalsIgnoreCase(signMd5);
        log.debug("4.[即付代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //验证金额
   @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        String bizContent="";
        try {
            bizContent = JiFuPayUtils.decrypt(api_response_params.get(cipher),channelWrapper.getAPI_KEY());
        } catch (Exception e) {
            throw new PayException("解密回调数据获取订单号错误，请检查私钥。");
        }
        JSONObject resBizObj = JSON.parseObject(bizContent);
        boolean checkResult = false;
        String responseAmount =  resBizObj.getString(transAmt);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[即付代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }


    //验证订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        String bizContent="";
        try {
            bizContent = JiFuPayUtils.decrypt(api_response_params.get(cipher),channelWrapper.getAPI_KEY());
        } catch (Exception e) {
            throw new PayException("解密回调数据获取订单号错误，请检查私钥。");
        }
        JSONObject resBizObj = JSON.parseObject(bizContent);

        //0-处理中 1-支付成功 2-支付失败，己退汇
        if(resBizObj.containsKey(status)){
            if( "2".equalsIgnoreCase(resBizObj.getString(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
            if( "0".equalsIgnoreCase(resBizObj.getString(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
            if( "1".equalsIgnoreCase(resBizObj.getString(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[即付代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }



    //响应第三方内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[即付代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }




}