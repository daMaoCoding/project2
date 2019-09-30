package dc.pay.business.xinzhangtuozhifu;

import java.util.Map;

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
 * @author XXXXXXX
 * June 1, 2019
 */
@ResponsePayHandler("XINZHANGTUOZHIFU")
public final class XinZhangTuoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称 是否必须 数据类型 描述
    //sign true string 加密串：平台公钥解密后获取内部参数
    private static final String sign                ="sign";
    //merNo true string 商户号
    private static final String merNo                ="merNo";
    //orderNo true string 订单号
    private static final String orderNo                ="orderNo";
    //amount true string 订单金额
    private static final String amount                ="amount";
    //orderStreamNo true string 订单流水号
    private static final String orderStreamNo                ="orderStreamNo";
    //orderStatus true string 订单交易状态： 0：待支付 1：交易成功    2：交易失败
    private static final String orderStatus                ="orderStatus";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign   ";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[新掌托支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[新掌托支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新掌托支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
      //异步回调||补单都会返回一个sign 根据平台公钥解密后获取到 里面的参数
        String sign= api_response_params.get(signature);
        String paramsStr = null;
        try {
            paramsStr = new String(RSAUtils.decryptByPublicKey(Base64.decode(sign), channelWrapper.getAPI_PUBLIC_KEY()));

        } catch (Exception e) {
            System.out.println(e.getMessage());

        }
        System.out.println("签名源串=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新掌托支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        //异步回调||补单都会返回一个sign 根据平台公钥解密后获取到 里面的参数
        String tmp_sign = api_response_params.get(signature);
        String data = null;
        try {
            data = new String(RSAUtils.decryptByPublicKey(Base64.decode(tmp_sign), channelWrapper.getAPI_PUBLIC_KEY()));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Map<String,String> jsonToMap = JSONObject.parseObject(data, Map.class);
        
        boolean my_result = false;
        //orderStatus true string 订单交易状态： 0：待支付 1：交易成功        2：交易失败
        String payStatusCode = jsonToMap.get(orderStatus);
        String responseAmount = HandlerUtil.getFen(jsonToMap.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[新掌托支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新掌托支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新掌托支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新掌托支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}