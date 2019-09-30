package dc.pay.business.jinyangdaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@ResponseDaifuHandler("JINYANGDAIFU")
public final class JinYangDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("ok");

    private static final String   amount="amount";   // "SpwGXhCjoPA=",
    private static final String   orderno="orderno";   // "20190301165404844633",
    private static final String   sign="sign";   // "155a32daaf5e17d17fa5f3fd67d16af7",
    private static final String   mchtid="mchtid";   // "32805",
    private static final String   state="state";   // "2"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(orderno);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[金阳代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5
        String pay_md5sign = null;
        if(!channelWrapper.getAPI_KEY().contains("&")){
            throw new PayException("密钥填写错误，格式：[代付3DESKEY]&[代付MD5KEY],如：ABCD&1234");
        }
        String md5key = channelWrapper.getAPI_KEY().split("&")[1];
        Map<String, String> resParamMap = new TreeMap<String, String>();
        resParamMap.put(mchtid, payParam.get(mchtid));
        resParamMap.put(orderno, payParam.get(orderno));
        resParamMap.put(state, payParam.get(state));
        resParamMap.put(amount, payParam.get(amount));
        String reqParamJsonStr = JinYangDaiFuUtil.mapToParamByKeysSort(resParamMap)+"&key="+md5key;
        pay_md5sign = JinYangDaiFuUtil.MD5(reqParamJsonStr, "UTF-8").toLowerCase();// 32位
        log.debug("2.[金阳代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        if(!channelWrapper.getAPI_KEY().contains("&")){
            throw new PayException("密钥填写错误，格式：[代付3DESKEY]&[代付MD5KEY],如：ABCD&1234");
        }
        String deskey = channelWrapper.getAPI_KEY().split("&")[0];

        boolean checkResult = false;
        String responseAmount = "0";
        try {
            responseAmount = HandlerUtil.getFen(JinYangDaiFuUtil.Decrypt3DES(api_response_params.get(amount), deskey));
        } catch (Exception e) {
           throw new PayException("请检查deskey,解密回调金额出错。");
        }
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[金阳代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[金阳代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[金阳代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(api_response_params));
        if(jsonObject.containsKey(state)){
            if(HandlerUtil.valJsonObj(jsonObject,state,"0","1","100"))  orderStatus= PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObj(jsonObject,state,"3","120"))  orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
            if(HandlerUtil.valJsonObj(jsonObject,state,"2"))  orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[金阳代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}