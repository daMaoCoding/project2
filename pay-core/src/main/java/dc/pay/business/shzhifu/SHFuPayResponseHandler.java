package dc.pay.business.shzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
@ResponsePayHandler("SHZHIFU")
public final class SHFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

     private static final String  reqData = "reqData";    // "reqData",
     private static final String  body = "body";    // "20181222145133498132",
     private static final String  mch_id = "mch_id";    // "9cuifan92543928",
     private static final String  order_no = "order_no";    // "20181222145216985705261",
     private static final String  out_trade_no = "out_trade_no";    // "20181222145133498132",
     private static final String  pay_type = "pay_type";    // "41",
     private static final String  ret_code = "ret_code";    // "00",
     private static final String  sign = "sign";    // "DDD4031711904F7A5F61C165FB357B57",
     private static final String  store_id = "store_id";    // "9cuifan",
     private static final String  trans_amt = "trans_amt";    // "2.00"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String reqDataJsonStr =API_RESPONSE_PARAMS.get(reqData);
        JSONObject reqDataJsonObj = JSONObject.parseObject(reqDataJsonStr);
        String ordernumberR = reqDataJsonObj.getString(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[SH支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params111, String api_key) throws PayException {
        String reqDataJsonStr =API_RESPONSE_PARAMS.get(reqData);
        JSONObject reqDataJsonObj = JSONObject.parseObject(reqDataJsonStr);
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(reqDataJsonObj);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(reqDataJsonObj.getString(paramKeys.get(i).toString())) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(reqDataJsonObj.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[SH支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params_b, String amountDb) throws PayException {


        String reqDataJsonStr =API_RESPONSE_PARAMS.get(reqData);
        JSONObject reqDataJsonObj = JSONObject.parseObject(reqDataJsonStr);


        boolean checkResult = false;
        String payStatus = reqDataJsonObj.getString(ret_code);
        String responseAmount =  HandlerUtil.getFen(reqDataJsonObj.getString(trans_amt));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("00")) {
            checkResult = true;
        } else {
            log.error("[SH支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[SH支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params11, String signMd5) {
        String reqDataJsonStr =API_RESPONSE_PARAMS.get(reqData);
        JSONObject reqDataJsonObj = JSONObject.parseObject(reqDataJsonStr);
        boolean result = reqDataJsonObj.getString(sign).equalsIgnoreCase(signMd5);
        log.debug("[SH支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[SH支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}