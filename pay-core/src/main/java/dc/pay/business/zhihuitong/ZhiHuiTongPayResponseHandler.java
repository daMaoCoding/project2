package dc.pay.business.zhihuitong;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("ZHIHUITONG")
public final class ZhiHuiTongPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "ok";
     private static final String serialVersionUID =   "serialVersionUID" ;     //: "1",
     private static final String logger =   "logger" ;                         //: "Logger[com.payinxl.channel.OrderRechargeNotify]",
     private static final String orderno =   "orderno" ;                       //: "ZHT18040517164601753H",
     private static final String merchantno =   "merchantno" ;                 //: "U18031712421801277S",
     private static final String customno =   "customno" ;                     //: "ZHIHUITONG_ZFB_SM-HemlS",
     private static final String type =   "type" ;                             //: "2",
     private static final String bankcode =   "bankcode" ;                     //: "AP",
     private static final String tjmoney =   "tjmoney" ;                       //: "50.00",
     private static final String money =   "money" ;                           //: "48.25",
     private static final String status =   "status" ;                         //: "1",
     private static final String sign =   "sign" ;                             //: "9217909234960ef73ed13d2c5fa6485f",
     private static final String resultcode =   "resultcode" ;                 //: "1001",
     private static final String resultmsg =   "resultmsg" ;                  //: "productname"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(customno);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[智慧通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // merchantno+“|”+orderno+“|”+customno+“|”+type+ “|”+tjmoney+“|”+money+“|”+status+ “|”+md5key;
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                params.get(merchantno),
                params.get(orderno),
                params.get(customno),
                params.get(type),
                params.get(tjmoney),
                params.get(money),
                params.get(status),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[智慧通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean result = false;
        String payStatus = api_response_params.get(status);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(tjmoney));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[智慧通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[智慧通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[智慧通]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[智慧通]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}