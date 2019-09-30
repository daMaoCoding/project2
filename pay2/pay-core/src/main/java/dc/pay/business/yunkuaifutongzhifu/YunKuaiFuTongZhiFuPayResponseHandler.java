package dc.pay.business.yunkuaifutongzhifu;

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
 * 
 * @author andrew
 * Aug 13, 2019
 */
@ResponsePayHandler("YUNKUAIFUTONGZHIFU")
public final class YunKuaiFuTongZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //{
    //    "status": 10000,
    private static final String status                ="status";
    //    "data": {
    private static final String data                ="data";
    //        "PayMoney": 100,
    private static final String PayMoney                ="PayMoney";
    //        "RealMoney": 100,
    private static final String RealMoney                ="RealMoney";
    //        "SettleMoney": 998,
    private static final String SettleMoney                ="SettleMoney";
    //        "OrderNo": "T118012218372668417",
    private static final String OrderNo                ="OrderNo";
    //        "AgentOrder": "T120180122143044402",
    private static final String AgentOrder                ="AgentOrder";
    //        "Sign": "ssdfdsfdsfdsfdsf"
//    private static final String Sign                ="Sign";
    //    },
    //    "msg": "成功"
//    private static final String msg                ="msg";
    //}
    
    private static final String Key        ="Key";
    //signature    数据签名    32    是    　
    private static final String signature  ="Sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[云快付通支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[云快付通支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//      Map<String, String> jsonToMap = handlerUtil.jsonToMap(API_RESPONSE_PARAMS.get(data));
        JSONObject parseObject = JSON.parseObject(API_RESPONSE_PARAMS.get(data));
        if (null == parseObject || parseObject.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = parseObject.getString(AgentOrder);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[云快付通支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        JSONObject parseObject = JSON.parseObject(api_response_params.get(data));
        StringBuilder signStr = new StringBuilder();
        signStr.append(PayMoney+"=").append(parseObject.getString(PayMoney)).append("&");
        signStr.append(SettleMoney+"=").append(parseObject.getString(SettleMoney)).append("&");
        signStr.append(OrderNo+"=").append(parseObject.getString(OrderNo)).append("&");
        signStr.append(AgentOrder+"=").append(parseObject.getString(AgentOrder)).append("&");
        signStr.append(RealMoney+"=").append(parseObject.getString(RealMoney)).append("&");
        signStr.append(Key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[云快付通支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject parseObject = JSON.parseObject(api_response_params.get(data));
        
        boolean my_result = false;
        //状态10000为成功状态，其他为错误.data值就是具体数据
        String payStatusCode = api_response_params.get(status);
        String responseAmount =parseObject.getString(RealMoney);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("10000")) {
            my_result = true;
        } else {
            log.error("[云快付通支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[云快付通支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：10000");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject parseObject = JSON.parseObject(api_response_params.get(data));
        
        boolean my_result = parseObject.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[云快付通支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[云快付通支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}