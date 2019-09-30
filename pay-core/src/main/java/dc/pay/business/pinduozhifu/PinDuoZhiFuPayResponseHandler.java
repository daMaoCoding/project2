package dc.pay.business.pinduozhifu;

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
 * Mar 18, 2019
 */
@ResponsePayHandler("PINDUOZHIFU")
public final class PinDuoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String remark               ="remark";
    private static final String money                ="money";
    private static final String guo_ordes            ="guo_ordes";
    private static final String order_statues        ="order_statues";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        //  奇葩回调 application/x-www-form-urlencoded
        Map<String, String> paramsMap = this.getParamsMap(API_RESPONSE_PARAMS);
        String ordernumberR = paramsMap.get(remark) ;
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[拼多支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> API_RESPONSE_PARAMS, String api_key) throws PayException {
        Map<String, String> paramsMap = this.getParamsMap(API_RESPONSE_PARAMS);
        String paramsStr = String.format("%s%s%s",
                paramsMap.get(money),
                paramsMap.get(guo_ordes),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[拼多支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //  奇葩回调 application/x-www-form-urlencoded
        Map<String, String> paramsMap = this.getParamsMap(api_response_params);
        //   未付款 已付款
        String payStatusCode = paramsMap.get(order_statues);
        String responseAmount = HandlerUtil.getFen(paramsMap.get(money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //未付款 已付款
        if (checkAmount && payStatusCode.equalsIgnoreCase("已付款")) {
            my_result = true;
        } else {
            log.error("[拼多支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[拼多支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：已付款");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Map<String, String> paramsMap = this.getParamsMap(api_response_params);
        boolean my_result = paramsMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[拼多支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[拼多支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    public Map<String, String> getParamsMap(Map<String, String> api_response_params) {
        Map<String ,String > parse = null;
        try {
            if (api_response_params.size() ==1 ){
                for(Map.Entry<String, String> vo : API_RESPONSE_PARAMS.entrySet()){
                    String key = vo.getKey();
                    Map<String ,Object > map = (Map<String, Object>) JSON.parse(key);
                    String data = map.get("data").toString();
                    parse = (Map<String, String>) JSON.parse(data);
                }
            }else {
                throw new PayException("[拼多支付]-[响应支付]-1.1 获取回调参数异常"+JSON.toJSONString(API_RESPONSE_PARAMS));
            }
        }catch (PayException e) {
            e.printStackTrace();
        }
        return parse;
    }
}