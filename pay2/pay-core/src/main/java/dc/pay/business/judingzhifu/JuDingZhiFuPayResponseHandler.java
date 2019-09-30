package dc.pay.business.judingzhifu;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 22, 2019
 */
@ResponsePayHandler("JUDINGZHIFU")
public final class JuDingZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //ordercode     订单号     
    private static final String ordercode  ="ordercode";
    //state         订单状态        10Z交易成功
    private static final String state  ="state";
//    //result          执行结果        200执行成功，最终是否交易成功，要看state节点
//    private static final String result  ="result";
    //sign          加密签名        详见4.6.3中签名效验规范
    private static final String sign  ="sign";
//    //callbackurl     商户回调的地址     商户上送的回调地址
    private static final String callbackurl  ="callbackurl";
//    //callbackMemo    回调附加信息      initOrder时由商户填入，原样送回
    private static final String callbackMemo  ="callbackMemo";
    //goodsid           产品ID        如需开通，请联系运营人员
    private static final String goodsId  ="goodsId";
    //amount            交易金额        如需开通，请联系运营人员
    private static final String amount  ="amount";
//    //actAmount       实际到账金额      如需开通，请联系运营人员
//    private static final String actAmount  ="actAmount";
    
    private static final String key  ="key";
        
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = null;
        try {
            parseObject = JSON.parseObject(next);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String ordernumberR = parseObject.get(ordercode).toString();
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[聚鼎支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        StringBuilder signStr = new StringBuilder();
        signStr.append(amount).append("=").append(parseObject.getString(amount)).append("&");
        signStr.append(callbackMemo).append("=").append(parseObject.getString(callbackMemo)).append("&");
        signStr.append(callbackurl).append("=").append(parseObject.getString(callbackurl)).append("&");
        signStr.append(goodsId).append("=").append(parseObject.getString(goodsId)).append("&");
        signStr.append(key).append("=").append(channelWrapper.getAPI_KEY()).append("&");
        signStr.append(ordercode).append("=").append(parseObject.getString(ordercode)).append("&");
        signStr.append(state).append("=").append(parseObject.getString(state));
        String paramsStr = signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"UTF-8").toLowerCase();
        log.debug("[聚鼎支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        boolean result = false;
        //state     10Z交易成功
        String payStatusCode = parseObject.getString(state);
        String responseAmount = handlerUtil.getFen(parseObject.getString(amount));
        //amount数据库存入的是分    第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("10Z")) {
            result = true;
        } else {
            log.error("[聚鼎支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[聚鼎支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：10Z");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        boolean result = parseObject.get(sign).toString().equalsIgnoreCase(signMd5);
        log.debug("[聚鼎支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[聚鼎支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}