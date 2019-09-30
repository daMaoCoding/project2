package dc.pay.business.futongzhifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("FUTONGZHIFU")
public final class FuTongZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名				类型				说明
//    order_no			String			商户订单号
//    order_amount		String			订单金额（元为单位）
//    order_time		String			UF系统订单创建时间
//    pay_type			String			订单支付通道码
//    product_name		String			支付时上传的商品名称
//    product_code		String			支付时上传的商品CODE
//    user_no			String			支付时上传的用户ID
//    payment			String			支付成功，目前该字段值一定为支付成功四个字。

    private static final String order_no                   		="order_no";
    private static final String order_amount                    ="order_amount";
    private static final String order_time                  	="order_time";
    private static final String pay_type               		 	="pay_type";
    private static final String product_name             		="product_name";
    private static final String product_code                 	="product_code";
    private static final String user_no              			="user_no";
    private static final String payment              			="payment";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("200");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String transdata=HandlerUtil.UrlDecode(API_RESPONSE_PARAMS.get("transdata"));
        JSONObject transdataJson=null;
        try {
        	transdataJson = JSONObject.parseObject(transdata);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(transdata);
        }
        String ordernumberR = transdataJson.getString(order_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[富通支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String transdata=HandlerUtil.UrlDecode(API_RESPONSE_PARAMS.get("transdata"));
    	JSONObject transdataJson=null;
        try {
        	transdataJson = JSONObject.parseObject(transdata);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(transdata);
        }
    	List paramKeys = MapUtils.sortMapByKeyAsc(transdataJson.getInnerMap());
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(transdataJson.getInnerMap().get(paramKeys.get(i)).toString())) {
            	signSrc.append(paramKeys.get(i)).append("=").append(transdataJson.getInnerMap().get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[富通支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	String transdata=HandlerUtil.UrlDecode(API_RESPONSE_PARAMS.get("transdata"));
    	JSONObject transdataJson=null;
        try {
        	transdataJson = JSONObject.parseObject(transdata);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(transdata);
        }
        boolean my_result = false;
        String payStatusCode = transdataJson.getString(payment);
        String responseAmount = HandlerUtil.getFen(transdataJson.getString(order_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("支付成功")) {
            my_result = true;
        } else {
            log.error("[富通支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[富通支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：支付成功");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[富通支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[富通支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}