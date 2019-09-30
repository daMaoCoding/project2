package dc.pay.business.daqian;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 7, 2018
 */
@ResponsePayHandler("DAQIAN")
public final class DaQianPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //order_trano_in         商户订单号         String(32)         商户订单号 
    //order_number           平台单号           String(32)         平台单号，支付平台生成 
    //order_pay              支付类型           Int                支付类型：1.支付宝 2.微信 3.财付通 4.银行 5.快捷支付 
    //order_goods            商品名称           String(32)         商品名称 
    //order_price            商品单价           String(32)         商品单价，单位分 
    //order_num              商品数量           Int(32)            商品数量 
    //order_amount           订单金额           String(32)         订单金额，单位分 
    //order_extend           扩展参数           String(64)         扩展参数，最大长度 64 位 
    //order_imsi             设备imsi           String(32)         设备 imsi 
    //order_mac              设备mac            String(32)         设备 mac 
    //order_brand            设备品牌           String(32)         设备品牌 
    //order_version          设备版本           String(32)         设备版本 
    //order_time             成功时间           String(32)         成功时间 
    //order_state            支付状态           String(32)         支付状态:0.待支付 1.支付成功 
    //signature              数据签名           String(32)         数据签名，验证签名时按照 key 字母顺序排序，空值和 signature 不纳入签名计算，末尾拼接应用KEY，将得到的最终字符串进行 MD5（32 位）加密 注：空值不参与排序和加密，不传
    private static final String order_trano_in                 ="order_trano_in";
//    private static final String order_number                   ="order_number";
//    private static final String order_pay                      ="order_pay";
//    private static final String order_goods                    ="order_goods";
//    private static final String order_price                    ="order_price";
//    private static final String order_num                      ="order_num";
    private static final String order_amount                   ="order_amount";
    private static final String order_extend                   ="order_extend";
//    private static final String order_imsi                     ="order_imsi";
//    private static final String order_mac                      ="order_mac";
//    private static final String order_brand                    ="order_brand";
//    private static final String order_version                  ="order_version";
//    private static final String order_time                     ="order_time";
    private static final String order_state                    ="order_state";

    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(order_extend);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_trano_in);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[大千]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	TreeMap<String, String> map = new TreeMap<String, String>();
		for (Map.Entry<String, String> entry : api_response_params.entrySet()) {
			if (!entry.getKey().equals(signature)) {
				map.put(entry.getKey(), entry.getValue().toString());
			}
		}
		//空值不参与排序
		String signStr = SignHelper.sortSign(map) + api_key;
        String signMd5 = SignHelper.MD5(signStr);
        log.debug("[大千]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //order_state            支付状态           String(32)         支付状态:0.待支付 1.支付成功 
        String payStatusCode = api_response_params.get(order_state);
        String responseAmount = api_response_params.get(order_amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[大千]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[大千]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[大千]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[大千]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}