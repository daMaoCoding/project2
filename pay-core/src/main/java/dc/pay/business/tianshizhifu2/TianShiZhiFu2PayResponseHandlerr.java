package dc.pay.business.tianshizhifu2;

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
 * Jul 2, 2019
 */
@ResponsePayHandler("TIANSHIZHIFU2")
public final class TianShiZhiFu2PayResponseHandlerr extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名 是否必需    类型  说明
    //code    是   Int 充值结果    0-充值成功 1-充值失败
    private static final String code        ="code";
    //msg 否   String  错误消息
//    private static final String msg        ="msg";
    //data    是   String  本平台生成的订单号
    private static final String data        ="data";
    //amount        是   String  订单金额（单位元）
    private static final String amount        ="amount";
    //orderid        是   String  渠道订单号
    private static final String orderid        ="orderid";
    //orderno        是   String  备用，暂存渠道订单号
//    private static final String orderno        ="orderno";
    //flowid  是   String  运营商对应订单的流水号
//    private static final String flowid        ="flowid";
    //sign    是   String  签名，参考签名算法，计算出来的值全部小写
//    private static final String sign        ="sign";
    
    //signature 数据签名    32  是   　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(seller_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[天使支付2]-[响应支付]-TRADE_FINISHED.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(code));
        signSrc.append(api_response_params.get(data));
        signSrc.append(api_key);
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[天使支付2]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //code  是   Int 充值结果        0-充值成功 1-充值失败
        String payStatusCode = api_response_params.get(code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //TRADE_FINISHED代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[天使支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[天使支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[天使支付2]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[天使支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}