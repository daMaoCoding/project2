package dc.pay.business.shanrubao;

import java.util.List;
import java.util.Map;

import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

@ResponsePayHandler("SHANRUBAO")
public final class ShanRuBaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private static final String paysapi_id           ="paysapi_id"; //闪入宝生成的订单ID号
    private static final String orderid              ="orderid";    //您的自定义订单号
    private static final String price                ="price";      //订单定价  差额一般在1-2分钱上下
    private static final String realprice            ="realprice";  //实际支付金额
    private static final String orderuid             ="orderuid";   //您的自定义用户ID
    private static final String key                  ="key";        //秘钥

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String orderNum = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(orderNum))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[闪入宝]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,orderNum);
        return orderNum;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//      md5("orderid=".$orderid ."&orderuid=". $orderuid ."&paysapi_id=". $paysapi_id ."&price=". $price ."&realprice=". $realprice ."&token=". $token)
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
	    paramKeys.remove(0);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!"key".equalsIgnoreCase(api_response_params.get(paramKeys.get(i))) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符 &
        signSrc.append("token"+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[闪入宝]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String responseAmount = HandlerUtil.getFen(api_response_params.get(realprice));
//        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount ) { // && payStatusCode.equalsIgnoreCase("1")
            my_result = true;
        } else {
            log.error("[闪入宝]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + 1 + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[闪入宝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + " ,计划成功：1");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(key).equalsIgnoreCase(signMd5);
        log.debug("[闪入宝]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[闪入宝]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}