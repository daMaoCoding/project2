package dc.pay.business.yijiazhifu0;

import java.util.List;
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
import dc.pay.utils.MapUtils;

/**
 * 
 * @author andrew
 * Aug 10, 2019
 */
@ResponsePayHandler("YIJIAZHIFU0")
public final class YiJiaZhiFu0PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称    参数含义    数据类型    参数说明
    //typeCode    业务编码    char    详见：业务编码
//    private static final String typeCode                ="typeCode";
    //err_code    错误码 char    详见：错误码
//    private static final String err_code                ="err_code";
    //err_msg 错误信息    char    详见：错误码中错误描述
//    private static final String err_msg                ="err_msg";
    //merchantCode    商户号 char    商户编号
    private static final String merchantCode                ="merchantCode";
    //orderNo 商户订单号   int 商户唯一订单号
    private static final String orderNo                ="orderNo";
    //amount  订单金额    char    分为单位,整数,金额校检,以支付金额为准。
//    private static final String amount                ="amount";
    //terraceNo   平台订单号   char    平台唯一订单号
//    private static final String terraceNo                ="terraceNo";
    //orderTime   订单时间    char    原样返回
//    private static final String orderTime                ="orderTime";
    //payAmount   支付金额    char    分为单位,整数,以支付金额为准
    private static final String payAmount                ="payAmount";
    //payTime 支付时间    char    详见：签名加密明细
//    private static final String payTime                ="payTime";
    //reserver    订单保留信息  char    原样返回
//    private static final String reserver                ="reserver";
    //status  订单状态    char    SUCCESS：成功 FAIL:失败
    private static final String status                ="status";
    //signature   签名  char    详见：签名加密明细
//    private static final String signature                ="signature";
    
//    private static final String paycorerespData        ="pay-core-respData";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[易嘉支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[易嘉支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String my_data = API_RESPONSE_PARAMS.get(paycorerespData);
////        Map<String, String> jsonToMap = handlerUtil.getUrlParams("?"+string.replace(":", "="));
//        Map<String, String> jsonToMap = getUrlParams("?"+string.replace(":", "="));
//        Map<String, Object> jsonToMap = StringJSONUtil.paramsParse(my_data);
//        if (null == jsonToMap || jsonToMap.isEmpty())
//            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantCode)+"";
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo)+"";
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[易嘉支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//        String my_data = api_response_params.get(paycorerespData);
//        Map<String, Object> jsonToMap = StringJSONUtil.paramsParse(my_data);

        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i))+"")) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
//        System.out.println("签名源串=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[易嘉支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
//        String my_data = api_response_params.get(paycorerespData);
//        Map<String, Object> jsonToMap = StringJSONUtil.paramsParse(my_data);
      
        boolean my_result = false;
        //status    订单状态    char    SUCCESS：成功 FAIL:失败
        String payStatusCode = api_response_params.get(status)+"";
        String responseAmount = api_response_params.get(payAmount)+"";

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[易嘉支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[易嘉支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
//        String my_data = api_response_params.get(paycorerespData);
//        Map<String, Object> jsonToMap = StringJSONUtil.paramsParse(my_data);
      ;
        
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[易嘉支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[易嘉支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    
    /**
     * 解析出url参数中的键值对
     * 如 "index.jsp?Action=del&id=123"，解析出Action:del,id:123存入map中
     * @param URL  url地址
     * @return  url请求参数部分
     */
//    public static Map<String, String> getUrlParams(String URL)
//    {
//        Map<String, String> mapRequest = new HashMap<String, String>();
//        String[] arrSplit=null;
//        String strUrlParam=truncateUrlPage(URL);
//        if(strUrlParam==null) {
//            return mapRequest;
//        }
//        //每个键值为一组 www.2cto.com
//        arrSplit=strUrlParam.split("[&]");
//        for(String strSplit:arrSplit) {
//            String[] arrSplitEqual=null;
//            arrSplitEqual= strSplit.split("[=]");
//            //解析出键值
//            if(arrSplitEqual.length>1){
//                //正确解析
//                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);
//            }
//            else{
//                if(arrSplitEqual[0]!=""){
//                    //只有参数没有值，不加入
//                    mapRequest.put(arrSplitEqual[0], "");
//                }
//            }
//        }
//        return mapRequest;
//    }
    
    /**
     * 去掉url中的路径，留下请求参数部分
     */
//    private static String truncateUrlPage(String strURL)
//    {
//        String strAllParam=null;
//        String[] arrSplit=null;
////        strURL=strURL.trim().toLowerCase();
//        strURL=strURL.trim();
//        arrSplit=strURL.split("[?]");
//        if(strURL.length()>1){
//            if(arrSplit.length>1){
//                if(arrSplit[1]!=null){
//                    strAllParam=arrSplit[1];
//                }
//            }
//        }
//        return strAllParam;
//    }
}