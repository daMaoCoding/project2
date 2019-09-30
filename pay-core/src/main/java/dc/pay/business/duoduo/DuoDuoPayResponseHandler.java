package dc.pay.business.duoduo;

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
 * Jul 9, 2018
 */
@ResponsePayHandler("DUODUO")
public final class DuoDuoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名            变量名            说明
    //订单状态          resultcode       1 表示订单支付成功， 0 表示订单支付失败
    //商户ID            mchid            商户在平台的 商户ID号
    //商户订单号        mchno            商户自己的订单号
    //订单类型          tradetype        alipayh5 （固定值）表示支付宝h5支付
    //成交金额          totalfee         订单实际成交的金额，单位：元（人民币），保留2位小数，如： 60.00 、 80.18
    //附加数据          attach           商户的附加数据，原样返回，如果含有中文，商户需要utf-8编码
    //MD5签名           sign             sign加密时要按照下面示例：resultcode=1&mchid=10000&mchno=201803051730&tradetype=weixin&totalfee=60.00&attach=yyyxx&key=c4b70b766ea78fe1689f4e4e1afa291akey值为商户在平台的 通信KEY平台组织好数据按以上排列进行MD5加密后 赋值给 sign
    private static final String resultcode                       ="resultcode";
    private static final String mchid                            ="mchid";
    private static final String mchno                            ="mchno";
    private static final String tradetype                        ="tradetype";
    private static final String totalfee                         ="totalfee";
    private static final String attach                           ="attach";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mchid);
        String ordernumberR = API_RESPONSE_PARAMS.get(mchno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[多多]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(resultcode+"=").append(api_response_params.get(resultcode)).append("&");
        signStr.append(mchid+"=").append(api_response_params.get(mchid)).append("&");
        signStr.append(mchno+"=").append(api_response_params.get(mchno)).append("&");
        signStr.append(tradetype+"=").append(api_response_params.get(tradetype)).append("&");
        signStr.append(totalfee+"=").append(api_response_params.get(totalfee)).append("&");
        signStr.append(attach+"=").append(api_response_params.get(attach)).append("&");
        signStr.append("key="+channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[多多]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
      //订单状态          resultcode       1 表示订单支付成功， 0 表示订单支付失败
        String payStatusCode = api_response_params.get(resultcode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(totalfee));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[多多]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[多多]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[多多]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[多多]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}