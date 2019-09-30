package dc.pay.business.xiaotianbaozhifu;

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
 * Jul 30, 2019
 */
@ResponsePayHandler("XIAOTIANBAOZHIFU")
public final class XiaoTianBaoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String data                ="data";
    
    //参数  类型  说明
    //mid string  商户号
    private static final String mid                ="mid";
    //oid string  商户订单号
    private static final String oid                ="oid";
    //amt double  交易金额
    private static final String amt                ="amt";
    //tamt    double  实际交易金额
    private static final String tamt                ="tamt";
    //way string  交易方式 1微信支付， 2支付宝支付， 3微信WAP ，4支付宝WAP
    private static final String way                ="way";
    //code    string  交易结果：100成功，200失败
    private static final String code                ="code";
    //remark  string  可选，后台回调是会返回
//    private static final String remark                ="remark";
    //sign    string  签名，详细见签名方式
//    private static final String sign                ="sign";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    //重要：回调必须按此格式返回：{"errorcode":"200","msg":"成功"}
    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"errorcode\":\"200\",\"msg\":\"成功\"}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[小天宝支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[小天宝支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Map<String,String> jsonToMap = JSONObject.parseObject(API_RESPONSE_PARAMS.get(data), Map.class);
        String partnerR = jsonToMap.get(mid);
        String ordernumberR = jsonToMap.get(oid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[小天宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Map<String,String> jsonToMap = JSONObject.parseObject(api_response_params.get(data), Map.class);
        StringBuilder signStr = new StringBuilder();
        signStr.append(jsonToMap.get(mid)).append("|");
        signStr.append(jsonToMap.get(oid)).append("|");
        signStr.append(jsonToMap.get(amt)).append("|");
        signStr.append(jsonToMap.get(way)).append("|");
        if(jsonToMap.get(jsonToMap.get(code)) instanceof java.lang.String){
            signStr.append(jsonToMap.get(code)).append("|");
        }else{
            Object string2 = jsonToMap.get(code);
            signStr.append(string2+"").append("|");
        }
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[小天宝支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Map<String,String> jsonToMap = JSONObject.parseObject(api_response_params.get(data), Map.class);
        
        boolean my_result = false;
        //code  string  交易结果：100成功，200失败
        String payStatusCode = null;
        if(jsonToMap.get(jsonToMap.get(code)) instanceof java.lang.String){
            payStatusCode = jsonToMap.get(code);
        }else{
            Object string2 = jsonToMap.get(code);
            payStatusCode = string2+"";
        }
        String responseAmount = HandlerUtil.getFen(jsonToMap.get(tamt));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("100")) {
            my_result = true;
        } else {
            log.error("[小天宝支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[小天宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：100");
        return my_result;
    }
    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Map<String,String> jsonToMap = JSONObject.parseObject(api_response_params.get(data), Map.class);
        
        boolean my_result = jsonToMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[小天宝支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[小天宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}