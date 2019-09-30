package dc.pay.business.yafu;/**
 * Created by admin on 2017/5/25.
 */

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */

@ResponsePayHandler("YAFU")
public class YaFuPayResponseHandler extends PayResponseHandler {
    private static final Logger log =  LoggerFactory.getLogger(YaFuPayResponseHandler.class);
    private static final String  consumerNo  = "consumerNo";       //: "21107",
    private static final String  merOrderNo  = "merOrderNo";       //: "YAFU_QQ_SM-qGc2W",
    private static final String  orderNo  = "orderNo";            //: "20171207161506496522",
    private static final String  orderStatus  = "orderStatus";    //: "1",
    private static final String  payType  = "payType";            //: "0502",
    private static final String  sign  = "sign";                  //: "16BFD367142DB6B646B0B3A32403E2B2",
    private static final String  transAmt  = "transAmt";          //: "0.01",
    private static final String  version  = "version";            //: "3.0"
    private static final String  RESPONSE_PAY_MSG_OK  = "SUCCESS";


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if(null==API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String orderId = API_RESPONSE_PARAMS.get(merOrderNo);
        if(StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_PARAM_ERROR);
        log.debug("[雅付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
       // consumerNo=12004&merOrderNo=1494778744867&orderNo=20170515001905084693&orderStatus=1&payType=0202&transAmt=1.50&version=3.0&key=FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
        String paramsStr = String.format("consumerNo=%s&merOrderNo=%s&orderNo=%s&orderStatus=%s&payType=%s&transAmt=%s&version=%s&key=%s",
                api_response_params.get(consumerNo),
                api_response_params.get(merOrderNo),
                api_response_params.get(orderNo),
                api_response_params.get(orderStatus),
                api_response_params.get(payType),
                api_response_params.get(transAmt),
                api_response_params.get(version),
                channelWrapper.getAPI_KEY());

        String pay_md5sign =HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[雅付支付]-[响应支付]-2.响应内容生成md5完成：" + pay_md5sign);
        return  pay_md5sign;
    }
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode =  api_response_params.get(orderStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(transAmt));
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if(checkAmount && payStatusCode.equalsIgnoreCase("1")){
            result = true;
        }else{
            log.error("[雅付支付]-[响应支付]金额及状态验证错误,订单号："+channelWrapper.getAPI_ORDER_ID()+",第三方支付状态："+payStatusCode +" ,支付金额："+responseAmount+" ，应支付金额："+amount);
        }
        log.debug("[雅付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果："+ result+" ,金额验证："+checkAmount+" ,responseAmount="+responseAmount +" ,数据库金额："+amount+",第三方响应支付成功标志:"+payStatusCode+" ,计划成功：1");
        return result;
    }
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[雅付支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }
    @Override
    protected String responseSuccess() {
        log.debug("[雅付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG_OK);
        return RESPONSE_PAY_MSG_OK;
    }
}