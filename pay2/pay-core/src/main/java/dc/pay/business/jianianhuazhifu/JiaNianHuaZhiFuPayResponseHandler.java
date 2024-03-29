package dc.pay.business.jianianhuazhifu;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 7, 2019
 */
@ResponsePayHandler("JIANIANHUAZHIFU")
public final class JiaNianHuaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称    变量名 类型长度    说明
    //状态  status  varchar(10) success:成功，fail失败
    private static final String status                ="status";
    //商户编号    shid    int(8)  订单对应的商户ID
    private static final String shid                ="shid";
    //商户订单号   ddh varchar(20) 商户网站上的订单号
    private static final String ddh                ="ddh";
    //订单金额    je  decimal(18,2)   支付金额
    private static final String je                ="je";
    //支付通道    zftd    varchar(10) 渠道代码
    private static final String zftd                ="zftd";
    //异步通知URL ybtz    varchar(50) POST异步通知
    private static final String ybtz                ="ybtz";
    //同步跳转URL tbtz    varchar(50) GET同步跳转
    private static final String tbtz                ="tbtz";
    //订单名称    ddmc    varchar(50) 支付订单的名称
    private static final String ddmc                ="ddmc";
    //订单备注    ddbz    varchar(50) 支付订单的备注
    private static final String ddbz                ="ddbz";
    //md5签名串  sign    varchar(32) 参照通知MD5签名
//    private static final String sign                ="sign";
    private static final String bb                ="bb";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[嘉年华支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[嘉年华支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(shid);
        String ordernumberR = API_RESPONSE_PARAMS.get(ddh);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[嘉年华支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
        signStr.append(shid+"=").append(api_response_params.get(shid)).append("&");
        signStr.append(bb+"=").append(api_response_params.get(bb)).append("&");
        signStr.append(zftd+"=").append(api_response_params.get(zftd)).append("&");
        signStr.append(ddh+"=").append(api_response_params.get(ddh)).append("&");
        signStr.append(je+"=").append(api_response_params.get(je)).append("&");
        signStr.append(ddmc+"=").append(api_response_params.get(ddmc)).append("&");
        signStr.append(ddbz+"=").append(api_response_params.get(ddbz)).append("&");
        signStr.append(ybtz+"=").append(api_response_params.get(ybtz)).append("&");
        signStr.append(tbtz+"=").append(api_response_params.get(tbtz)).append("&");
        signStr.append(api_key);
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[嘉年华支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //状态    status  varchar(10) success:成功，fail失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(je));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[嘉年华支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[嘉年华支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[嘉年华支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[嘉年华支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}