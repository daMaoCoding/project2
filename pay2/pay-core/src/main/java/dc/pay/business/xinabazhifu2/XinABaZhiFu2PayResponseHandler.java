package dc.pay.business.xinabazhifu2;

import java.util.Base64;
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
import dc.pay.utils.HandlerUtil;

/**
 * 
 * @author andrew
 * Sep 9, 2019
 */
@ResponsePayHandler("XINABAZHIFU2")
public final class XinABaZhiFu2PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //下列数据项为所有业务上送数据的公共数据项
    //上送数据公共字段
    //参数名称 中文名称 是否必填 参与签名 参数说明
    //merchantNo 商户号 是 是 平台分配的商户号
    private static final String merchantNo                ="merchantNo";
    //businessType 业务类型标识 是 是 order: 订单类型, query: 订单状态查询,
    private static final String businessType                ="businessType";
    //timeStamp 时间戳 是 是 系统当前时间戳(长整型数值串)
//    private static final String timeStamp                ="timeStamp";
    //ipAddr 客户端 IP 地址 是 是 请填写真实下单客户 ip 地址
    private static final String ipAddr                ="ipAddr";
    //data 业务数据封装 是 是 将业务数据转为 json 字符串后. 再次使用 base64 编码. 存入此字段中
    private static final String data                ="data";
    //sign 数据签名 是 否 见本手册加解密章节
//    private static final String sign                ="sign";
    
    //异步回调需要客户端返回小写 success 字符视为成功. 否则每隔 10 秒调用一次. 重复 20 次
    //异步回调参数
    //参数名称 中文名称 参与签名 参数说明
    //merchant_no 商户号 是 平台分配的商户号
    private static final String merchant_no                ="merchant_no";
    //order_no 商户订单号 是 商户自己在创建订单时发送的订单号
    private static final String order_no                ="order_no";
    //platform_order_no 平台订单号 是 在创建订单成功后平台生成的订单号
//    private static final String platform_order_no                ="platform_order_no";
    //order_money 订单金额(分) 是 订单金额采用分为计算单位. 不带小数点的正整数. 例如 1 元. 需要转换为 100 分
//    private static final String order_money                ="order_money";
    //pay_time 支付时间 是 订单在完成支付的时间. 格式为 yyyyMMddHHmmss
//    private static final String pay_time                ="pay_time";
    //pay_money 实际支付金额 是 订单金额采用分为计算单位. 不带小数点的正整数. 例如 1 元. 需要转换为 100 分
    private static final String pay_money                ="pay_money";
    //order_state 订单状态 是 订单支付状态: 82001: 待支付, 82002: 已支付, 82003:订单异常
    private static final String order_state                ="order_state";
    //extend 扩展字段 否 客户端上送原样返回
//    private static final String extend                ="extend";
    private static final String order_money                ="order_money";
    private static final String pay_time                ="pay_time";
    private static final String platform_order_no                ="platform_order_no";
    //sign md5 签名字符串 否 见加密解密章节
    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[新A8支付2]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[新A8支付2]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_no);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新A8支付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Map<String, String> jsonToMap = new TreeMap<String, String>(api_response_params);
        jsonToMap.remove(sign);
        
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merchant_no+"=").append(api_response_params.get(merchant_no)).append("&");
        signSrc.append(order_money+"=").append(api_response_params.get(order_money)).append("&");
        signSrc.append(order_no+"=").append(api_response_params.get(order_no)).append("&");
        signSrc.append(order_state+"=").append(api_response_params.get(order_state)).append("&");
        signSrc.append(pay_money+"=").append(api_response_params.get(pay_money)).append("&");
        signSrc.append(pay_time+"=").append(api_response_params.get(pay_time)).append("&");
        signSrc.append(platform_order_no+"=").append(api_response_params.get(platform_order_no)).append("&");
        
//        signSrc.append(timeStamp+"=").append(api_response_params.get(extend).split("&")[1]).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新A8支付2]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //order_state 订单状态 是 订单支付状态: 82001: 待支付, 82002: 已支付, 82003:订单异常
        String payStatusCode = api_response_params.get(order_state);
        String responseAmount = api_response_params.get(pay_money);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("82002")) {
            my_result = true;
        } else {
            log.error("[新A8支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新A8支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：82002");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新A8支付2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新A8支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}