package dc.pay.business.gaoshengzhifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

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
@ResponsePayHandler("GAOSHENGZHIFU")
public final class GaoShengZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名				参数含义			长度				描述			必填
//    appId				商户号			16				是
//    payType			支付网关			16				是
//    outTradeNo		订单号			32				是
//    totalAmount		交易金额			14				（单位：分）	是
//    goodsInfo			商品名称			20				是
//    resultCode		支付状态			16				00表示成功，99表示失败	是
//    payDate			支付时间			19				格式：yyyy-MM-dd HH:mm:ss	是
//    sign				交易签名			32				（字母大写）	是
    
    private static final String appId                   	  ="appId";
    private static final String payType                    	  ="payType";
    private static final String outTradeNo                    ="outTradeNo";
    private static final String totalAmount                   ="totalAmount";
    private static final String goodsInfo             		  ="goodsInfo";
    private static final String resultCode                    ="resultCode";
    private static final String payDate              		  ="payDate";
    private static final String reqData              		  ="reqData";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resJson=getResultJson(API_RESPONSE_PARAMS);
        String ordernumberR = resJson.getString(outTradeNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[高盛支付]-[响应支付]-1.2获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	JSONObject resJson=getResultJson(api_response_params);
    	Map<String, String> resultMap = HandlerUtil.jsonToMap(resJson.toJSONString());
    	List paramKeys = MapUtils.sortMapByKeyAsc(resultMap);
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        JSONObject resResultJson=new JSONObject();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(resultMap.get(paramKeys.get(i)))) {
            	resResultJson.put(paramKeys.get(i).toString(), resultMap.get(paramKeys.get(i)));
            }
        }
        String paramJson=JSONObject.toJSONString(resResultJson,SerializerFeature.SortField.MapSortField);
        String paramsStr = paramJson+channelWrapper.getAPI_KEY();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[高盛支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	JSONObject resJson=getResultJson(api_response_params);
        boolean my_result = false;
        String payStatusCode = resJson.getString(resultCode);
        String responseAmount = resJson.getString(totalAmount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[高盛支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[高盛支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：10000");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	JSONObject resJson=getResultJson(api_response_params);
        boolean my_result = resJson.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[高盛支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[高盛支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
    private JSONObject getResultJson(Map<String, String> API_RESPONSE_PARAMS){
    	String reqDatas = API_RESPONSE_PARAMS.get(reqData);
        JSONObject resJson=null;
        try {
            resJson = JSONObject.parseObject(reqDatas);
            return resJson;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[高盛支付]-[响应支付]-发送支付请求，及获取支付请求结果：" + JSON.toJSONString(reqDatas) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        }
        return resJson;
    }
}