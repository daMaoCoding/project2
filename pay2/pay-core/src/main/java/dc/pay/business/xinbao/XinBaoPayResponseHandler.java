package dc.pay.business.xinbao;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.yunbao.YunBaoUtil;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 16, 2018
 */
@ResponsePayHandler("XINBAO")
public final class XinBaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//字段名		变量名			必填		类型	说明
	//商户ID		P_UserId		是		String	商户ID，由平台分配。
    private static final String P_UserId	  ="P_UserId";
	//商户订单号	P_OrderId		是		String	商户订单号，平台唯一。
    private static final String P_OrderId	  ="P_OrderId";
	//平台订单号	P_SMPayId		是		String	平台订单号，平台唯一。
    private static final String P_SMPayId	  ="P_SMPayId";
	//面值		P_FaceValue		是		String	面值，单位为元，必须是整数
    private static final String P_FaceValue	  ="P_FaceValue";
	//充值渠道		P_ChannelId		是		String	渠道
    private static final String P_ChannelId	  ="P_ChannelId";
//	//商品名称		P_Subject		是		String	商品名称
//    private static final String P_Subject	  ="P_Subject";
//	//签名认证串	P_PostKey		是		String	签名认证串 md5(P_UserId|P_OrderId|P_SMPayId|P_FaceValue|P_ChannelId|秘钥)
//    private static final String P_PostKey	  ="P_PostKey";
//	//支付类型		P_Type			是		String	支付类型：1:微信支付 2:微信扫码 3:支付宝支付 4:支付宝扫码 5:快捷支付
//    private static final String P_Type		  ="P_Type";
    //0	是成功
    private static final String P_ErrCode		  ="P_ErrCode";
    
    //signature	数据签名	32	是	　
    private static final String signature  ="P_PostKey";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(P_UserId);
        String ordernumberR = API_RESPONSE_PARAMS.get(P_OrderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[辛宝]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//签名认证串		P_PostKey		是		String	签名认证串 md5(P_UserId|P_OrderId|P_SMPayId|P_FaceValue|P_ChannelId|秘钥)
    	String signMd5 = YunBaoUtil.Md5(api_response_params.get(P_UserId) + "|" +api_response_params.get(P_OrderId) + "|"+api_response_params.get(P_SMPayId) + "|" 
    										+api_response_params.get(P_FaceValue) + "|"+api_response_params.get(P_ChannelId) + "|" + channelWrapper.getAPI_KEY());
        log.debug("[辛宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //0是成功
        String payStatusCode = api_response_params.get(P_ErrCode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(P_FaceValue));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[辛宝]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[辛宝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[辛宝]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[辛宝]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}