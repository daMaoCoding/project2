package dc.pay.business.mafuzhifu;

import java.util.List;
import java.util.Map;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;


/**
 * @author cobby
 * Jan 26, 2019
 */
@ResponsePayHandler("MAFUZHIFU")
public final class MaFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

// orderno	平台生成的订单ID号	string	一定存在。是此订单在本服务器上的唯一编号
// order_id	您的自定义订单号	string	一定存在。是您在发起付款接口传入的您的自定义订单号
// money	订单定价	float	一定存在。是您在发起付款接口传入的订单价格
// real_money	实际支付金额	float	一定存在。表示用户实际支付的金额。一般会和price值一致，如果同时存在多个用户支付同一金额，就会和price存在一定差额，差额一般在1-2分钱上下，越多人同时付款，差额越大。
// pay_way	您的支付方式	string(1)	如果您在发起付款类型 1支付宝 2微信。
// account_id	您在平台的收款账号id	string(32)	一定存在。
// product_id	您在平台的产品id	string(32)	一定存在。
// uid	您的商户ID	string(32)	一定存在。
// goodsname	商品名称	string(32)	一定存在。是您在发起付款接口传入的商品名称
// remark	备注	string(32)	一定存在。是您在发起付款接口传入的备注
// key	密钥	string(32)	一定存在。我们把使用到的所有参数，连您的Token一起，按参数名字母升序排序。把参数值拼接在一起。做md5-32位加密，取字符串小写。得到key。您需要在您的服务端按照同样的算法，自己验证此key是否正确。只在正确时，执行您自己逻辑中支付成功代码。
    private static final String orderno                  ="orderno";
    private static final String order_id                 ="order_id";
    private static final String money                    ="money";
    private static final String real_money               ="real_money";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="key";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(orderno);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[码付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!key.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[码付支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
//        String payStatusCode = api_response_params.get(real_money);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(real_money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount) {
            my_result = true;
        } else {
            log.error("[码付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + "失败" + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[码付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + "成功" + " ,计划成功：成功");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[码付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[码付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}