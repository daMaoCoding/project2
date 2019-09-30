package dc.pay.business.yinxinzhifu;

import java.util.Map;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * @author cobby
 * Jan 28, 2019
 */
@ResponsePayHandler("YINXINZHIFU")
public final class YinXinZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//orderNo	String	必选		平台唯一单号	"orderNo": "be8b505854ba4fb88be0f215a17da78d"
//orderid	String	必选	50	订单id	SH-98d6cffa-d742-4f30-9637-0
//orderuid	String	必选	20	客户定义的id	"orderuid": "12"
//paymoney	String	必选	7	支付金额	"paymoney": "0.01"
//payStatus	String	必选		支付状态 0:待支付 1:支付成功 其他:支付失败	"payStatus": "1"
//createTime	String	必选		支付时间	"createTime": "2018-10-18 19:41:38"
//signStr	String	可选		签名字符串	"signStr":"orderNo=PAYS-f5f646adaa264ca3a7b1a375fb5a58fc&orderid=TEST-c71f8079-2506-5de4-5797-f24860a953a9"
//sign	String	可选		签名,加密后的签名	"sign":"CB8036D6CE499E23642C0C6BEA7DBBB7"
    private static final String orderNo                ="orderNo";
    private static final String orderid                 ="orderid";
    private static final String paymoney                ="paymoney";
    private static final String orderno                ="orderno";
    private static final String payStatus              ="payStatus";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(orderNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[银鑫支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//      orderNo=PAYS-f5f646adaa264ca3a7b1a375fb5a58fc&orderid= TEST-c71f8079-2506-5de4-5797-f24860a953a9

        StringBuilder signSrc = new StringBuilder();
        signSrc.append(orderNo +"="+ api_response_params.get(orderNo)).append("&");
        signSrc.append(orderid+"="+channelWrapper.getAPI_ORDER_ID());
        String paramsStr = signSrc.toString();
        System.out.println("签名源串=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[银鑫支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
        String payStatusCode = api_response_params.get(payStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(paymoney));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
//        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[银鑫支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[银鑫支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[银鑫支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[银鑫支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}