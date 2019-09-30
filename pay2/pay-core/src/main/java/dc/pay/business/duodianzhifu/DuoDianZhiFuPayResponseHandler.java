package dc.pay.business.duodianzhifu;

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
import dc.pay.utils.Sha1Util;

/**
 * @author sunny
 * @date 10 Jul 2019
 */
@ResponsePayHandler("DUODIANZHIFU")
public final class DuoDianZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数			类型			可空			说明
//    pid			String		N			商户ID
//    amount		float		N			金额,单位元，范围（1~2000）
//    trade_no		String		N			订单号
//    order_source	String		N			订单来源,推荐设置为区别用户的标识，比如设置为用户的ID，方便在商户在后台按用户筛选订单
//    pay_type		String		N			支付方式,可用值: alih5 #支付宝H5 wxh5 #微信H5
//    return_url	String		N			服务器同步返回地址
//    notify_url	String		N				服务器异步通知地址
//    sign			String		N			32位小写MD5签名值，参数字典序排列 md5(amount={}&notify_url={}&order_source={}&pay_type={}&pid={}

    private static final String trade_no                  	  ="trade_no";
    private static final String ori_amount                    ="ori_amount";
    private static final String trade_status                  ="trade_status";
    private static final String amount                  	  ="amount";
    private static final String order_source                  ="order_source";
    private static final String pay_type                      ="pay_type";
    private static final String pay_amount                    ="pay_amount";
    private static final String pay_time                      ="pay_time";
    private static final String uuid                  		  ="uuid";
    private static final String apikey                        ="apikey";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[多点支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s", 
      		  amount+"="+  api_response_params.get(amount)+"&",
      		  order_source+"="+  api_response_params.get(order_source)+"&",
      		  ori_amount+"="+  api_response_params.get(ori_amount)+"&",
      		  pay_amount+"="+  api_response_params.get(pay_amount)+"&",
      		  pay_time+"="+  api_response_params.get(pay_time)+"&",
      		  pay_type+"="+  api_response_params.get(pay_type)+"&",
      		  trade_no+"="+  api_response_params.get(trade_no)+"&",
      		  trade_status+"="+  api_response_params.get(trade_status)+"&",
      		  uuid+"="+  api_response_params.get(uuid)+"&",
      		  apikey+"="+  channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[多点支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount =HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[多点支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[多点支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[多点支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[多点支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}