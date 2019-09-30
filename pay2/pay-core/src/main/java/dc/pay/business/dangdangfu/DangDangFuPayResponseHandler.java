package dc.pay.business.dangdangfu;

import java.io.UnsupportedEncodingException;
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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Oct 27, 2018
 */
@ResponsePayHandler("DANGDANGFU")
public final class DangDangFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名                 字段说明            最大长度      是否必填      备注
    //transDate              交易日期            10                是      
    //transTime              交易时间            8                 是      
    //merchno                商户号              15                是      
    //merchName              商户名称            30                是      
    //customerno             客户号              50                否      
    //amount                 交易金额            12                是         以元为单位
    //traceno                商户流水号          32                是         商家的流水号
    //payType                支付方式            1                 是         1-支付宝  2-微信  4-百度 8-QQ钱包  16-京东 32-银联钱包
    //orderno                系统订单号          12                是         系统订单号,同上面接口的refno。
    //channelOrderno         渠道订单号          32                否         通道交易单号
    //channelTraceno         渠道流水号          50                否         通道交易流水号
    //openId                 用户OpenId          50                否         公众号支付的时候返回
    //status                 交易状态            1                 是         0-未支付 1-支付成功 2-支付失败
    //cust1                  自定义域1           100               否      
    //cust2                  自定义域2           100               否      
    //cust3                  自定义域3           100               否      
    //signature              数据签名            32                是      
//    private static final String transDate                     ="transDate";
//    private static final String transTime                     ="transTime";
    private static final String merchno                       ="merchno";
//    private static final String merchName                     ="merchName";
//    private static final String customerno                    ="customerno";
    private static final String amount                        ="amount";
    private static final String traceno                       ="traceno";
//    private static final String payType                       ="payType";
//    private static final String orderno                       ="orderno";
//    private static final String channelOrderno                ="channelOrderno";
//    private static final String channelTraceno                ="channelTraceno";
//    private static final String openId                        ="openId";
    private static final String status                        ="status";
//    private static final String cust1                         ="cust1";
//    private static final String cust2                         ="cust2";
//    private static final String cust3                         ="cust3";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(traceno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[当当付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"GBK");
        log.debug("[当当付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status                 交易状态            1                 是         0-未支付 1-支付成功 2-支付失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[当当付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[当当付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[当当付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[当当付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    
    
    public static void main(String[] args) {
        
        String tmpStr = "{\"amount\":\"30.00\",\"merchName\":\"������������Ƽ����޹�˾\",\"merchno\":\"654440157220001\",\"orderno\":\"82181020100003436881\",\"payType\":\"1\",\"signature\":\"815C9220A21683644BD5B870301C97D8\",\"status\":\"1\",\"traceno\":\"20181020151458\",\"transDate\":\"2018-10-20\",\"transTime\":\"15:14:58\"}";
        try {
//            tmpStr = new String(tmpStr.getBytes("gb2312"), "GBK");
//            tmpStr = new String(tmpStr.getBytes(), "UTF-8");
            tmpStr = new String(tmpStr.getBytes(), "GBK");
//            tmpStr = new String(tmpStr.getBytes("gb2312"));
            System.out.println("tmpStr====>"+tmpStr);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}