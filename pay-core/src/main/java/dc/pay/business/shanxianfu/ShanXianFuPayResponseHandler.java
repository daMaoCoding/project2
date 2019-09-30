package dc.pay.business.shanxianfu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@ResponsePayHandler("SHANXIANFUZHIFU")
public final class ShanXianFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");


     private static final String  data = "data";   // "VovR5yEiSL8fm+QnKd7mpX2Al3S49rsAfSppNbapBczB/jx7y3sk2WF61OfNIs4uL0XttoeGH4lYsV1T0pA2iD8cQOlNq79fYse8cc4pubYtXnl1ByFJd9LQ5YFZjesKIP2uBvQx2WasPeXwKq6iVHSaIty1ANQUtk8Xglc2M91v13XAI1cQHDXsB5DRum5q19nhTZ0StThqLFFtZDzX4WIs238YHOdkSSrZrpeitzJJXLSAWqzdRy1N1n7T8JUvUj4/G6MovA/XwcH83ikbJGgQKwmu526kP7enFJngyuhULB88toHlJsKbEORNlwn5EjXyMC+3FHj/dEHnwTbWoA==",
     private static final String  merchNo = "merchNo";   // "SH201812220107",
     private static final String  orderNum = "orderNum";   // "20181224154312394151"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() || !API_RESPONSE_PARAMS.containsKey(orderNum)|| !API_RESPONSE_PARAMS.containsKey(data))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNum);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[闪现付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        try {
            String dataStr = params.get(data);
            byte[] result = ToolKit.decryptByPrivateKey(new BASE64Decoder().decodeBuffer(dataStr), channelWrapper.getAPI_KEY());
            String resultData = new String(result, ToolKit.CHARSET);// 解密数据
            JSONObject jsonObj = JSONObject.parseObject(resultData);
            Map<String, String> metaSignMap = new TreeMap<String, String>();
            metaSignMap.put("merchNo", jsonObj.getString("merchNo"));
            metaSignMap.put("netwayCode", jsonObj.getString("netwayCode"));
            metaSignMap.put("orderNum", jsonObj.getString("orderNum"));
            metaSignMap.put("amount", jsonObj.getString("amount"));
            metaSignMap.put("goodsName", jsonObj.getString("goodsName"));
            metaSignMap.put("payStateCode", jsonObj.getString("payStateCode"));// 支付状态
            metaSignMap.put("payDate", jsonObj.getString("payDate"));// yyyyMMddHHmmss
            String jsonStr = ToolKit.mapToJson(metaSignMap);
            String signMd5 = ToolKit.MD5(jsonStr.toString() + channelWrapper.getAPI_MEMBERID().split("&")[1], ToolKit.CHARSET);
            if(signMd5.equalsIgnoreCase(jsonObj.getString("sign"))){return "true";}
            return  signMd5;
        } catch (IOException e) {
            new PayException(e.getMessage());
        }

        return "0";
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        try {
            String dataStr = api_response_params.get(data);
            byte[] result = ToolKit.decryptByPrivateKey(new BASE64Decoder().decodeBuffer(dataStr), channelWrapper.getAPI_KEY());
            String resultData = null;// 解密数据
            resultData = new String(result, ToolKit.CHARSET);
            JSONObject jsonObj = JSONObject.parseObject(resultData);

            String payStatus = jsonObj.getString("payStateCode");
            String responseAmount =   jsonObj.getString("amount");
            //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
            boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
            if (checkAmount && payStatus.equalsIgnoreCase("00")) {
                checkResult = true;
            } else {
                log.error("[闪现付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
            }
            log.debug("[闪现付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
            return checkResult;
        } catch (Exception e) {
            throw  new PayException(e.getMessage());
        }

    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        if("true".equalsIgnoreCase(signMd5)) return true;
        return  false;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[闪现付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}