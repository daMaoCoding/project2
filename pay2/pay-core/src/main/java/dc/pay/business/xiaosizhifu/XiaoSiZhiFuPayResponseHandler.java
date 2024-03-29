package dc.pay.business.xiaosizhifu;

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
 * May 29, 2019
 */
@ResponsePayHandler("XIAOSIZHIFU")
public final class XiaoSiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数  说明  是否签名
    //pid 商户编号    是
    private static final String pid                       ="pid";
    //cid 渠道编号    是
    private static final String cid                       ="cid";
    //oid 商户订单号   是
    private static final String oid                       ="oid";
    //sid 支付订单号   是
    private static final String sid                       ="sid";
    //uid 用户id    是
    private static final String uid                       ="uid";
    //amount  订单金额    是
    private static final String amount                       ="amount";
    //ramount 实收金额    否
    private static final String ramount                       ="ramount";
    //stime   成功时间    是
    private static final String stime                       ="stime";
    //code    状态码 是
    private static final String code                       ="code";
    //sign    签名  否
    private static final String sign                       ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "Success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(pid);
        String ordernumberR = API_RESPONSE_PARAMS.get(oid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[小四支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signSrc = new StringBuilder();
//        MD5(“=商户编号&=渠道号&=商户订单&=支付订单&=用户id&=订单金额&=成功时间&=状态码&key=密钥”)
        signSrc.append(pid+"=").append(api_response_params.get(pid)).append("&");
        signSrc.append(cid+"=").append(api_response_params.get(cid)).append("&");
        signSrc.append(oid+"=").append(api_response_params.get(oid)).append("&");
        signSrc.append(sid+"=").append(api_response_params.get(sid)).append("&");
        signSrc.append(uid+"=").append(api_response_params.get(uid)).append("&");
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(stime+"=").append(api_response_params.get(stime)).append("&");
        signSrc.append(code+"=").append(api_response_params.get(code)).append("&");
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[小四支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //code  状态码 是
        String payStatusCode = api_response_params.get(code);
//        String responseAmount = HandlerUtil.getFen(api_response_params.get(ramount));
        String responseAmount = api_response_params.get(ramount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("101")) {
            my_result = true;
        } else {
            log.error("[小四支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[小四支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：101");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[小四支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[小四支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}