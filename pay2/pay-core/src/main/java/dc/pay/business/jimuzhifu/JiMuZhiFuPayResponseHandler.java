package dc.pay.business.jimuzhifu;


import java.util.List;
import java.util.Map;

import dc.pay.utils.MapUtils;
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
 * 积木支付
 */
@ResponsePayHandler("JIMUZHIFU")
public final class JiMuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    private static final String mer_no            ="mer_no";      //  商户编号   商户编号
    private static final String amt               ="amt";         //    订单金额   订单金额单位分 积木收银平台地址，商户只需跳转该地址即可
    private static final String order_no          ="order_no";    //    商户订单号  订单号
    private static final String pay_status        ="pay_status";  //    支付结果    1-待支付，2-支付成功，3-支付失败，12-手动确认支付
//    private static final String finish_time       ="finish_time"; //  支付时间   格式:YYYYMMDDHHmmss
//    private static final String reason            ="reason";      //  失败原因
//    private static final String sign_type         ="sign_type";   //  签名类型   目前只支持MD5
    private static final String sign              ="sign";        //    签名       数字签名


    private static final String RESPONSE_PAY_MSG = "success";
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);

        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(ordernumberR)){
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        log.debug("[积木]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {

        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()) )  //
                continue;
            signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }

        //  删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr+channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[积木]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //pay_status    支付结果	是	1-待支付，2-支付成功，3-支付失败，12-手动确认支付
        String payStatusCode = api_response_params.get(pay_status);
        String responseAmount = api_response_params.get(amt);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[积木]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[积木]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[积木]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[积木]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}