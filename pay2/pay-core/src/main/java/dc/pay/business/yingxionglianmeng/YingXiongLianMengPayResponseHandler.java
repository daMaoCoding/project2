package dc.pay.business.yingxionglianmeng;

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
import dc.pay.utils.RsaUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 15, 2019
 */
@ResponsePayHandler("YINGXIONGLIANMENG")
public final class YingXiongLianMengPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    
    //merchant    int32       是   商户ID
//    private static final String merchant               ="merchant";
    //project int32       是   项目ID
//    private static final String project               ="project";
    //product int32       是   商品ID
//    private static final String product               ="product";
    //morder  string  32  是   商户订单ID
    private static final String morder               ="morder";
    //channel int32       是   物理通道ID
//    private static final String channel               ="channel";
    //paytype int32       是   交易类型ID
//    private static final String paytype               ="paytype";
    //price   int32       可选  交易金额，以分为单位。
    private static final String price               ="price";
    //callback    string  1024    是   商户的支付回调链接
//    private static final String callback               ="callback";
    //status    int32       是   订单状态：0-已失败；1-已创建；2-等待支付；3-已支付；4-已完成；
    private static final String status               ="status";
    //平台订单号
    private static final String order               ="order";

    private static final String info        ="info";
    
//    private static final String key        ="paySecret";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

//    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(API_RESPONSE_PARAMS.get(info));
//        String partnerR = jsonToMap.get(merchant);
        String ordernumberR = jsonToMap.get(morder);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[英雄联盟]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Map<String,String> jsonToMap = JSONObject.parseObject(api_response_params.get(info), Map.class);
        List paramKeys = MapUtils.sortMapByKeyAsc(jsonToMap);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(!signature.equals(paramKeys.get(i)) && (jsonToMap.get(paramKeys.get(i)) instanceof java.lang.String)){
                signSrc.append(paramKeys.get(i)).append("=").append(jsonToMap.get(paramKeys.get(i))).append("&");
            }else if(!signature.equals(paramKeys.get(i))){
                Object string2 = jsonToMap.get(paramKeys.get(i));
                signSrc.append(paramKeys.get(i)).append("=").append(string2+"").append("&");
            }
        }
        signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
        boolean my_result = false;
        //String wpay_public_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCGosEaDEGG9VaZbJ0NOxevFLd9xGEI0/mXcy1EOfHaI0/NZgFbysS0SDf1M1vRCBLXL3dmoiUW8cLWNf0askCtQanxz5kWXXKrGmJpsL5a8dTu6PCl0wD4OB+9B0zCoe/SquACJLBGjsHNGeYS8FmitdYnDjfrTDClimkUUuRthQIDAQAB";
        my_result = RsaUtil.validateSignByPublicKey(info+"={"+paramsStr+"}", channelWrapper.getAPI_PUBLIC_KEY(), api_response_params.get(signature),"SHA256withRSA");  // 验签   signInfo安全付返回的签名参数排序， wpay_public_key安全付公钥， wpaySign安全付返回的签名
        log.debug("[英雄联盟]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(my_result) );
        return String.valueOf(my_result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
//        Map<String, String> jsonToMap = handlerUtil.jsonToMap(API_RESPONSE_PARAMS.get(info));
////      String partnerR = jsonToMap.get(merchant);
//      String ordernumberR = jsonToMap.get(morder);
      
        boolean my_result = false;
        //status    int32       是   订单状态：0-已失败；1-已创建；2-等待支付；3-已支付；4-已完成；
        String payStatusCode = JSON.parseObject(api_response_params.get(info)).getString(status);
        String responseAmount = JSON.parseObject(api_response_params.get(info)).getString(price);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("3")) {
            my_result = true;
        } else {
            log.error("[英雄联盟]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[英雄联盟]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：3");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
//        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        
        Boolean signMd5Boolean = Boolean.valueOf(signMd5);
        log.debug("[英雄联盟]-[响应支付]-4.验证MD5签名：{}", signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        String tmp = "{\"code\":0,\"message\":\"\",\"data\":{\"order\":\""+JSON.parseObject(API_RESPONSE_PARAMS.get(info)).getString(order)+"\",\"sign\":\""+buildPaySign()+"\"}}";
        
        log.debug("[英雄联盟]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", tmp);
        return tmp;
    }
    
    protected String buildPaySign(){
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(API_RESPONSE_PARAMS.get(info));
        StringBuffer signSrc= new StringBuffer();
        List paramKeys = MapUtils.sortMapByKeyAsc(jsonToMap);
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(jsonToMap.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(jsonToMap.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.deleteCharAt(signSrc.length()-1);
        String signInfo = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY(),"SHA256withRSA");   // 签名
        } catch (Exception e) {
            log.error("[英雄联盟]-[响应支付]-5.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        log.debug("[英雄联盟]-[响应支付]-5.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
}