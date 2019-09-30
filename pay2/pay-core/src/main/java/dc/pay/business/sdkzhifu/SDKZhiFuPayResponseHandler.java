package dc.pay.business.sdkzhifu;

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
 * Jan 24, 2019
 */
@ResponsePayHandler("SDKZHIFU")
public final class SDKZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //名称 类型 说明
    //参数名 参数类型 参数说明
    //appid String 商户号，对应appid
    private static final String appid                        ="appid";
    //fee String 实际⽀付⾦额（单位：分）
    private static final String fee                        ="fee";
    //extra String 透传参数，对应于transp
    private static final String extra                        ="extra";
    //orderid String 我司⽣成的订单流⽔号
    private static final String orderid                        ="orderid";
    //thirdorderid String 外部订单流⽔号（如⽀付宝⽣成的流⽔号）
    private static final String thirdorderid                        ="thirdorderid";
    //unit String ⾦额单位，固定为"fen"
    private static final String unit                        ="unit";
    //status String 状态，success或fail
    private static final String status                        ="status";
    //time String ⽀付时间（格式：yyyy-MM-dd HH:mm:ss）
    private static final String time                        ="time";
    //paymethod String ⽀付⽅式,如"⽀付宝wap",具体值⻅附1
    private static final String paymethod                        ="paymethod";
    //sign String md5(appid + extra + fee + unit + status + paymethod    + secret)    参数名 参数类型 参数说明
//    private static final String sign                        ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(appid );
        String ordernumberR = API_RESPONSE_PARAMS.get(extra);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[sdk支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuffer signSrc= new StringBuffer();
//      sign String md5( +  +  +  +  +     + secret)    参数名 参数类型 参数说明
        signSrc.append(api_response_params.get(appid));
        signSrc.append(api_response_params.get(extra));
        signSrc.append(api_response_params.get(fee));
        signSrc.append(api_response_params.get(unit));
        signSrc.append(api_response_params.get(status));
        signSrc.append(api_response_params.get(paymethod));
      signSrc.append(channelWrapper.getAPI_KEY());
      //删除最后一个字符
      //signSrc.deleteCharAt(paramsStr.length()-1);
      String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[sdk支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status String 状态success或fail
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(fee );
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[sdk支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[sdk支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[sdk支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[sdk支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}