package dc.pay.business.tangniuzhifu;

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
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("TANGNIUZHIFU")
public final class TangNiuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数				参数名称				数据类型				必填		描述
//    merchant_no		商户编号				String(32)			M		商户编号
//    pay_type			支付方式				String(12)			M		支付方式
//    order_no			商户订单号			String(36)			M		商户订单号
//    product_name		商品名称				String(32)			M		商品名称
//    order_amount		订单金额				String(15)			M		订单金额
//    pay_amount		实际支付金额			String(15)					实际支付金额
//    ord_status		交易状态				String(20)			M		SUCCESS 交易成功 FINISH 订单完成FAILED 交易失败LOSE 订单关闭
//    fee				手续费				Number(15,2)		C	
//    complete_time		交易完成时间			String(19)			C		格式yyyy-MM-dd HH:mm:ss
//    payment_trx_no	平台交易流水号			String(36)			M		平台返回交易流水号
//    remark			备注					String(200)			C		备注
//    sign				签名	String(50)		M							签名

    private static final String merchant_no                   	="merchant_no";
    private static final String order_no                    	="order_no";
    private static final String pay_amount                  	="pay_amount";
    private static final String ord_status                		="ord_status";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_no);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[唐牛支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
       List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
       paramKeys.remove(signature);
 	   StringBuilder signSrc = new StringBuilder();
 	   for (int i = 0; i < paramKeys.size(); i++) {
 	          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
 	          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
 	          }
 	    }
       signSrc.append(key+"="+channelWrapper.getAPI_KEY());
       String paramsStr = signSrc.toString();
       String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
       log.debug("[唐牛支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
       return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(ord_status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(pay_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount =HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[唐牛支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[唐牛支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[唐牛支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[唐牛支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}