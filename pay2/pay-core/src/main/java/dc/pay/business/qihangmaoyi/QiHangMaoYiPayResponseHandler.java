package dc.pay.business.qihangmaoyi;

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
@ResponsePayHandler("QIHANGMAOYI")
public final class QiHangMaoYiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    #	参数名			含义				类型				说明
//    1	result_code		业务结果			string(16)		SUCCESS或FAIL，注意大写
//    2	platform_trade_no平台生成的订单ID号string(16)		一定存在。一个16位字符串，是此订单在本服务器上的唯一编号
//    3	transaction_id	支付流水号		string(60)		支付宝支付或微信支付的流水订单号
//    4	orderid			您的自定义订单号	string(50)		一定存在。是您在发起付款接口传入的您的自定义订单号
//    5	price			订单定价			float			一定存在。是您在发起付款接口传入的订单价格
//    6	realprice		实际支付金额		float			一定存在。表示用户实际支付的金额。一般会和price值一致，如果同时存在多个用户支付同一金额，就可能会和price存在一定差额，差额一般在1-2分钱上下，越多人同时付款，差额越大。
//    7	orderuid		您的自定义用户ID	string(100)		如果您在发起付款接口带入此参数，我们会原封不动传回。
//    8	attach			附加内容			string(2048)	将会根据您传入的attch字段原样返回
//    9	key			秘钥				str	ing(32)		一定存在。我们把使用到的所有参数，连您的Token一起，按 参数名 字母升序排序。把 参数值 拼接在一起。做md5-32位加密，取字符串小写。得到key。您需要在您的服务端按照同样的算法，自己验证此key是否正确。只在正确时，执行您自己逻辑中支付成功代码。

    private static final String result_code                   ="result_code";
    private static final String platform_trade_no             ="platform_trade_no";
    private static final String transaction_id                ="transaction_id";
    private static final String orderid                		  ="orderid";
    private static final String price             			  ="price";
    private static final String realprice                 	  ="realprice";
    private static final String orderuid              		  ="orderuid";
    private static final String attach              		  ="attach";
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("OK");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[起航贸易支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s", 
         	   api_response_params.get(orderid),
         	   api_response_params.get(platform_trade_no),
         	   api_response_params.get(price),
         	   api_response_params.get(realprice),
         	   channelWrapper.getAPI_KEY()
         );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[起航贸易支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(result_code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(realprice));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[起航贸易支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[起航贸易支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(key).equalsIgnoreCase(signMd5);
        log.debug("[起航贸易支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[起航贸易支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}