package dc.pay.business.tongyingzhifu;

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
 * 
 * @author andrew
 * Sep 5, 2019
 */
@ResponsePayHandler("TONGYINGZHIFU")
public final class TongYingZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //下单时传入的回调地址
    //返回说明：money=订单金额 realMoney=实际付款金额 orderNum=你的订单号 sign=签名(订单金额+实际付款金额+你的订单号;MD5加密三次) sign1=签名(订单金额+实际付款金额+你的订单号+商户密匙;MD5加密三次) 推荐使用sign1验签
    //
    //money=订单金额 
    private static final String money                ="money";
    //realMoney=实际付款金额 
    private static final String realMoney                ="realMoney";
    //orderNum=你的订单号 
    private static final String orderNum                ="orderNum";
    //sign=签名(订单金额+实际付款金额+你的订单号;MD5加密三次) 
//    private static final String sign                ="sign";
    //sign1=签名(订单金额+实际付款金额+你的订单号+商户密匙;MD5加密三次) 推荐使用sign1验签
//    private static final String sign1                ="sign1";
    //收到回调信息且完成相关逻辑后 请返回 'success'

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign1";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[通盈支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[通盈支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNum);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[通盈支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
      //sign1=签名(订单金额+实际付款金额+你的订单号+商户密匙;MD5加密三次) 推荐使用sign1验签
        signStr.append(api_response_params.get(money));
        signStr.append(api_response_params.get(realMoney));
        signStr.append(api_response_params.get(orderNum));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase()).toLowerCase()).toLowerCase();
        log.debug("[通盈支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
        String payStatusCode = "无";
        String responseAmount = HandlerUtil.getFen(api_response_params.get(realMoney));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[通盈支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[通盈支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[通盈支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[通盈支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}