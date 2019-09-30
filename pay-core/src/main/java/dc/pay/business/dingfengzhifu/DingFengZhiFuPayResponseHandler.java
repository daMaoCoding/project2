package dc.pay.business.dingfengzhifu;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 26, 2019
 */
@ResponsePayHandler("DINGFENGZHIFU")
public final class DingFengZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名 类型 说明
    //noise string(16-32) 随机串,噪⾳元素
    private static final String noise                ="noise";
    //notify_id string 每次异步通知的唯⼀标识
    private static final String notify_id                ="notify_id";
    //notify_time string(15) 异步通知发起请求时的 Unix毫秒时间戳
    private static final String notify_time                ="notify_time";
    //signature string(32) 参数签名值,详⻅签名算法
//    private static final String signature                ="signature";

    //参数名 类型 说明
    //sequence string 商户创建⽀付订单时携带的内部订单编号
    private static final String sequence                 ="sequence";
    //order_id string ⽀付系统的唯⼀订单编号
    private static final String order_id                 ="order_id";
    //origin_amount string(decimal(10,2)) 订单原始⾦额
    private static final String origin_amount                 ="origin_amount";
    //payed_amount string( decimal(10,2)) 实际⽀付⾦额
    private static final String payed_amount                 ="payed_amount";
    //platform int 选择的⽀付平台,以下值取其⼀: 1: alipay, 2: weixin
    private static final String platform                 ="platform";
    //up_order_id string 上游订单号,⽀付宝或微信的订单号
    private static final String up_order_id                 ="up_order_id";
    //up_create_time string 上游订单创建时间,⽀付宝或微信的订单号
    private static final String up_create_time                 ="up_create_time";
    //pay_status string ⽀付状态 0:未⽀付 1:⽀付 2:部分⽀付
    private static final String pay_status                 ="pay_status";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"code\": 200,\"message\": \"\",\"data\": {\"noise\": \"qpok8yuhjnhgytgd\",\"notify_id\": \"181218880000320792\",},\"signature\": \"b33be145911c707f190a8a70798da208\"}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[鼎峰支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[鼎峰支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = null;
        try {
            parseObject = JSON.parseObject(next);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String ordernumberR = parseObject.getString(sequence);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[鼎峰支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        StringBuilder signStr = new StringBuilder();
        signStr.append(noise+"=").append(parseObject.getString(noise)).append("&");
        signStr.append(notify_id+"=").append(parseObject.getString(notify_id)).append("&");
        signStr.append(notify_time+"=").append(parseObject.getString(notify_time)).append("&");
        signStr.append(order_id+"=").append(parseObject.getString(order_id)).append("&");
        signStr.append(origin_amount+"=").append(parseObject.getString(origin_amount)).append("&");
        signStr.append(pay_status+"=").append(parseObject.getString(pay_status)).append("&");
        signStr.append(payed_amount+"=").append(parseObject.getString(payed_amount)).append("&");
        signStr.append(platform+"=").append(parseObject.getString(platform)).append("&");
        signStr.append(sequence+"=").append(parseObject.getString(sequence)).append("&");
        signStr.append(up_create_time+"=").append(parseObject.getString(up_create_time)).append("&");
        signStr.append(up_order_id+"=").append(parseObject.getString(up_order_id)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[鼎峰支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        boolean my_result = false;
        //paystatus string ⽀付状态 0:未⽀付 1:⽀付 2:部分⽀付
        String payStatusCode = parseObject.getString(pay_status);
        String responseAmount = HandlerUtil.getFen(parseObject.getString(payed_amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[鼎峰支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[鼎峰支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        boolean my_result = parseObject.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[鼎峰支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        
        StringBuilder signStr = new StringBuilder();
        signStr.append(noise+"=").append(parseObject.getString(noise)).append("&");
        signStr.append(notify_id+"=").append(parseObject.getString(notify_id)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = null;
        try {
            signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        } catch (PayException e) {
            e.printStackTrace();
            
        }
        String str = "{\"code\": 200,\"message\": \"\",\"data\": {\"noise\": \""+parseObject.getString(noise)+"\",\"notify_id\": \""+parseObject.getString(notify_id)+"\",},\"signature\": \""+signMd5+"\"}";
        log.debug("[鼎峰支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", jsonResponsePayMsg(str));
        return jsonResponsePayMsg(str);
    }
}