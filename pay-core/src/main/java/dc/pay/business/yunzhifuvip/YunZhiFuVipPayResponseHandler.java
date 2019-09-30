package dc.pay.business.yunzhifuvip;

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
 * Mar 11, 2019
 */
@ResponsePayHandler("YUNZHIFUVIPZHIFU")
public final class YunZhiFuVipPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

// key     md5验证     string     拼接顺序 伪代码 md5(auth_code+trade_no+order_id+channel+money+remark+order_uid+goods_name)
// trade_no     交易号     string     支付宝或者微信里的交易号，此交易号是唯一的，您可在数据库里存储此字段，用来判断是否已经回接收过通知了，防止重复到帐加款等操作。
// order_id     订单号     string     您在发起付款接口里传入的订单号
// channel     渠道     string     支付宝-参数:alipay、微信-参数:wechat、云闪付-参数:unionpay、支付宝转银行卡-参数:alipaybank、支付宝当面付-参数:alipayf2f、支付宝红包-参数:alipaybag、聊天宝-参数:bullet、钉钉-参数:dingding
// money     金额     double/float     订单金额
// remark     备注     String     二维码上的备注字符串
// order_uid     用户编号     String     您在发起付款接口里传入的用户编号
// goods_name     商品名称     String     您在发起付款接口里传入的商品名称
    private static final String trade_no                ="trade_no"; // 交易号
    private static final String order_id                 ="order_id"; // 订单号
    private static final String channel                ="channel"; // 渠道
    private static final String money                ="money"; // 金额
    private static final String remark              ="remark"; // 备注
    private static final String order_uid                 ="order_uid"; // 用户编号
    private static final String goods_name              ="goods_name"; // 商品名称

    //signature    数据签名    32    是    　
    private static final String signature  ="key";

    private static final String RESPONSE_PAY_MSG = "200";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(trade_no);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[云支付V]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//        md5(auth_code+trade_no+order_id+channel+money+remark+order_uid+goods_name)
        StringBuilder signStr = new StringBuilder();
        signStr.append(channelWrapper.getAPI_KEY());
        signStr.append(api_response_params.get(trade_no));
        signStr.append(api_response_params.get(order_id));
        signStr.append(api_response_params.get(channel));
        signStr.append(api_response_params.get(money));
        signStr.append(api_response_params.get(remark));
        signStr.append(api_response_params.get(order_uid));
        signStr.append(api_response_params.get(goods_name));
        String paramsStr =signStr.toString();

        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[云支付V]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;

        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount ) {
            my_result = true;
        } else {
            log.error("[云支付V]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[云支付V]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount );
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[云支付V]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[云支付V]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}