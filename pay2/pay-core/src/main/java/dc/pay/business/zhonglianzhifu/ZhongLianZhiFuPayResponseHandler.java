package dc.pay.business.zhonglianzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("ZHONGLIANZHIFU")
public final class ZhongLianZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{    \"code\": \"200\",    \"msg\": \"alwaysSuccess\",    \"data\": \"Success\" }");

    private static final String  company_order_id = "company_order_id";      // "20181231112857376155",
    private static final String  player_id = "player_id";      // "LzAVGR3pzm",
    private static final String  company_id = "company_id";      // "207",
    private static final String  operating_time = "operating_time";      // "1546226938829",
    private static final String  original_amount = "original_amount";      // "100.00",
    private static final String  actual_amount = "actual_amount";      // "100.00",
    private static final String  trade_no = "trade_no";      // "207_20181231112858007482",
    private static final String  api_version = "api_version";      // "1.6",
    private static final String  notify_url = "notify_url";      // "http://66p.nsqmz6812.com:30000/respPayWeb/ZHONGLIANZHIFU_BANK_WEBWAPAPP_ZFB_SM/",
    private static final String  timestamp = "timestamp";      // "1546227498262"
    private static final String  sign = "sign";      // "7B72D7CFC86C714CBC1D8BF7B94E5352",


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(company_order_id);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[众联支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = null;
        pay_md5sign = SignUtil.getSign(params,channelWrapper.getAPI_KEY());
        log.debug("[众联支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));

        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus =  "1";   //api_response_params.get(opstate);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(actual_amount));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {
            checkResult = true;
        } else {
            log.error("[众联支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[众联支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[众联支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[众联支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}