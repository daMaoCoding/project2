package dc.pay.business.kuaijinfu;

import java.security.MessageDigest;
import java.util.List;
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
import dc.pay.utils.MapUtils;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("KUAIJINFU")
public final class KuaiJinFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数			类型				描述			
//    app_id		字符串			商户应用id，请登录商户后台设置中心->开发者设置里查看	3577154428
//    pay_method	字符串			支付方式，pc_alipay(PC支付宝)、wap_alipay(WAP支付宝)、pc_wechatpay(PC微信)、wap_wechatpay(WAP微信)	pc_alipay
//    trade_no		字符串			平台交易单号	KJ1000201807191007378639
//    order_id		字符串			商户订单号	201807050911453635
//    price			浮点型			订单金额，单位为元，精确到小数点后两位	10.00
//    amount		浮点型			实付金额，单位为元，精确到小数点后两位	9.99
//    subject		字符串			订单标题	测试订单
//    body			字符串			订单描述，没有会留空	这是一个测试订单
//    sign			字符串			签名字符串，签名规则如下	64F7C22310C19A85233DC0C0451C7186

    private static final String app_id                       ="app_id";
    private static final String pay_method                   ="pay_method";
    private static final String trade_no                     ="trade_no";
    private static final String order_id                     ="order_id";
    private static final String price                     	 ="price";
    private static final String amount                       ="amount";
    private static final String subject                      ="subject";
    private static final String body                         ="body";
    
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("200");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(app_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[快金付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	api_response_params.put("token", getMD5ofStr(channelWrapper.getAPI_KEY(),"UTF-8"));
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	paramKeys.remove(signature);
    	StringBuilder signSrc = new StringBuilder();
    	for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))))  //
              continue;
            signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        //signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[快金付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //returncode          交易状态         是            “00” 为成功
        String payStatusCode = "无";
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //实际支付金额  有可能比提交金额少0.01-0.05
        boolean checkAmount =  HandlerUtil.isRightAmount(db_amount,responseAmount,"5");//第三方回调金额差额0.05元内
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[快金付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.info("[快金付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.info("[快金付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.info("[快金付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    
    public static String getMD5ofStr(String str, String encode) {
        try{
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes(encode));
            byte[] digest = md5.digest();

            StringBuffer hexString = new StringBuffer();
            String strTemp;
            for (int i = 0; i < digest.length; i++) {
                // byteVar &
                // 0x000000FF的作用是，如果digest[i]是负数，则会清除前面24个零，正的byte整型不受影响。
                // (...) | 0xFFFFFF00的作用是，如果digest[i]是正数，则置前24位为一，
                // 这样toHexString输出一个小于等于15的byte整型的十六进制时，倒数第二位为零且不会被丢弃，这样可以通过substring方法进行截取最后两位即可。
                strTemp = Integer.toHexString(
                        (digest[i] & 0x000000FF) | 0xFFFFFF00).substring(6);
                hexString.append(strTemp);
            }
            return hexString.toString();
        }catch(Exception e){
            e.printStackTrace();
            return "";
        }

    }
}