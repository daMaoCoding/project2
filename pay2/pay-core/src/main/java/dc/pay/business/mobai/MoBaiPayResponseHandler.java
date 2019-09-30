package dc.pay.business.mobai;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 8, 2018
 */
@ResponsePayHandler("MOBAI")
public final class MoBaiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名                      变量名               类型                 说明        可空
    //基本参数      
    //版本号                      version               String(3)           当前接口版本号1.0        N
    //商户ID                      partnerid             String(4)           商户在平台的用户ID        N
    //商户订单号                  partnerorderid        String(32)          商户订单号        N
    //订单总金额                  payamount             Int                 单位：分        N
    //订单状态                    orderstatus           Date                订单状态： 0 待支付， 1 已支付，2 支付失败，3提交失败,4已结算(注：4表示已支付，平台已结算)，5 未结算，6 结算失败，7 冻结，8 解冻 9异常        N
    //平台订单号（订单号）        orderno               String(32)          平台订单号        N
    //订单完成时间                okordertime           Date                订单完成时间        N
    //支付类型                    paytype               String(10)          支付类型见8表        N
    //MD5签名                     sign                  String(32)          MD5签名结果        N
    //业务参数
    //订单交易结果说明            message               String(255)         订单交易结果说明（0 支付成功，其他失败）        N
    //商家自定义数据包            remark                String(50)          商户自定义数据包，原样返回，例如：可填写会员ID(验签时为编码状态)        Y
//    private static final String version                                                ="version";
    private static final String partnerid                                              ="partnerid";
    private static final String partnerorderid                                         ="partnerorderid";
    private static final String payamount                                              ="payamount";
    private static final String orderstatus                                            ="orderstatus";
//    private static final String orderno                                                ="orderno";
//    private static final String okordertime                                            ="okordertime";
//    private static final String paytype                                                ="paytype";
//    private static final String sign                                                   ="sign";
//    private static final String message                                                ="message";
//    private static final String remark                                                 ="remark";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        if (!next.contains("{") || !next.contains("}")) {
            log.error("[摩拜]-[响应支付]-1.第三方响应结果：" + JSON.toJSONString(API_RESPONSE_PARAMS) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(API_RESPONSE_PARAMS));
         }
        JSONObject paseObject = null;
        try {
            paseObject = JSON.parseObject(next);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String partnerR = paseObject.getString(partnerid);
        String ordernumberR = paseObject.getString(partnerorderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[摩拜]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        Map<String, String> map = handlerUtil.jsonToMap(iterator.next());
        List paramKeys = MapUtils.sortMapByKeyAsc(map);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(map.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[摩拜]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        boolean my_result = false;
        //订单状态                    orderstatus           Date                订单状态： 0 待支付， 1 已支付，2 支付失败，3提交失败,4已结算(注：4表示已支付，平台已结算)，5 未结算，6 结算失败，7 冻结，8 解冻 9异常        N
        String payStatusCode = parseObject.getString(orderstatus);
        String responseAmount = parseObject.getString(payamount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("4")) {
            my_result = true;
        } else {
            log.error("[摩拜]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[摩拜]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：4");
        return my_result;
    }

     @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject parseObject = JSON.parseObject(next);
        boolean my_result = parseObject.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[摩拜]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[摩拜]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}