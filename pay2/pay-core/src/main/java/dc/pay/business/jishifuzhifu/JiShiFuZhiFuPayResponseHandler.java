package dc.pay.business.jishifuzhifu;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * @author cobby
 * Jan 21, 2019
 */
@ResponsePayHandler("JISHIFUZHIFU")
public final class JiShiFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

// transDate	交易日期	10	是
// transTime	交易时间	8	是
// merchno	商户号	15	是
// merchName	商户名称	30	是
// customerno	客户号	50	否
// amount	交易金额	12	是	以元为单位
// traceno	商户流水号	32	是	商家的流水号
// payType	支付方式	1	是	1-支付宝 
//2-微信 
//4-百度
//8-QQ钱包 
//16-京东
//32-银联钱包
// orderno	系统订单号	12	是	系统订单号,同上面接口的refno。
// openId	用户OpenId	50	否	公众号支付的时候返回
// status	交易状态	1	是	0-未支付 1-支付成功 2-支付失败
// signature	数据签名	32	是
	private static final String merchno                ="merchno";
    private static final String status                 ="status";
    private static final String amount                 ="amount";
    private static final String traceno                ="traceno";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(traceno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[即时付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {

	    List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[即时付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }


    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;

        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
//        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);

        if (HandlerUtil.isWY(channelWrapper) && checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;// status H5 快捷 交易状态  0-未支付 1-支付成功 2-支付失败
        } else if (HandlerUtil.isYLKJ(channelWrapper) && checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;// status 网银 交易状态  0-未支付 1-支付成功 2-支付失败
        } else if(HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isWEBWAPAPP(channelWrapper) && checkAmount && payStatusCode.equalsIgnoreCase("1")){
	        my_result = true;// status 支付宝 微信 扫码 wap 0-未支付       1-支付成功        2-支付失败
        }else{
	        log.error("[即时付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }

        log.debug("[即时付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功："+payStatusCode);
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[即时付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[即时付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}