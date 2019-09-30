package dc.pay.business.dufuzhifu;

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
 * @author sunny
 * 26 04, 2019
 */
@ResponsePayHandler("DUFUZHIFU")
public final class DuFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数					参数名称 				类型（长度）			使用			说明
//    merchant_code			商家号				String(12)			必选			商户签约时，支付平台分配的唯一商家号。
//    notify_type			通知类型				String(14)			必选			取值如下：服务器后台异步通知：offline_notify
//    notify_id				通知校验ID			String(100)			必选			商家系统接收到此通知消息后，用此校验ID向支付平台校验此通知的合法性，由32位数字和字母组成。例如：e722dceae317466bbf9cc5f1254b8b0a
//    interface_version		接口版本				String(10)			必选			接口版本，固定值：V3.0
//    sign_type				签名方式				String(10)			必选			RSA或RSA-S，不参与签名
//    sign					签名					String				必选			签名数据，详见附录中的签名规则定义。
//    order_no				商户网站唯一订单号		String(100)			必选			商户系统订单号，由商户系统保证唯一性，最长64位字母、数字组成，举例：1000201555。
//    order_time			商户订单时间			Date				必选			商户订单时间，格式：yyyy-MM-dd HH:mm:ss，举例：2013-11-01 12:34:54。
//    order_amount			商户订单总金额			Number(13,2)		必选			该笔订单的总金额，以元为单位，精确到小数点后两位，举例：12.01。
//    extra_return_param	公用回传参数			String(100)			可选			如果商户网站支付请求时传递了该参数，则通知商户支付成功时会回传该参数。
//    trade_no				交易订单号			String(30)			必选			交易订单号，举例：1000004817
//    trade_time			交易订单时间			Date				必选			交易订单时间，格式为：yyyy-MM-dd HH:mm:ss，举例：2013-12-01 12:23:34。
//    trade_status			交易状态				String(7)			必选			该笔订单交易状态
//    bank_seq_no			网银交易流水号			String(50)			可选			银行交易流水号，举例：2013060911235456。

    private static final String merchant_code                   ="merchant_code";
    private static final String notify_type                     ="notify_type";
    private static final String notify_id                  		="notify_id";
    private static final String interface_version               ="interface_version";
    private static final String sign_type             			="sign_type";
    private static final String order_no                 		="order_no";
    private static final String order_time              		="order_time";
    private static final String order_amount              		="order_amount";
    private static final String extra_return_param              ="extra_return_param";
    private static final String trade_no              			="trade_no";
    private static final String trade_time              		="trade_time";
    private static final String trade_status              		="trade_status";
    private static final String bank_seq_no              		="bank_seq_no";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_code);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[都付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	paramKeys.remove(sign_type);
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        String paramsStr = signSrc.toString();
        boolean signMD5=false;
		try {
			signMD5 = RSAWithSoftware.validateSignByPublicKey(signSrc.toString(),channelWrapper.getAPI_PUBLIC_KEY() , api_response_params.get(signature));
		} catch (Exception e) {
			e.printStackTrace();
		}
        log.debug("[都付支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return String.valueOf(signMD5);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(order_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[都付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[都付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = Boolean.valueOf(signMd5);
        log.debug("[都付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[都付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}