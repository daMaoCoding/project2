package dc.pay.business.caishi;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 27, 2018
 */
@ResponsePayHandler("CAISHI")
public final class CaiShiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

  //外部订单号	outOid		String		必填		是	
    private static final String outOid  ="outOid";
  //平台商户编号	merchantCode	String		必填		是	
    private static final String merchantCode  ="merchantCode";
  //平台集团商户编号mgroupCode	String		必填		是	
    private static final String mgroupCode  ="mgroupCode";
  //支付类型	payType		String		必填		是	参考附录支付类型
    private static final String payType  ="payType";
  //支付金额	payAmount	String		必填		是	单位：分
    private static final String payAmount  ="payAmount";
  //业务类型	busType		String		非必填		非空是	
    private static final String busType  ="busType";
  //交易金额	tranAmount	String		必填		是	单位：分
    private static final String tranAmount  ="tranAmount";
  //支付状态	orderStatus	String		必填		是	参考附录支付状态
    private static final String orderStatus  ="orderStatus";
  //平台订单号	platformOid	String		必填		是	
    private static final String platformOid  ="platformOid";
  //时间戳	timestamp	long		必填		是
    private static final String timestamp  ="timestamp";
    private static final String key  ="key";
    private static final String signature  ="sign";
    	
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantCode);
        String ordernumberR = API_RESPONSE_PARAMS.get(outOid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[彩世]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(merchantCode+"=").append(api_response_params.get(merchantCode)).append("&");
		signSrc.append(mgroupCode+"=").append(api_response_params.get(mgroupCode)).append("&");
		signSrc.append(orderStatus+"=").append(api_response_params.get(orderStatus)).append("&");
		signSrc.append(outOid+"=").append(api_response_params.get(outOid)).append("&");
		signSrc.append(payAmount+"=").append(api_response_params.get(payAmount)).append("&");
		signSrc.append(payType+"=").append(api_response_params.get(payType)).append("&");
		signSrc.append(platformOid+"=").append(api_response_params.get(platformOid)).append("&");
		signSrc.append(timestamp+"=").append(api_response_params.get(timestamp)).append("&");
		signSrc.append(tranAmount+"=").append(api_response_params.get(tranAmount)).append("&");
		signSrc.append(key+"=").append(api_key);
		String paramsStr = signSrc.toString();
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[彩世]-[响应支付]-2.生成加密URL签名完成："+ JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //已支付	2
        String payStatusCode = api_response_params.get(orderStatus);
        String responseAmount = api_response_params.get(tranAmount);
        //db_amount数据库存入的是分 	第三方返回的responseAmount是
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            result = true;
        } else {
            log.error("[彩世]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[彩世]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[彩世]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[彩世]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}