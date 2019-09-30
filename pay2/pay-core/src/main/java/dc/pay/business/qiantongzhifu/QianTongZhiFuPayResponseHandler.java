package dc.pay.business.qiantongzhifu;

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
 * Jul 6, 2019
 */
@ResponsePayHandler("QIANTONGZHIFU")
public final class QianTongZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名 变量名 类型  示例值 描述
    //商户入账    income  int 100 商户实际入账金额，单位：分
    private static final String income                ="income";
    //商户ID    mchId   String(30)  20001222    支付中心分配的商户号
    private static final String mchId                ="mchId";
    //应用ID    appId   String(32)  0ae8be35ff634e2abe94f5f32f6d5c4f    该商户创建的应用对应的ID
    private static final String appId                ="appId";
    //支付产品ID    productId   int 8001    支付产品ID
    private static final String productId                ="productId";
    //商户订单号   mchOrderNo  String(30)  20160427210604000490    商户生成的订单号
    private static final String mchOrderNo                ="mchOrderNo";
    //支付金额    amount  int 100 支付金额,单位分
    private static final String amount                ="amount";
    //状态  status  int 1   支付状态,0-订单生成,1-支付中,2-支付成功,3-业务处理完成
    private static final String status                ="status";
    //签名    sign    String(32)  C380BEC2BFD727A4B6845133519F3AD6    签名值，详见签名算法
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[乾通支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[乾通支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mchId);
        String ordernumberR = API_RESPONSE_PARAMS.get(mchOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[乾通支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        //或者直接取出数值类型
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(appId+"=").append(api_response_params.get(appId)).append("&");
        signSrc.append(mchId+"=").append(api_response_params.get(mchId)).append("&");
        signSrc.append(mchOrderNo+"=").append(api_response_params.get(mchOrderNo)).append("&");
        signSrc.append(productId+"=").append(api_response_params.get(productId)).append("&");
        signSrc.append(status+"=").append(api_response_params.get(status)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[乾通支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //状态    status  int 1   支付状态,0-订单生成,1-支付中,2-支付成功,3-业务处理完成
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(amount);

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[乾通支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[乾通支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[乾通支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }
    
    @Override
    protected String responseSuccess() {
        log.debug("[乾通支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}