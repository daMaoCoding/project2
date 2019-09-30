package dc.pay.business.epay;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 24, 2019
 */
@ResponsePayHandler("E-PAY")
public final class EPayPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //异步通知参数
    //序号  域名  变量名 数据格式    出现要求    域说明
    //基本信息                    
    //1   交易类型编号  transTypeNo AN1..10 M   详见附录
    private static final String transTypeNo                        ="transTypeNo";
    //2   签名  signature   ANS1..1024  M   MD5签名
    private static final String signature                        ="signature";
    //商户信息                    
    //3   商户代码    merchantNum AN15    M   已被批准加入平台的商户代码
    private static final String merchantNum                        ="merchantNum";
    //订单信息                    
    //4   订单号 orderId AN8..40 M   商户订单号，不应含“-”或“_
    private static final String orderId                        ="orderId";
    //5   交易金额    txnAmt  N1..12  M   单位为分，不能带小数点，样例：1元送100
    private static final String txnAmt                        ="txnAmt";
    //6   订单发送时间  txnTime YYYYMMDDHHmmss  M   必须使用当前北京时间（年年年年月月日日时时分分秒秒）24小时制，样例：20151123152540，北京时间 商户发送交易时间
    private static final String txnTime                        ="txnTime";
    //7   请求方自定义域 reqReserved ANS1..1024  O   商户自定义保留域，交易应答时会原样返回
//    private static final String reqReserved                        ="reqReserved";
    //通知信息                    
    //8   流水单号    queryId AN8..40 M   平台流水号
//    private static final String queryId                        ="queryId";
    //9   应答码 respCode    AN2 M   
    private static final String respCode                        ="respCode";
    //10  应答信息    respMsg ANS1..256   M   
//    private static final String respMsg                        ="respMsg";

//    private static final String key        ="key";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantNum);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[E-PAY]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(transTypeNo));
        signSrc.append(api_response_params.get(merchantNum));
        signSrc.append(api_response_params.get(orderId));
        signSrc.append(api_response_params.get(txnTime));
        signSrc.append(api_key);
        signSrc.append(api_response_params.get(txnAmt));
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[E-PAY]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //应答码（respCode）        00  支付完成
        String payStatusCode = api_response_params.get(respCode);
        String responseAmount = api_response_params.get(txnAmt);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[E-PAY]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[E-PAY]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[E-PAY]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[E-PAY]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}