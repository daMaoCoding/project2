package dc.pay.business.huitongzhifu;

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
 * Sep 3, 2019
 */
@ResponsePayHandler("HUITONGZHIFU")
public final class HuiTongZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称    是否必填    是否签名    数据类型    参数示例    描述
    //amount        true    true    number  198.00  支付金额（元）
    private static final String amount                ="amount";
    //real_amount true    true    number  197.00  实际支付金额（元）
    private static final String real_amount                ="real_amount";
    //app_id  True    true    String  2019027 商户号
    private static final String app_id                ="app_id";
    //plat_order_no   true    true    string  201900015   订单编号(平台生成订单的编号)
    private static final String plat_order_no                ="plat_order_no";
    //order_no    true    true    string  1023528753  商户订单编号(商户推送订单编号)
    private static final String order_no                ="order_no";
    //sign    True    False   string  YJZoOFxuf775WGXjYVvKRwen/sL2RarHcfynsepwjmyE1wjtci82qoSKYIWtX3AFTbxmpWFn86G3FSEZdQANCj0ZotRzXniRaO8moy3Qr6Ro3QtCcnfVvi19c4iI5IldTeGHAgdRnrZUq7fNeVm1sMeylmgx597hCHT9JiyAT2HhFHmpYy91aPyLDapWQxnBpqMsLlGty8r3eMi7A4FzYDxiGJfSc7XXhAtYpoW5TBmFMf8Bc4AsEqRi5LpH5naLr+XLgwaXHbgHb6w/y01Xnn7w605o9q5iWxTk1r9yfixT8aX2Nzfd7Qh2a0zYCEtuvgdfJ0REnPAcRawK54d9gQ==    签名后的数据，建议非JAVA语音选择MD5方式    MD5的密钥在商户后台查看，将上述参数按顺序拼接后，添加app_secret=xxxx后，MD5签名
//    private static final String sign                ="sign";
    
    private static final String key        ="app_secret";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[汇通支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[汇通支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(app_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇通支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signStr.append(app_id+"=").append(api_response_params.get(app_id)).append("&");
        signStr.append(order_no+"=").append(api_response_params.get(order_no)).append("&");
        signStr.append(plat_order_no+"=").append(api_response_params.get(plat_order_no)).append("&");
        signStr.append(real_amount+"=").append(api_response_params.get(real_amount)).append("&");
        signStr.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇通支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
//        String payStatusCode = api_response_params.get(status);
        String payStatusCode = "无";
        String responseAmount = HandlerUtil.getFen(api_response_params.get(real_amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[汇通支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[汇通支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[汇通支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇通支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}