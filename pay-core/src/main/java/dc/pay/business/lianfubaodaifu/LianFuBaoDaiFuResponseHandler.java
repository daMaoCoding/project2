package dc.pay.business.lianfubaodaifu;


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
@ResponseDaifuHandler("LIANFUBAODAIFU")
public final class LianFuBaoDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


    private static final String  merOrderNo   		="merOrderNo";//   商户订单号	String	是	商户订单号
    private static final String  merId            	="merId";         //   商户编号	String	是	
    private static final String  data            	="data";         //   参数列表	String	是	使用RSA加密
    private static final String  key            	="key";         //   参数列表	String	是	使用RSA加密


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(merOrderNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[联付宝代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
    	String signMd5="";
    	try {
			String data = SecurityUtils.decrypt(payParam.get("data"), channelWrapper.getAPI_KEY());
			JSONObject dataJSON = JSONObject.parseObject(data);
			SortedMap<String,Object> bodyMap = new TreeMap<>();
	        bodyMap.put("merOrderNo",dataJSON.getString("merOrderNo"));
	        bodyMap.put("orderState",dataJSON.getInteger("orderState"));
	        bodyMap.put("orderNo",dataJSON.getString("orderNo"));
	        bodyMap.put("amount",dataJSON.getBigDecimal("amount"));
	        List paramKeys = MapUtils.sortMapByKeyAsc(bodyMap);
            StringBuilder signSrc = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isNotBlank(bodyMap.get(paramKeys.get(i)).toString())) {
                	signSrc.append(paramKeys.get(i)).append("=").append(bodyMap.get(paramKeys.get(i))).append("&");
                }
            }
            signSrc.append(key+"="+channelWrapper.getAPI_MEMBERID().split("&")[1]);
            String paramsStr = signSrc.toString();
            signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
        log.debug("2.[联付宝代付]-[代付回调]-生成md5签名：{}",signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
    	boolean checkResult = false;
    	String data="";
		try {
			data = SecurityUtils.decrypt(api_response_params.get("data"), channelWrapper.getAPI_KEY());
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject dataJSON = JSONObject.parseObject(data);
        String responseAmount =  HandlerUtil.getFen(dataJSON.getString("amount"));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[联付宝代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	String data="";
		try {
			data =  SecurityUtils.decrypt(api_response_params.get("data"), channelWrapper.getAPI_KEY());
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject dataJSON = JSONObject.parseObject(data);
        boolean result = dataJSON.getString("sign").equalsIgnoreCase(signMd5);
        log.debug("4.[联付宝代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[联付宝代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
    	PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
    	String data="";
		try {
			data =  SecurityUtils.decrypt(api_response_params.get("data"), channelWrapper.getAPI_KEY());
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject dataJSON = JSONObject.parseObject(data);
        if(dataJSON.containsKey("orderState")){
           if( "2".indexOf(dataJSON.get("orderState").toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "0".indexOf(dataJSON.get("orderState").toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "1".indexOf(dataJSON.get("orderState").toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[联付宝代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }

}