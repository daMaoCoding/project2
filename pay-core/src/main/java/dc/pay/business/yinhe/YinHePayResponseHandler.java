package dc.pay.business.yinhe;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("YINHE")
public final class YinHePayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    键名				描述					示例
//    money				金额	如：10.05
//    orderNum			传入的给B方的唯一单号	如：1539444859247
//    trade_no			支付宝交易号	如：2018101322001428770511706100
//    Type				订单状态 
//    					0=等待充值 
//    					1=充值成功
//    					2=关闭交易
//    					3=充值失败 	
//    Sign				数据签名	




    private static final String money                    ="money";
    private static final String orderNum                 ="orderNum";
    private static final String trade_no                 ="trade_no";
    private static final String Type              		 ="type";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("OK");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
    	API_RESPONSE_PARAMS = HandlerUtil.oneSizeJsonMapToMap(API_RESPONSE_PARAMS);
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNum);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[银河支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	API_RESPONSE_PARAMS = HandlerUtil.oneSizeJsonMapToMap(api_response_params);
    	List paramKeys = MapUtils.sortMapByKeyAsc(API_RESPONSE_PARAMS);
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(API_RESPONSE_PARAMS.get(paramKeys.get(i)))) {
            	signSrc.append(API_RESPONSE_PARAMS.get(paramKeys.get(i)));
            }
        }
        String paramsStr = signSrc.append(api_key).toString();
        String signMD5 = HandlerUtil.getPhpMD5(paramsStr);
    	/*ArrayList<String> list = new ArrayList<String>();
 		for (Map.Entry<String, String> entry : api_response_params.entrySet()) 
 		{
 			if (entry.getKey().equals("sign")) 
 			{
 				continue;
 			}
 			list.add(entry.getValue());
 		}
 		int size = list.size();
 		String[] arrayToSort = list.toArray(new String[size]);
 		StringBuilder sb = new StringBuilder();
 		for (int i = 0; i < size; i++) 
 		{
 			sb.append(arrayToSort[i]);
 		}*/
        log.debug("[银河支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	//API_RESPONSE_PARAMS = HandlerUtil.oneSizeJsonMapToMap(api_response_params);
        boolean my_result = false;
        //returncode          交易状态         是            “00” 为成功
        String payStatusCode = api_response_params.get(Type);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //实际支付金额  有可能比提交金额少0.01-0.05
        boolean checkAmount =  HandlerUtil.isRightAmount(db_amount,responseAmount,"5");//第三方回调金额差额0.05元内
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[银河支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[银河支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	//API_RESPONSE_PARAMS = HandlerUtil.oneSizeJsonMapToMap(api_response_params);
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[银河支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[银河支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}