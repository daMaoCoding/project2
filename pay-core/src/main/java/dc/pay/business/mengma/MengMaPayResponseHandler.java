package dc.pay.business.mengma;

import java.net.URLDecoder;
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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 11, 2018
 */
@ResponsePayHandler("MENGMA")
public final class MengMaPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //回调报头
    //字段 ⻓长度             可否为空        注释              
    //input_charset            10             否              编码格式:UTF-8          
    //sign_type                3              否              签名⽅方式:SHA1WITHRSA          
    //request_time             20             可              YYMMDDHHmmss          
    //content                  1024           否              业务参数待解密密⽂文
    //out_trade_no             64             否              商户平台订单号          
    //status                   3              否              订单状态：1.交易易完成；2.交易易失败
    //sign                     256            否              待验签字符串串          
    //业务参数
    //字段                  长度             可否为空              注释
    //trade_id                64              否              平台提供的交易易流⽔水号          
    //out_trad_no             40              否              原始商户订单          
    //amount_str              40              否              ⾦金金额          
    //amount_fee              40              否              ⼿手续费          
    //status                  40              否              状态：0.处理理中；1.已完成；2.失败          
    //for_trade_id            40              可              对应交易易流⽔水号(还款)
    //business_type           2               否              业务类型:0.代付;1.代扣;2.充值;3.退款;4.取现;5.移动快捷⽀支付;6.移动          快捷退款;7.⽹网关⽀支付;8.微信⽀支付;9.退票;10.调账;11.⽀支付宝;99.未知          
    //create_time             40              否              创建时间          
    //modiﬁed_time            40              可              交易易时间          
    //remark                  256             可              备注单
//    private static final String input_charset                             ="input_charset";
//    private static final String sign_type                                 ="sign_type";
//    private static final String request_time                              ="request_time";
    private static final String content                                   ="content";
    private static final String out_trade_no                              ="out_trade_no";
//    private static final String status                                    ="status";
//    private static final String sign                                      ="sign";
//    private static final String trade_id                                  ="trade_id";
//    private static final String out_trad_no                               ="out_trad_no";
    private static final String amount_str                                ="amount_str";
//    private static final String amount_fee                                ="amount_fee";
    private static final String status                                    ="status";
//    private static final String for_trade_id                              ="for_trade_id";
//    private static final String business_type                             ="business_type";
//    private static final String create_time                               ="create_time";
//    private static final String modiﬁed_time                              ="modiﬁed_time";
//    private static final String remark                                    ="remark";
    
//    private static final String key        ="verfication_code";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() || StringUtils.isBlank(API_RESPONSE_PARAMS.get(content)))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[猛犸]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        boolean my_result = false;
        //1.获取回调通知中 content 密文
        String contentEncode = api_response_params.get(content);
        try {
            //2.通过 URLDecoder 进行密文转义，获取RSA密文的 BASE64格式
            String rsaContent = URLDecoder.decode(contentEncode,"UTF-8");
            //3.获取content 明文
            String my_content= RSAUtil.decrypt(rsaContent,api_key.split("-")[1],"UTF-8");
            //4.获取回调中sign签名密文
            String signEncode = api_response_params.get(signature);
            //5.通过 URLDecoder 进行密文转义 获取RSA密文的 BASE64格式
            String sign = URLDecoder.decode(signEncode,"utf-8");
            //6.调用签名验证方法  返回true 则验证通过   false为验证不通过
            my_result = RSAUtil.verify(my_content,sign,channelWrapper.getAPI_PUBLIC_KEY(),"utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        log.debug("[猛犸]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(my_result) );
        return String.valueOf(my_result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        //1.获取回调通知中 content 密文
        String contentEncode = api_response_params.get(content);
        //3.获取content 明文
        String my_content = null;
        try {
            //2.通过 URLDecoder 进行密文转义，获取RSA密文的 BASE64格式
            String rsaContent = URLDecoder.decode(contentEncode,"UTF-8");
            my_content = RSAUtil.decrypt(rsaContent,channelWrapper.getAPI_KEY().split("-")[1],"UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        Map<String,String> contentMap = JSONObject.parseObject(my_content, Map.class);
        boolean my_result = false;
        //status    3   否   订单状态：1.交易完成；2.交易失败
        Object my_status = contentMap.get(status);
        String payStatusCode = my_status+"";
        Object my_amount_str = contentMap.get(amount_str);
        String responseAmount = HandlerUtil.getFen(my_amount_str+"");
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[猛犸]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[猛犸]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

     @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
         Boolean my_result = new Boolean(signMd5);
//        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[猛犸]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[猛犸]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}
