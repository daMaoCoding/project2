package dc.pay.business.xinhongniuzhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;


/**
 * @author Cobby
 * July 01, 2019
 */
@ResponsePayHandler("XINHONGNIUZHIFU")
public final class XinHongNiuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String orderNo     = "orderNo";    //    string    Y        订单编号
    private static final String tradeNo     = "tradeNo";    //    string    Y        Red Bull Pay 交易订单编号
    private static final String price       = "price";      //    string    Y    #.00    交易金额（元）
    private static final String actualPrice = "actualPrice";//    string    Y    #.00    实际交易金额（元）
    private static final String orderTime   = "orderTime";  //    string    Y    yyyyMMddHHmmss    订单时间 (2018-07-17 07:19:25.910)
    private static final String dealTime    = "dealTime";   //    string    Y    yyyyMMddHHmmss    交易完成时间 (2018-07-17 07:19:26.333)
    private static final String orderStatus = "orderStatus";//    integer    Y        订单状态（见订单状态代码表）  当未收到明确的 “成功(1) or 失败(3) 时，皆列入待处理人工判定
    //  private static final String code        = "code";       //    integer    N        錯誤代碼（見错误代码描述）
//  private static final String message     = "message";    //    string    N        錯誤訊息
//      signature    数据签名    32    是    　
    private static final String signature = "signature";

    private static final String RESPONSE_PAY_MSG = "OK";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(tradeNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新红牛支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //actualPrice=435.00&dealTime=、&orderNo=、&orderStatus=1&orderTime=20180717071925&price=435.00&tradeNo=、
        String dealTime1 = strToDateLong(api_response_params.get(dealTime));

        String orderTime1 = strToDateLong(api_response_params.get(orderTime));

        api_response_params.put(dealTime, dealTime1);
        api_response_params.put(orderTime, orderTime1);

        StringBuilder signStr = new StringBuilder();
        signStr.append(actualPrice + "=").append(api_response_params.get(actualPrice)).append("&");
        signStr.append(dealTime + "=").append(api_response_params.get(dealTime)).append("&");
        signStr.append(orderNo + "=").append(api_response_params.get(orderNo)).append("&");
        signStr.append(orderStatus + "=").append(api_response_params.get(orderStatus)).append("&");
        signStr.append(orderTime + "=").append(api_response_params.get(orderTime)).append("&");
        signStr.append(price + "=").append(api_response_params.get(price)).append("&");
        signStr.append(tradeNo + "=").append(api_response_params.get(tradeNo));
        String signMd5 = signStr.toString();
        log.debug("[新红牛支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //1:成功，其他失败
        String payStatusCode  = api_response_params.get(orderStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(price));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[新红牛支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新红牛支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = false;
        try {
            my_result = RSAUtils.checkSignByPubkey(
                    signMd5, Base64.getDecoder().decode(URLDecoder.decode(api_response_params.get(signature), "UTF-8")), channelWrapper.getAPI_PUBLIC_KEY());
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.debug("[新红牛支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新红牛支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }


    public static String strToDateLong(String strDate) {
        SimpleDateFormat formatter  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ParsePosition    pos        = new ParsePosition(0);
        Date             strtodate  = formatter.parse(strDate, pos);
        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMddHHmmss");
        String           dateString = formatter1.format(strtodate);
        return dateString;
    }
}