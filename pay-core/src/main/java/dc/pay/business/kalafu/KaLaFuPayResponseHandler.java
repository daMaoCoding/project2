package dc.pay.business.kalafu;

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
@ResponsePayHandler("KALAFU")
public final class KaLaFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

     private static final String   bb = "bb";   // "1.0",
     private static final String   ddbz = "ddbz";   // "20181219154029259127",
     private static final String   ddh = "ddh";   // "20181219154029259127",
     private static final String   ddmc = "ddmc";   // "20181219154029259127",
     private static final String   je = "je";   // "20.000",
     private static final String   shid = "shid";   // "10000370",
     private static final String   sign = "sign";   // "9627dd21803282578f891fb4ce12e46e",
     private static final String   status = "status";   // "success",
     private static final String   tbtz = "tbtz" ;   //http://www.baidu.com",
     private static final String   ybtz = "ybtz" ;   //http://66p.nsqmz6812.com:30000/respPayWeb/KALAFU_BANK_WAP_ZFB_SM/",
     private static final String   zftd = "zftd";   // "alapi"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(ddh);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[卡拉付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // opstate=opstate值&orderid=orderid值&ovalue=ovalue值&parter=商家接口调用ID&key=商家
        String paramsStr = String.format("status=%s&shid=%s&bb=%s&zftd=%s&ddh=%s&je=%s&ddmc=%s&ddbz=%s&ybtz=%s&tbtz=%s&%s",
                params.get(status),
                params.get(shid),
                params.get(bb),
                params.get(zftd),
                params.get(ddh),
                params.get(je),
                params.get(ddmc),
                params.get(ddbz),
                params.get(ybtz),
                params.get(tbtz),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[卡拉付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(status);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(je));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("success")) {
            checkResult = true;
        } else {
            log.error("[卡拉付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[卡拉付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[卡拉付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[卡拉付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}