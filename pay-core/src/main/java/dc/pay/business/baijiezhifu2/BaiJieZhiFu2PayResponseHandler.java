package dc.pay.business.baijiezhifu2;

import java.io.UnsupportedEncodingException;
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
 * @author mikey
 * Jun 26, 2019
 */
@ResponsePayHandler("BAIJIEZHIFU2")
public final class BaiJieZhiFu2PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
/*
    	段			类型			空		注释
    trade_no		string		否		订单号
    total_fee		string		否		支付金额
    out_trade_no	string		否		商户订单号
    tradingfee		string		否		商户交易手续费
    paysucessdate	string		否		支付成功时间
    sign			string		否		签名，此字段不参与签名
*/

	private static final String trade_no		= "trade_no";		
	private static final String total_fee		= "total_fee";		
	private static final String out_trade_no	= "out_trade_no";	
	private static final String tradingfee		= "tradingfee";		
	private static final String paysucessdate	= "paysucessdate";	
	private static final String sign			= "sign";	
	private static final String key			= "key";		

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[百捷支付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		String pay_md5sign = null;
		List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
			   sb.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
		   }
		}
		sb.append(key+"="+channelWrapper.getAPI_KEY());
		pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase(); // 进行MD5运算，再将得到的字符串所有字符转换为大写
        log.debug("[百捷支付2]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_fee));
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        if (checkAmount) {
            my_result = true;
        } else {
            log.error("[百捷支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + "无" + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[百捷支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount);
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[百捷支付2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[百捷支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}