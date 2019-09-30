package dc.pay.business.xinzhongfu;

import java.util.List;
import java.util.Map;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;


import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;


@ResponsePayHandler("XINZHONGFU")
public final class XinZhongFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    private static final String mchid                ="mchid";         //mchid string 是 商户id ( 接入编号 )
    private static final String order_id             ="order_id";      //order_id string 是 商户订单号
//    private static final String channel_id           ="channel_id";    //channel_id string 是 通道编号 (请找平台方运营人员获取)
    private static final String total_amount         ="total_amount";  //total_amount string 是 订单总额
//    private static final String return_url           ="return_url";    //return_url string 是 异步回调地址
    private static final String sign           ="sign";    //app_secret string 是 接入密匙

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"status\":\"true\",\"msg\":\"支付成功!\"}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新众付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)+api_response_params.get(paramKeys.get(i)));
            }
        }
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
	    String signSha1 = Sha1Util.getSha1(paramsStr).toUpperCase();
        log.debug("[新众付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signSha1) );
        return signSha1;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
//        String payStatusCode = "";  无状态判断
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        if (checkAmount  ) {
            my_result = true;
        } else {
            log.error("[新众付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + false + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新众付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + true + " ,计划成功：true");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get("sign").equalsIgnoreCase(signMd5);
        log.debug("[新众付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新众付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}