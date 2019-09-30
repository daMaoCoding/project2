package dc.pay.business.duoqianzhifu;

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
 * @author Cobby
 * Mar 12, 2019
 */
@ResponsePayHandler("DUOQIANZHIFU")
public final class DuoQianZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private static final String returncode       ="returncode";// 售卡结果    MAX(20)    返回代码，“1”代表扫码成功
    private static final String userid           ="userid";    // 商户ID    MAX(10)    商户在中的唯一标识，在直通车中获得
    private static final String orderid          ="orderid";   // 商户订单号    Max (30)    提交的订单号在商户系统中必须唯一
    private static final String money            ="money";     // 订单金额    Max(10)    支付金额
    private static final String sign             ="sign";      // 签名数据    Max (50)    32位小写的组合加密验证串
    private static final String sign2            ="sign2";     // 签名数据2    Max(50)    32位小写的组合加密验证串

    private static final String keyvalue        ="keyvalue";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(userid);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[多乾支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//        sign = md5( tolower(returncode={}&userid={}&orderid={}&keyvalue={}))
        StringBuilder signStr1 = new StringBuilder();
        signStr1.append(returncode+"=").append(api_response_params.get(returncode)).append("&");
        signStr1.append(userid+"=").append(api_response_params.get(userid)).append("&");
        signStr1.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signStr1.append(keyvalue+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr1 =signStr1.toString();
        String sign1Md5 = HandlerUtil.getMD5UpperCase(paramsStr1).toLowerCase();

//        sign2 = md5(tolower(money={}&returncode={}&userid={}&orderid={}&keyvalue={}))
        StringBuilder signStr2 = new StringBuilder();
        signStr2.append(money+"=").append(api_response_params.get(money)).append("&");
        signStr2.append(returncode+"=").append(api_response_params.get(returncode)).append("&");
        signStr2.append(userid+"=").append(api_response_params.get(userid)).append("&");
        signStr2.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signStr2.append(keyvalue+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr2 =signStr2.toString();
        String sign2Md5 = HandlerUtil.getMD5UpperCase(paramsStr2).toLowerCase();

        StringBuilder signMd5Str = new StringBuilder();
        signMd5Str.append(sign1Md5).append("&").append(sign2Md5);

        String signMd5 = signMd5Str.toString();
        log.debug("[多乾支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
        String payStatusCode = api_response_params.get(returncode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[多乾支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[多乾支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String[] signSplit = signMd5.split("&");
        boolean my_result = false;
        if (signSplit.length==2){
            String sign1Md5 = signSplit[0];
            String sign2Md5 = signSplit[1];
            my_result = api_response_params.get(sign).equalsIgnoreCase(sign1Md5);
            if (my_result){
                my_result = api_response_params.get(sign2).equalsIgnoreCase(sign2Md5);
            }
        }
        log.debug("[多乾支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[多乾支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}