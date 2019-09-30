package dc.pay.business.yqing;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 18, 2019
 */
@ResponsePayHandler("YQING")
public final class YQingPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //1   org_number  接入机构编码  YES YES String  由系统分配
    private static final String org_number               ="org_number";
    //2   merchant_number 交易商户编号  YES YES String  由系统分配
    private static final String merchant_number               ="merchant_number";
    //3   payType 接口服务名称  YES YES String  由系统分配
    private static final String payType               ="payType";
    //4   data    请求报文    YES YES String  请求报文，采用AES加密密文，十六进制，全部大写
    private static final String data               ="data";
    
    //序号  参数名 参数说明    签名  必填  数据类型    说明
    //1   out_trade_no    订单号 -   -   String  
    private static final String out_trade_no                       ="out_trade_no";
    //2   amount  订单金额    -   -   Number  
    private static final String amount                       ="amount";
    //3   realAmount  支付金额    -   -   Number  
//    private static final String realAmount                       ="realAmount";
    //4   orderStatus 订单状态            Number  1=成功
    private static final String orderStatus                       ="orderStatus";
    //5   callback_time   回调时间            Time    
//    private static final String callback_time                       ="callback_time";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);

        String ApiKey = handlerUtil.getApiKeyFromReqPayMemberId(API_RESPONSE_PARAMS.get(merchant_number));
//        String ApiKey = "a7c586116de4bc03740722d72ca7b603";
        
        Map<String,String> jsonToMap = null;
        try {
            // 解密
            String mapStr = new String(AesUtil.decrypt2(AesUtil.parseHexStr2Byte(API_RESPONSE_PARAMS.get(data)), ApiKey.substring(0, 16)));
            jsonToMap = JSONObject.parseObject(mapStr, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[yqing]-[响应支付]-1.0.1.获取支付通道响应信息出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        
        String ordernumberR = jsonToMap.get(out_trade_no);
//      if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
       if (StringUtils.isBlank(ordernumberR))
          throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
       
        log.debug("[yqing]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(org_number));
        signStr.append(api_response_params.get( merchant_number));
        signStr.append(api_response_params.get( payType));
        signStr.append(api_response_params.get( data));
        signStr.append(api_key);
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[yqing]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        
        String mapStr = new String(AesUtil.decrypt2(AesUtil.parseHexStr2Byte(API_RESPONSE_PARAMS.get(data)), channelWrapper.getAPI_KEY().substring(0, 16)));
        Map<String,String> jsonToMap = JSONObject.parseObject(mapStr, Map.class);
        
        boolean my_result = false;
        //4 orderStatus 订单状态            Number  1=成功
        Object orderStatusStr = jsonToMap.get(orderStatus);
        String payStatusCode = orderStatusStr+"";
        
        Object realAmountStr = jsonToMap.get(amount);
        String responseAmount = realAmountStr+"";
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[yqing]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[yqing]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[yqing]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[yqing]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}