package dc.pay.business.liying3zhifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;

/**
 * 
 * @author andrew
 * Jan 26, 2019
 */
@ResponsePayHandler("LIYING3ZHIFU")
public final class LiYing3ZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

//     private static final String  mch_id  = "mch_id";                  //: "ceshi03",
//     private static final String  nonce_str  = "nonce_str";            //: "1521165485704p87oe16",
     private static final String  out_trade_no  = "out_trade_no";      //: "LIYINGZHIFU_QQ_SM-KhupY",
//     private static final String  time_end  = "time_end";            //: "20180316095805",
     private static final String  total_fee  = "total_fee";          //: "1000",  总金额，以分为单位
//     private static final String  trade_no  = "trade_no";            //: "180316095720774099731",
     private static final String  trade_state  = "trade_state";      //: "SUCCESS",SUCCESS—支付成功
//     private static final String  trade_type  = "trade_type";       //: "05",
     private static final String  sign  = "sign";                  //: "32A8C157F54D0ED5BB68887E0B1496C8"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[利盈3支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // partner={}&ordernumber={}&orderstatus={}&paymoney={}key
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if( sign.equalsIgnoreCase(paramKeys.get(i).toString())  )  //StringUtils.isBlank(params.get(paramKeys.get(i))) ||
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key="+channelWrapper.getAPI_KEY());//"key="+
        String signMd5 = HandlerUtil.getMD5UpperCase(sb.toString());
        log.debug("[利盈3支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return  signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean result = false;
        String payStatus = api_response_params.get(trade_state);
        String responseAmount =  api_response_params.get(total_fee);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(amountDb,responseAmount,"100");//我平台默认允许一元偏差

//        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("SUCCESS")) {
            result = true;
        } else {
            log.error("[利盈3支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[利盈3支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：SUCCESS");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[利盈3支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[利盈3支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}