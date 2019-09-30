package dc.pay.business.xingdeng;

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
@ResponsePayHandler("XINGDENG")
public final class XingDengPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数					名称					类型				说明
//    resp_code				响应码				string(5)		00表示成功，其它表示失败
//    resp_desc				响应描述				string(100)		响应描述
//    version				版本					string(8)		固定返回1.1
//    appid					商户号				string(20)		系统统一分配的商户号
//    out_trade_no			商户系统订单号			string(32)		原样返回请求参数列表中的out_trade_no
//    amount				支付金额（元）			string(20)		以这个金额上分
//    tran_amount			费率后金额			string(40)		费率后金额
//    pay_no				支付订单号			String(32)		系统订单号
//    pay_id				支付方式				String(20)		参数2.0支付方式表
//    payment				payment				String(200)	
//    sign					签名					String(32)		签名

    private static final String resp_code                    ="resp_code";
    private static final String resp_desc                    ="resp_desc";
    private static final String version                  	 ="version";
    private static final String appid                		 ="appid";
    private static final String out_trade_no             	 ="out_trade_no";
    private static final String amount                 		 ="amount";
    private static final String pay_id                 		 ="pay_id";
    private static final String pay_no                 		 ="pay_no";
    private static final String payment                 	 ="payment";
    private static final String tran_amount                  ="tran_amount";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(appid);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[星灯支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	 String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s%s",
    			 amount+"="+api_response_params.get(amount)+"&",
    			 appid+"="+api_response_params.get(appid)+"&",
    			 out_trade_no+"="+api_response_params.get(out_trade_no)+"&",
    			 pay_id+"="+api_response_params.get(pay_id)+"&",
    			 pay_no+"="+api_response_params.get(pay_no)+"&",
    			 payment+"="+api_response_params.get(payment)+"&",
    			 resp_code+"="+api_response_params.get(resp_code)+"&",
    			 resp_desc+"="+api_response_params.get(resp_desc)+"&",
    			 tran_amount+"="+api_response_params.get(tran_amount)+"&",
    			 version+"="+api_response_params.get(version),
         		channelWrapper.getAPI_KEY()
         );
         String paramsStr = signSrc.toString();
         String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[星灯支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(resp_code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[星灯支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[星灯支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[星灯支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[星灯支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}