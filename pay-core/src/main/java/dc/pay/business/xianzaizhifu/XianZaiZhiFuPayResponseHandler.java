package dc.pay.business.xianzaizhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.RsaUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@ResponsePayHandler("XIANZAIZHIFU")
public final class XianZaiZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";

     private static final String  opstate = "opstate";      //: "1", 交易状态，0:未支付,1:已支付
     private static final String  orderid = "orderid";      //: "GEFU_WX_SMT-Py53Y", 商家订单号
     private static final String  ovalue = "ovalue";      //: "1.00", 提交金额跟回调的金额
     private static final String  parter = "parter";      //: "fe927f7e97e838fd692f78d8f8bb5d3f", 接口调用ID


     private static final String  opay_status = "opay_status"; // "0",
     private static final String  order_type = "order_type"; // "2706",
     private static final String  out_trade_no = "out_trade_no"; // "20180912131028302583121",
     private static final String  pay_status = "pay_status"; // "1",
     private static final String  return_code = "return_code"; // "SUCCESS",
     private static final String  return_msg = "return_msg"; // "order_notify",
     private static final String  seller_id = "seller_id"; // "302583121",
     private static final String  sign = "sign"; // "XVbKBZWTGeEmF+yZqhEU9H4Y0HNTTNEXkOC8y/kKephnHm5X4GVaC2OehuQiC9/wG3JyRMSod2bBANZOlrVC7OhnkKZWnmeaZ6csoL+0g/QYM24/jGmHS85NDsNXiJ/yTbqMsWnewWlhyq8vc2SeyIN+214QWGX8Oc5aKdriYaasnxpxofNZIpAPFCyhoua9xERzIpapeQFeSivRYCPTYXPtXIUTcnWUj0mNNbjFeSWShepg4Aio3kmEfuEYfDR6pEasWQ9dGoF7rCRGRIQd/NxWlTU0XZgZgjcM0bGGEZu3swmhHA42VKUNKhM3zn5oIDbE24tgf8GBQwHtyHdKsw==",
     private static final String  state = "state"; // "00",
     private static final String  total_fee = "total_fee"; // 100


    public Map<String,String> parseRespData(Map<String, String> API_RESPONSE_PARAMS){
        if(null!=API_RESPONSE_PARAMS && API_RESPONSE_PARAMS.containsKey("pay-core-respData")){
            try {
                JSONObject jsonObject = JSON.parseObject(new String(new BASE64Decoder().decodeBuffer(API_RESPONSE_PARAMS.get("pay-core-respData"))));
                return    JSONObject.toJavaObject(jsonObject, Map.class);
            } catch (IOException e) {
              return Maps.newHashMap();
            }
        }
        return Maps.newHashMap();
    }


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Map<String, String>   parseRespData = parseRespData(API_RESPONSE_PARAMS);
        String ordernumberR = parseRespData.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[现在支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // opstate=opstate值&orderid=orderid值&ovalue=ovalue值&parter=商家接口调用ID&key=商家
        String paramsStr = "false";
        Map<String, String>   parseRespData = parseRespData(params);
        ArrayList<String> list = new ArrayList<String>();
        for (Map.Entry<String , ?> entry : parseRespData.entrySet()) {
            if (entry.getValue() != "" && entry.getKey() != sign) {
                list.add(entry.getKey() + "=" + entry.getValue() + "&");
            }
        }
        int size = list.size();
        String[] arrayToSort = list.toArray(new String[size]);
        Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(arrayToSort[i]);
        }
        sb = sb.deleteCharAt(sb.length() - 1);
        try {
            paramsStr=String.valueOf( RsaUtil.validateSignByPublicKey(sb.toString(), channelWrapper.getAPI_PUBLIC_KEY(),parseRespData.get(sign)));
        } catch (Exception e) {
            throw new PayException("[现在支付]密钥错误。");
        }
        log.debug("[现在支付]-[请求支付]-2.生成加密URL签名完成：" + paramsStr);
        return paramsStr;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        Map<String, ?>   parseRespData = parseRespData(api_response_params);
        boolean checkResult = false;
        String payStatus = String.valueOf(parseRespData.get(pay_status));
        String state_S =  String.valueOf(parseRespData.get(state));
        String return_code_S =  String.valueOf(parseRespData.get(return_code));
        String responseAmount =    String.valueOf(parseRespData.get(total_fee));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("1") && return_code_S.equalsIgnoreCase("SUCCESS") &&state_S.equalsIgnoreCase("00")) {
            checkResult = true;
        } else {
            log.error("[现在支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[现在支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        log.debug("[现在支付]-[响应支付]-4.验证MD5签名：" + signMd5);
        return Boolean.valueOf(signMd5);
    }

    @Override
    protected String responseSuccess() {
        log.debug("[现在支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}