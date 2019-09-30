package dc.pay.business.huifengzhifu;

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
 * Apr 1, 2019
 */
@ResponsePayHandler("HUIFENGZHIFU")
public final class HuiFengZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数             参数含义           必须           说明
    //fxid             商务号               是           唯一号，由汇丰支付提供
    //fxddh            商户订单号           是           平台返回商户提交的订单号
    //fxorder          平台订单号           是           平台内部生成的订单号
    //fxdesc           商品名称             是           utf-8编码
    //fxfee            支付金额             是           支付的价格(单位：元)
    //fxattch          附加信息             是           原样返回，utf-8编码
    //fxstatus         订单状态             是           【1代表支付成功】
    //fxtime           支付时间             是           支付成功时的时间，unix时间戳。
    //fxsign           签名                 是           通过签名算法计算得出的签名值。【md5(订单状态+商务号+商户订单号+支付金额+商户秘钥)】
    private static final String fxid                     ="fxid";
    private static final String fxddh                    ="fxddh";
//    private static final String fxorder                  ="fxorder";
//    private static final String fxdesc                   ="fxdesc";
    private static final String fxfee                    ="fxfee";
//    private static final String fxattch                  ="fxattch";
    private static final String fxstatus                 ="fxstatus";
//    private static final String fxtime                   ="fxtime";

    //signature    数据签名    32    是    　
    private static final String signature  ="fxsign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(fxid);
        String ordernumberR = API_RESPONSE_PARAMS.get(fxddh);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇丰支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(fxstatus)).append("&");
        signStr.append(api_response_params.get(fxid)).append("&");
        signStr.append(api_response_params.get(fxddh)).append("&");
        signStr.append(api_response_params.get(fxfee)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇丰支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //fxstatus         订单状态             是           【1代表支付成功】
        String payStatusCode = api_response_params.get(fxstatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(fxfee));
        
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[汇丰支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[汇丰支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[汇丰支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇丰支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}