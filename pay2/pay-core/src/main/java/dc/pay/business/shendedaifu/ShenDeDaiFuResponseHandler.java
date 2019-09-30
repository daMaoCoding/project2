package dc.pay.business.shendedaifu;


import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.bsdaifu.SecurityUtils;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author sunny
 * 06 21, 2019
 */
@Slf4j
@ResponseDaifuHandler("SHENDEDAIFU")
public final class ShenDeDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


    private static final String  success   				="success";//   商户订单号	String	是	商户订单号
    private static final String  respCode            	="respCode";         //   商户编号	String	是	
    private static final String  respMsg            	="respMsg";         //   参数列表	String	是	使用RSA加密
    private static final String  merchantId            	="merchantId";         //   参数列表	String	是	使用RSA加密
    private static final String  merOrderId            	="merOrderId";         //   参数列表	String	是	使用RSA加密
    private static final String  txnAmt            		="txnAmt";         //   参数列表	String	是	使用RSA加密
    private static final String  signMethod             ="signMethod";         //   参数列表	String	是	使用RSA加密
    private static final String  signature              ="signature";         //   参数列表	String	是	使用RSA加密


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(merOrderId);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[申德代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
    	String signMd5="";
    	try {
    		List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            paramKeys.remove(signMethod);
            paramKeys.remove(signature);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            //最后一个&转换成#
            signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
            signSrc.append(channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            signMd5 = java.util.Base64.getEncoder().encodeToString(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
        log.debug("2.[申德代付]-[代付回调]-生成md5签名：{}",signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
    	boolean checkResult = false;
        String responseAmount = api_response_params.get(txnAmt);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[申德代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("4.[申德代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[申德代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
    	PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
		JSONObject dataJSON = JSONObject.parseObject(HandlerUtil.mapToJson(api_response_params));
        if(dataJSON.containsKey("respCode")){
           if( "1002".indexOf(dataJSON.get("respCode").toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "2007".indexOf(dataJSON.get("respCode").toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "2003".indexOf(dataJSON.get("respCode").toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "1111".indexOf(dataJSON.get("respCode").toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "1001".indexOf(dataJSON.get("respCode").toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[申德代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }

}