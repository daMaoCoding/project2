package dc.pay.business.jiyifu;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 22, 2018
 */
@ResponsePayHandler("JIYIFU")
public final class JiYiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//字段名				变量名					必填			类型				说明
	//协议参数（不参与签名）
	//签名方式				sign_type				否			String(8)			签名类型，取值：MD5、RSA，默认：MD5
	//字符集				input_charset			否			String(8)			字符编码,取值：GBK、UTF-8，默认：GBK。
	//签名				sign					是			String(32)			签名
	//业务参数(retcode和retmsg不参与签名)
	//返回状态码			retcode					是			Int					返回状态码，0表示成功，其他见错误码
	//返回信息				retmsg					否			String(64)			返回信息，如非空，为错误原因。
	//通知类型				notify_type				是			String(10)			通知类型：1-支付	2-退款	
	//交易单号				listid					是			String(28)			我司交易号
	//商户订单号			sp_billno				是			String(32)			商户系统内部的订单号
	//退款单号				refund_listid			否			String(32)			仅为退款通知时存在
	//支付类型				pay_type				是			String(6)			支付方式：扫码支付:800201	刷卡支付:800208	H5支付:800209
	//发起交易时间			tran_time				是			String(14)			发起交易的时间，格式为yyyyMMddhhmmss
	//交易金额				tran_amt				是			int					单位为分
	//交易状态				tran_state				是			Int					支付状态：0-失败；1-成功
	//系统处理时间			sysd_time				是			String(14)			系统处理的时间，格式为yyyyMMddhhmmss
	//退款标识				refund_state			是			Int					退款状态：0-未退款；1-已退款
	//商品描述				item_name				否			String(255)			商品名称或标示
	//附加数据				item_attach				否			String(128)			附加数据，如商品描述等信息
	private static final String sign_type				="sign_type";
//	private static final String input_charset			="input_charset";
	private static final String retcode					="retcode";
	private static final String retmsg					="retmsg";
//	private static final String notify_type				="notify_type";
//	private static final String listid					="listid";
	private static final String sp_billno				="sp_billno";
//	private static final String refund_listid			="refund_listid";
//	private static final String pay_type				="pay_type";
//	private static final String tran_time				="tran_time";
	private static final String tran_amt				="tran_amt";
	private static final String tran_state				="tran_state";
//	private static final String sysd_time				="sysd_time";
//	private static final String refund_state			="refund_state";
//	private static final String item_name				="item_name";
//	private static final String item_attach				="item_attach";

    //signature	数据签名	32	是	　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(sp_billno);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[极易付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign_type.equalsIgnoreCase(paramKeys.get(i).toString()) && !signature.equalsIgnoreCase(paramKeys.get(i).toString()) && !retcode.equalsIgnoreCase(paramKeys.get(i).toString()) && !retmsg.equalsIgnoreCase(paramKeys.get(i).toString()) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[极易付]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //tran_state				是			Int				支付状态：0-失败；1-成功
        String payStatusCode = api_response_params.get(tran_state);
        //tran_amt				是			int				单位为分
        String responseAmount = api_response_params.get(tran_amt);
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[极易付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[极易付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[极易付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[极易付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}