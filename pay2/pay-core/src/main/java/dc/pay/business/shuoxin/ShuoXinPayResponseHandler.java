package dc.pay.business.shuoxin;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ResponsePayHandler("SHUOXINZHIFU")
public final class ShuoXinPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String RESPONSE_PAY_MSG = "success";

    private static  final  String code  = "code";   // 1,
    private static  final  String msg  = "msg";   // "交易成功",

    private static  final  String result  = "result";   //
    private static  final  String merchantNo  = "merchantNo";   // "M2018062613055010000236",
    private static  final  String merchantOrderNo  = "merchantOrderNo";   // "20180803152556",
    private static  final  String notifyTime  = "notifyTime";   // "2018-08-03 15:27:06",
    private static  final  String orderAmount  = "orderAmount";   // 1,
    private static  final  String platformOrderNo  = "platformOrderNo";   // "PO20180803152557100083194",
    private static  final  String sign  = "sign";   // "9a40bb8cef88029ee81e68ff4404e07a",
    private static  final  String tradeStatus  = "tradeStatus";   // "PAY_SUCCESS"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        API_RESPONSE_PARAMS = HandlerUtil.oneSizeJsonMapToMap(API_RESPONSE_PARAMS);

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() || !API_RESPONSE_PARAMS.containsKey(code) || !"1".equalsIgnoreCase(String.valueOf(API_RESPONSE_PARAMS.get(code))) || null==API_RESPONSE_PARAMS.get(result))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        API_RESPONSE_PARAMS =  parseMap(API_RESPONSE_PARAMS);
        String ordernumberR = API_RESPONSE_PARAMS.get(merchantOrderNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[烁昕支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        params = HandlerUtil.oneSizeJsonMapToMap(params);
        params =  parseMap(params);
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(String.valueOf(params.get(paramKeys.get(i)))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                continue;

            if(orderAmount.equalsIgnoreCase(paramKeys.get(i).toString())){
                sb.append(String.valueOf(paramKeys.get(i))).append("=").append( HandlerUtil.getYuan(HandlerUtil.getFen(String.valueOf(params.get(paramKeys.get(i))))) ).append("&");
            }else {
                sb.append(String.valueOf(paramKeys.get(i))).append("=").append(String.valueOf(params.get(paramKeys.get(i)))).append("&");
            }


        }
        sb.append(channelWrapper.getAPI_KEY());
        String signStr = sb.toString();//.replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[烁昕支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        api_response_params =   HandlerUtil.oneSizeJsonMapToMap(api_response_params);

        api_response_params =  parseMap(api_response_params);
        boolean checkResult = false;
        String payStatus = api_response_params.get(tradeStatus);


        String responseAmount =  HandlerUtil.getFen(String.valueOf(api_response_params.get(orderAmount)));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("PAY_SUCCESS")) {
            checkResult = true;
        } else {
            log.error("[烁昕支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[烁昕支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：PAY_SUCCESS");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        api_response_params =   HandlerUtil.oneSizeJsonMapToMap(api_response_params);
        api_response_params =  parseMap(api_response_params);
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[烁昕支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[烁昕支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    private synchronized  Map<String,String> parseMap(Map<String, String> API_RESPONSE_PARAMS){
        if(null!=API_RESPONSE_PARAMS && API_RESPONSE_PARAMS.size()>0){
            return JSON.parseObject(String.valueOf(API_RESPONSE_PARAMS.get(result)), Map.class);
        }
        return new HashMap<>();
    }

}