package dc.pay.business.lantian;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 * Nov 22, 2018
 */
@ResponsePayHandler("LANTIAN")
public final class LanTianPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数              必填          类型               含义                      说明
    //out_trade_no       是           String(50)         业务平台唯一订单号          业务平台自己的订单号，用于校验，由于未响应情况下，系统会重复请求回调: 请务必对此订单号做校验处理，避免重复执行支付成功业务，导致多给用户充值等操作 
    //money              是           float              订单金额                    订单支付金额 
    //trade_status       是           int                订单支付状态                0-未支付，1-已支付 
    //name               是           String(50)         商品名称                    根据统一下单接口提交返回 
    //type               是           String(50)         支付方式                    wechat=微信，alipay=支付宝 
    //id                 是           id                 支付系统中唯一订单id        用于后续查单使用 
    //sign_type          是           String(32)         加密方式                    不参与加密排序，默认为 MD5 
    //sign               是           String(32)         签名字符串                  不参与加密排序，参考 签名算法 
    private static final String out_trade_no             ="out_trade_no";
    private static final String money                    ="money";
    private static final String trade_status             ="trade_status";
//    private static final String name                     ="name";
//    private static final String type                     ="type";
//    private static final String id                       ="id";
    private static final String sign_type                ="sign_type";
//    private static final String sign                     ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"code\":200,\"msg\":\"已收到回调\"}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(out_trade_no);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[蓝天]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //这里将map.entrySet()转换成list
        List<Map.Entry<String,String>> list = new ArrayList<Map.Entry<String,String>>(api_response_params.entrySet());
        //然后通过比较器来实现排序
        Collections.sort(list,new Comparator<Map.Entry<String,String>>() {
            //升序排序
            public int compare(Entry<String, String> o1,
                    Entry<String, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        StringBuilder signSrc = new StringBuilder();
        for(Map.Entry<String,String> mapping:list){ 
            if (!sign_type.equals(mapping.getKey()) && !signature.equals(mapping.getKey()) && StringUtils.isNotBlank(api_response_params.get(mapping.getKey()))) {
//                signSrc.append(mapping.getKey()).append("=").append(mapping.getValue()).append("&");
                signSrc.append(mapping.getValue());
            }
        }
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[蓝天]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //trade_status       是           int                订单支付状态                0-未支付，1-已支付 
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[蓝天]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[蓝天]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[蓝天]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[蓝天]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}