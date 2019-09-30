package dc.pay.business.xuanyang;

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
 * Dec 7, 2018
 */
@ResponsePayHandler("XUANYANG")
public final class XuanYangPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段  描述
    //version 接口版本,固定值:1.0
    private static final String version                       ="version";
    //code    错误码:0-有错误、1-成功 注意,此处成功有别于订单支付状态
    private static final String code                       ="code";
    //msg 错误描述:说明错误信息
    private static final String msg                       ="msg";
    //mchid   商户编号
    private static final String mchid                       ="mchid";
    //orderid 平台订单号
    private static final String orderid                       ="orderid";
    //out_trade_id    商户订单号
    private static final String out_trade_id                       ="out_trade_id";
    //status  订单状态:0-失败、1-已支付
    private static final String status                       ="status";
    //amount  订单金额:提交的订单金额
    private static final String amount                       ="amount";
    //payamount   实际付款金额,回调上分请以此校验,勿用订单金额校验！
    private static final String payamount                       ="payamount";
    //applydate   下单时间:格式YmdHis,如:20180120142910
    private static final String applydate                       ="applydate";
    //attach  备注信息:订单提交时的备注
//    private static final String attach                       ="attach";
    //signtype    签名类型:0-MD5 1-:RSA 默认:0
//    private static final String signtype                       ="signtype";
    //sign    签名:所有非空非0字段参与签名
//    private static final String sign                       ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mchid);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_id);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[烜洋]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(applydate+"=").append(api_response_params.get(applydate)).append("&");
        signSrc.append(code+"=").append(api_response_params.get(code)).append("&");
        signSrc.append(mchid+"=").append(api_response_params.get(mchid)).append("&");
        signSrc.append(msg+"=").append(api_response_params.get(msg)).append("&");
        signSrc.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc.append(out_trade_id+"=").append(api_response_params.get(out_trade_id)).append("&");
        signSrc.append(payamount+"=").append(api_response_params.get(payamount)).append("&");
        signSrc.append(status+"=").append(api_response_params.get(status)).append("&");
        signSrc.append(version+"=").append(api_response_params.get(version)).append("&");
        signSrc.append(key+"=").append(api_key);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[烜洋]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status    订单状态:0-失败、1-已支付
        String payStatusCode = api_response_params.get(status);
        //payamount   实际付款金额,回调上分请以此校验,勿用订单金额校验！
        String responseAmount = HandlerUtil.getFen(api_response_params.get(payamount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[烜洋]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[烜洋]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[烜洋]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[烜洋]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}