package dc.pay.business.shanghutong;

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
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author andrew
 * Sep 16, 2019
 */
@ResponsePayHandler("SHANGHUTONG")
public final class ShangHuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //POST 地址 商户在订单里提供的 notify_url，例如：http://x.com/notify
    //POST 参数名    含义说明    例子
    //order_no    商户网站订单号 例如：abcd1234
    private static final String order_no                ="order_no";
    //defined_yuan    订单原始人民币金额，单位为元  例如：12.00
    private static final String defined_yuan                ="defined_yuan";
    //actual_yuan 实际支付的人民币金额，单位为元 例如：11.00
    private static final String actual_yuan                ="actual_yuan";
    //sign    各请求参数按顺序的签名，全部采用小写，保证请求由商户通平台发起 例如：7e246b45b47b81a1b7a31319c0aa6ba4c1ad5d47
//    private static final String sign                ="sign";
    //注意！针对后台异步回调模式，商户网站需要返回字符串“success”（小写，不包括引号）来告诉商户通已成功收到该回调并已将该订单妥善处理成已支付状态。
        
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[商户通]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[商户通]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[商户通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
//        sign 的算法为：sha1( +  +  + api_secret)
        signStr.append(api_response_params.get(order_no));
        signStr.append(api_response_params.get(defined_yuan));
        signStr.append(api_response_params.get(actual_yuan));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
//        System.out.println("签名源串=========>"+paramsStr);
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        
        String signMd5 = null;
        try {
            signMd5 = Sha1Util.getSha1(paramsStr).toUpperCase();
        } catch (Exception e) {
            log.error("[商户通]-[响应支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        
        log.debug("[商户通]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
//        String payStatusCode = api_response_params.get(status);
        String payStatusCode = "无";
        String responseAmount = HandlerUtil.getFen(api_response_params.get(actual_yuan));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[商户通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[商户通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[商户通]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[商户通]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}