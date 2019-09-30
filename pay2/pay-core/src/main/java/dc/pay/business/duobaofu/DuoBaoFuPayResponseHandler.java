package dc.pay.business.duobaofu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;


/**
 * ************************
 * @author beck 2229556569
 */

@ResponsePayHandler("DUOBAOFU")
public final class DuoBaoFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";

     private static final String  orderNumber = "order_sn";
     private static final String  money = "amount";
     private static final String  signature = "sign";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNumber);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[多宝付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        StringBuilder sb = new StringBuilder();
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(params);
        
        for(int i = 0; i<paramKeys.size();i++){
            Object keyName = paramKeys.get(i);
            String value = params.get(keyName);
            if(!StringUtils.isBlank(value) && keyName.toString() !="sign"){
                sb.append(keyName).append("=").append(value).append("&");
            }
        }
        
        sb.append("app_key=").append(this.channelWrapper.getAPI_KEY());
        
        String paramsStr = sb.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[多宝付]-[响应支付]-2.2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
    

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        
        //执行订单查询
        String status = "无";//this.queryOrderStatus(api_response_params);
        boolean checkResult = false;
        String resultStr = status;//api_response_params.get(payStatus);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(money));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && resultStr.equalsIgnoreCase("无")) {
            checkResult = true;
        } else {
            log.error("[多宝付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + resultStr + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[多宝付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + resultStr + " ,计划成功：无");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[多宝付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[多宝付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    
    /**
     * 订单查询状态
     * */
    private String queryOrderStatus(Map<String, String> api_response_params) throws PayException
    {
        String  queryUrl = "http://www.nkgwss.com/query";
        Map<String,String> payParam = Maps.newHashMap(); 
        payParam.put("app_id", api_response_params.get("app_id"));
        //payParam.put("system_sn", api_response_params.get("system_sn"));
        payParam.put("order_sn", api_response_params.get("order_sn"));
        payParam.put("nonce_str", HandlerUtil.getRandomStr(8));
        
        String sign = this.buildPaySign(payParam,channelWrapper.getAPI_KEY());
        payParam.put("sign", sign);
        String status = null;
        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(queryUrl, payParam, String.class, HttpMethod.POST).trim();
        
        JSONObject result = JSONObject.parseObject(resultStr);
        if(result != null && result.getString("code").equalsIgnoreCase("200")){
            String dataStr = result.getString("data");
            JSONObject dataJson = JSONObject.parseObject(dataStr);
            
            status = dataJson.getString("status");
        }
        
        log.debug("[多宝付]-[响应支付]-6.订单查询结果：" + resultStr);
        
        return status;
    }
}