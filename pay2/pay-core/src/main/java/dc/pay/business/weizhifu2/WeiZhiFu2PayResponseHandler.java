package dc.pay.business.weizhifu2;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

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
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * Dec 27, 2018
 */
@Slf4j
@ResponsePayHandler("WEIZHIFU2")
public final class WeiZhiFu2PayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

      private static final String  totalAmount = "totalAmount";      // -> "1.0000"
     private static final String  merchantTradeNo = "merchantTradeNo";      // -> "20181017110903"
     private static final String  sign = "sign";      // -> "292481DFAF51596D3BBD3B421854AD45"
//     private static final String  SystemTradeNo = "SystemTradeNo";      // -> "MQ1539745743839AJEBL"
     private static final String  orderStatus = "orderStatus";      // -> "0"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Map<String, String> resmap = HandlerUtil.oneSizeJsonUrlEncodeMapToMap(API_RESPONSE_PARAMS);
        String ordernumberR = resmap.get(merchantTradeNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[薇支付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> paramsA, String api_key) throws PayException {
        Map<String, String> resmap = HandlerUtil.oneSizeJsonUrlEncodeMapToMap(paramsA);
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(resmap);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resmap.size(); i++) {
            if(StringUtils.isBlank(resmap.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(resmap.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[薇支付2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        Map<String, String> resmap = HandlerUtil.oneSizeJsonUrlEncodeMapToMap(api_response_params);
        boolean checkResult = false;
        String payStatus = resmap.get(orderStatus);
        String responseAmount =  HandlerUtil.getFen(resmap.get(totalAmount));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("0")) {
            checkResult = true;
        } else {
            log.error("[薇支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[薇支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;


    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Map<String, String> resmap = HandlerUtil.oneSizeJsonUrlEncodeMapToMap(api_response_params);
        boolean result = resmap.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[薇支付2]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[薇支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}