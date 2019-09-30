package dc.pay.business.boluomizhifu;

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

import java.util.List;
import java.util.Map;


/**
 * @author Cobby
 * June 22, 2019
 */
@ResponsePayHandler("BOLUOMIZHIFU")
public final class BoLuoMiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String mch_id     = "mch_id";      //: "2xxxxx", #商户号
    private static final String cash       = "cash";        //: 99 #实际收款⾦额
    private static final String status     = "status";      //: 3 #订单状态 (1: 待⽀付, 2:已关闭, 3:已⽀付, 4: 已失效) 正常⽀付成功都是返回3
    private static final String mch_number = "mch_number";  //: "4888999922", #商户订单号
//    private static final String order_number      ="order_number";//: "6XXXXXXX" #平台系统订单号
//    private static final String pay_url           ="pay_url";     //: "http:/xxxxxxx" #url⽀付地址，可封装成⼆维码给客户⽀付或者链接跳转⽀付,详情请看系统demo演示收款⻚⾯
//    private static final String total             ="total";       //: 100 #订单⾦额
//    private static final String body              ="body";        //: "⽀付内容", #下单内容
//    private static final String pay_mode          ="pay_mode";    //: "alipay_bank", #⽀付⽅式
//    private static final String free              ="free";        //: 1 #优惠⾦额
//    private static final String pay_time          ="pay_time";    //: "2019-02-01 23:23:23" #⽀付时间
//    private static final String nonce_str         ="nonce_str";   //: "随机字符", #随机字符

    private static final String key       = "key";
    //signature    数据签名    32    是    　
    private static final String signature = "sign";

    private static final String RESPONSE_PAY_MSG = "{\"code\":200}\n";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(mch_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(mch_number);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[菠萝蜜支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[菠萝蜜支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //3:成功，其他失败
        String payStatusCode  = api_response_params.get(status);
        String responseAmount = api_response_params.get(cash);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        //3代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("3")) {
            my_result = true;
        } else {
            log.error("[菠萝蜜支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[菠萝蜜支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：3");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[菠萝蜜支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[菠萝蜜支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}