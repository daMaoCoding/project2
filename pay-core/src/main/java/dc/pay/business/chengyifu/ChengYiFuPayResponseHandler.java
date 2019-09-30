package dc.pay.business.chengyifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.yuerongzhuang.EncryptUtil;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@ResponsePayHandler("CHENGYIZHIFU")
public final class ChengYiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "OK";

     private static final String   reqdata      ="reqdata";      // 加密数据

     private static final String   partner      ="partner";     //商户ID      Y  商户id,由支付分配
     private static final String   ordernumber  ="ordernumber"; // 商户订单号  y  上行过程中商户系统传入的p4_orderno
     private static final String   orderstatus  ="orderstatus"; //订单结果     Y  1:支付成功，非1为支付失败
     private static final String   paymoney     ="paymoney";    //订单金额     Y  单位元（人民币）
     private static final String   sign         ="sign";        //MD5签名     N
    // 32位小写MD5签名值 MD5(partner={}&ordernumber={}&orderstatus={}&paymoney={}key)



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
//        String api_key = channelWrapper.getAPI_KEY();
//        System.out.println(api_key);
//        API_RESPONSE_PARAMS = getReqdataMap(API_RESPONSE_PARAMS);
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(ordernumber);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[诚意支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        params = getReqdataMap(params);
        // (partner={}&ordernumber={}&orderstatus={}&paymoney={}key)
        String paramsStr = String.format("partner=%s&ordernumber=%s&orderstatus=%s&paymoney=%s%s",
                params.get(partner),
                params.get(ordernumber),
                params.get(orderstatus),
                params.get(paymoney),
                channelWrapper.getAPI_MEMBERID().split("&")[1]);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[诚意支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        api_response_params = getReqdataMap(api_response_params);
        boolean result = false;
        String payStatus = api_response_params.get(orderstatus);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(paymoney));
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[诚意支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[诚意支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        api_response_params = getReqdataMap(api_response_params);
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[诚意支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[诚意支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    private Map<String ,String > getReqdataMap(Map<String, String> API_RESPONSE_PARAMS) {

        String reqdataStr = API_RESPONSE_PARAMS.get(reqdata);
        Map<String ,String > parse=new HashMap<>();
        if (StringUtils.isBlank(reqdataStr)){
                throw new RuntimeException("[诚意支付]-[响应支付]-1.1 第三方支付返回数据reqdata字段为空");
        }
        try {
            String reqdataPaeams = EncryptUtil.decryptRSAByPrivateKey(channelWrapper.getAPI_KEY(), reqdataStr);
            parse = (Map<String ,String >)JSON.parse(reqdataPaeams);

        }catch (Exception e){
            e.printStackTrace();
        }

        return parse;
    }
}