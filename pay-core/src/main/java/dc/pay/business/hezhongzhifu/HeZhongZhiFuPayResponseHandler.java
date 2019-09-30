package dc.pay.business.hezhongzhifu;

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
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author andrew
 * Jul 22, 2019
 */
@ResponsePayHandler("HEZHONGZHIFU")
public final class HeZhongZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称    中文名 数据类型&长度 说明  是否必填
    //v_pagecode  协议包编码   String (4)      是
    private static final String v_pagecode                ="v_pagecode";
    //v_mid       商户编号    String (50)     初始单上所填商户编号为准    是
    private static final String v_mid                ="v_mid";
    //v_oid   订单编号        String (64)     该参数格式为：订单生成    日期-商户编号-商户流水号。例如：20100101-888-12345。商流水号为数字，每日内不可重复，并且不能包括除数字、英文字母和“-”外以其它字符。流水号可为一组也可以用“-”间隔成几组。   是
    private static final String v_oid                ="v_oid";
    //v_orderid   平台订单编号  String (64) 查询时使用   是
    private static final String v_orderid                ="v_orderid";
    //v_bankno    银行编号    String (64)     银行编号    是
//    private static final String v_bankno                ="v_bankno";
    //v_result    交易结果    String (8)  成功：2000，失败：2001 等待支付2002。   是
    private static final String v_result                ="v_result";
    //v_value 提交的金额   String (10) 最多两位小数  是
    private static final String v_value                ="v_value";
    //v_realvalue 实际金额    String (10) 结果金额，以此为准。  是
    private static final String v_realvalue                ="v_realvalue";
    //v_qq    客户的qq   String (16)     是
//    private static final String v_qq                ="v_qq";
    //v_telephone 客户电话    String (20)     是
//    private static final String v_telephone                ="v_telephone";
    //v_goodsname 商品名称    String (20) 中文使用URL-utf8编译  是
//    private static final String v_goodsname                ="v_goodsname";
    //v_goodsdescription  商品描述    String (200)    中文使用URL-utf8编译  是
//    private static final String v_goodsdescription                ="v_goodsdescription";
    //v_extmsg    商品描述    String (200)    中文使用URL-utf8编译  是
//    private static final String v_extmsg                ="v_extmsg";
    //v_resultmsg 结果描述        String (200)    订单结果的描述 是
//    private static final String v_resultmsg                ="v_resultmsg";
    //v_sign  验签字段（SHA1加密）    String (32) v_pagecode=1004&v_mid=商户编号&v_oid=商户订单编号&v_orderid=平台订单编号&v_result=交易结果&v_value=提交的金额+商户SHA密钥    (加密串需要转换成大写)
//    private static final String v_sign                ="v_sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="v_sign";

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("[{result:\"ok\"}]");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[合众支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[合众支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(v_mid);
        String ordernumberR = API_RESPONSE_PARAMS.get(v_oid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[合众支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(v_pagecode+"=").append(api_response_params.get(v_pagecode)).append("&");
        signStr.append(v_mid+"=").append(api_response_params.get(v_mid)).append("&");
        signStr.append(v_oid+"=").append(api_response_params.get(v_oid)).append("&");
        signStr.append(v_orderid+"=").append(api_response_params.get(v_orderid)).append("&");
        signStr.append(v_result+"=").append(api_response_params.get(v_result)).append("&");
        signStr.append(v_value+"=").append(api_response_params.get(v_value));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = Sha1Util.getSha1(paramsStr).toLowerCase();
        log.debug("[合众支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //v_result  交易结果    String (8)  成功：2000，失败：2001 等待支付2002。
        String payStatusCode = api_response_params.get(v_result);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(v_realvalue));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2000")) {
            my_result = true;
        } else {
            log.error("[合众支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[合众支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2000");
        return my_result;
    }
    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[合众支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[合众支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}