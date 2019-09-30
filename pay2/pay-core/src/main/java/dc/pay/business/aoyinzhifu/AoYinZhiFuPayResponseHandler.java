package dc.pay.business.aoyinzhifu;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Oct 19, 2018
 */
@ResponsePayHandler("AOYINZHIFU")
public final class AoYinZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名			必选				类型				说明
//    ordercode		是				Integer			订单号
//    status		是				String			状态 0-支付成功,1-等待支付,2-支付失败
//    confirm_amount是				Decimal(16,2)	确认打款金额
//    pay_time		是				Bigint			时间戳
//    keep_info		是				Object			生成订单请求的参数
    
    private static final String ordercode                     ="ordercode";
    private static final String status                 		  ="status";
    private static final String confirm_amount                ="confirm_amount";
    private static final String pay_time                      ="pay_time";
    private static final String keep_info                     ="keep_info";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG =jsonResponsePayMsg("{\"rescode\":10000}");;

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(ordercode);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[傲银支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        log.debug("[傲银支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(String.valueOf(true)) );
        return String.valueOf(true);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        AoYinZhiFuAesCBC aoYinZhiFuAesCBC = new AoYinZhiFuAesCBC();
        String encryptStr="";
        JSONObject jsonObject=null;
    	try {
			 encryptStr = aoYinZhiFuAesCBC.decrypt(api_response_params.get("data"),"utf-8", channelWrapper.getAPI_KEY(), channelWrapper.getAPI_KEY());
			 jsonObject = JSONObject.parseObject(encryptStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
        //code         返回支付状态          0：支付中  1：成功   2：失败  3：关闭
        String payStatusCode = jsonObject.getString(status);
        String responseAmount = HandlerUtil.getFen(jsonObject.getString(confirm_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount =HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[傲银支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[傲银支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = true;
        log.debug("[傲银支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[傲银支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}