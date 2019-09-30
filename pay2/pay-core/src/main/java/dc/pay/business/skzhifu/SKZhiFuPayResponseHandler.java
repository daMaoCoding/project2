package dc.pay.business.skzhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author Cobby
 * June 29, 2019
 */
@ResponsePayHandler("SKZHIFU")
public final class SKZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String orderNo    = "orderNo";    //    订单号    String    我方订单唯一标识符
    private static final String outOrderNo = "outOrderNo"; //    外部订单号    String    商户订单唯一标识符
    private static final String money      = "money";      //    金额    Integer    订单金额
    private static final String payType    = "payType";    //    订单类型    String    订单类型
    private static final String status     = "status";     //    订单状态    String    订单状态 SUCCESS
    private static final String attach     = "attach";     //    订单附加参数    String    商户自定义数据
    private static final String timestamp  = "timestamp";  //    时间戳    Long    异步响应的时间戳
    private static final String nonceStr   = "nonceStr";   //    业务流水号    String    10位以上的业务流水随机字符串

    //signature    数据签名    32    是    　
    private static final String signature = "signature";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(orderNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(outOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[SK支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[SK支付]-[响应支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[响应支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        
        Map<String, String> params = new HashMap<>();
        params.put(outOrderNo, api_response_params.get(outOrderNo));
        params.put("amount", api_response_params.get(money));
        params.put(payType, api_response_params.get(payType));
        params.put(attach, api_response_params.get(attach));

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys); //排序处理

        StringBuilder requestUrl = new StringBuilder("?");
        for (String key : keys) {
            requestUrl.append(key).append("=");
            try {
                requestUrl.append(URLEncoder.encode(params.get(key), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                requestUrl.append(params.get(key));
            }
            requestUrl.append("&");
        }

        String requestParamsEncode = requestUrl.replace(requestUrl.lastIndexOf("&"), requestUrl.length(), "").toString();
        String md5Value = HandlerUtil.getMD5UpperCase(requestParamsEncode + channelWrapper.getAPI_MEMBERID().split("&")[0] + api_response_params.get(timestamp) + api_response_params.get(nonceStr)).toLowerCase();
        String signMd5  = HandlerUtil.getMD5UpperCase(md5Value + channelWrapper.getAPI_KEY()).toUpperCase();
        log.debug("[SK支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
        String payStatusCode  = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[SK支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[SK支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[SK支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[SK支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}