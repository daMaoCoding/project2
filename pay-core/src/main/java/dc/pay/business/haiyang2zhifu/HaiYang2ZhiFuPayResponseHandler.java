package dc.pay.business.haiyang2zhifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("HAIYANG2ZHIFU")
public final class HaiYang2ZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数									说明	类型							长度										必须			备注
//    merchant							商户号								string	64								Y	商户号
//    money								金额									string	64								Y	
//    payWay								交易方式							string	64								Y	
//    respSts								交易状态							string	64								Y	成功：success  
//    tradeId								订单号								string	128		
//    attach								备注									String	128								N	原样返回，若交易时未上传此参数或此参数为空，则不会回调
//    sign									签名									string	128								Y	签名,详见签名规则说明

    private static final String merchant                   ="merchant";
    private static final String money                       ="money";
    private static final String payWay                     ="payWay";
    private static final String respSts                     ="respSts";
    private static final String tradeId                       ="tradeId";
    private static final String attach                         ="attach";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("OK");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resJson=getResponseJson();
        String partnerR = resJson.getString("merchant");
        String ordernumberR = resJson.getString("tradeId");
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[海洋支付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	JSONObject resJson=getResponseJson();
    	List paramKeys = MapUtils.sortMapByKeyAsc(resJson.getInnerMap());
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
        		signSrc.append(paramKeys.get(i)).append("=").append(resJson.getInnerMap().get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[海洋支付2]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        JSONObject resJson=getResponseJson();
        String payStatusCode =resJson.getString(respSts);
        String responseAmount = HandlerUtil.getFen(resJson.getString(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[海洋支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[海洋支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	boolean my_result=false;
    	try {
			JSONObject resJson=getResponseJson();
			 my_result = resJson.getString(signature).equalsIgnoreCase(signMd5);
		} catch (PayException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        log.debug("[海洋支付2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[海洋支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
    private JSONObject getResponseJson() throws PayException{
    	String responseJson = "";
        for (String keyValue : API_RESPONSE_PARAMS.keySet()) {
        	 responseJson = keyValue;
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(responseJson);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(responseJson);
        }
        if(null==resJson){
        	throw new PayException(responseJson);
        }
        return resJson;
    }
}