package dc.pay.business.qianlimazhifu;

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
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@ResponsePayHandler("QIANLIMAZHIFU")
public final class QianLiMaZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


     private static final String  amount = "amount";   // "0.01",
     private static final String  complete_date = "complete_date";   // "2018-11-08 14:02:02",
     private static final String  order_no = "order_no";   // "20181108140105682831",
     private static final String  platform_order = "platform_order";   // "qlm15416568970236884",
     private static final String  sign = "sign";   // "2779cc7ae6793786a192c305fccd985c",
     private static final String  status = "status";   // "8"
     private static final String  extra = "extra";   // "8"
     private static final String  realmoney = "realmoney";   // 实际支付金额

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[千里马支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())
                    ||extra.equalsIgnoreCase(paramKeys.get(i).toString())
                    ||platform_order.equalsIgnoreCase(paramKeys.get(i).toString())
                    ||realmoney.equalsIgnoreCase(paramKeys.get(i).toString())
                    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append( HandlerUtil.UrlEncode(params.get(paramKeys.get(i)))   ).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = channelWrapper.getAPI_KEY().concat(sb.toString().replaceFirst("&key=",""));



        String signMd5 = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[千里马支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(status);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(realmoney));  //更改为实际支付金额
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("8")) {
            checkResult = true;
        } else {
            log.error("[千里马支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[千里马支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[千里马支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[千里马支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}