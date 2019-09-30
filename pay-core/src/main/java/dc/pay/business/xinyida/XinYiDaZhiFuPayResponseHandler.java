package dc.pay.business.xinyida;

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
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("XINYIDAZHIFU")
public final class XinYiDaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    字段名				变量名			必填				是否签名			类型			示例值			描述
//    支付订单号			payOrderId		是				是				String(30)	P20160427210604000490	支付中心生成的订单号
//    商户ID				mchId			是				是				String(30)		20001222	支付中心分配的商户号
//    应用ID				appId			是				是				String(32)		0ae8be35ff634e2abe94f5f32f6d5c4f	该商户创建的应用对应的ID
//    支付产品ID			productId		是				是				int	8001	支付产品ID
//    商户订单号			mchOrderNo		是				是				String(30)	20160427210604000490	商户生成的订单号
//    支付金额			amount			是				是				nt	100	支付金额,单位分
//    订单金额			orderAmount（已取消）				否				否				int	100	订单金额，单位分
//    入账金额			income			是				是				int	100	入账金额,单位分
//    状态				status			是				是				int	1	支付状态,0-订单生成,1-支付中,2-支付成功,3-业务处理完成
//    渠道订单号			channelOrderNo	否				否				String(64)	wx2016081611532915ae15beab0167893571	三方支付渠道订单号
//    渠道数据包			channelAttach	否				否				String	{“bank_type”:”CMB_DEBIT”,”trade_type”:”pay.weixin.micropay” }	支付渠道数据包
//    扩展参数1			param1			否				否				String(64)		支付中心回调时会原样返回
//    扩展参数2			param2			否				否				String(64)		支付中心回调时会原样返回
//    支付成功时间		paySuccTime		是				是				long		精确到毫秒
//    通知类型			backType		是				是				int	1	通知类型，1-前台通知，2-后台通知
//    签名				sign			是				是				String(32)	C380BEC2BFD727A4B6845133519F3AD6	签名值，详见签名算法

    private static final String payOrderId                   ="payOrderId";
    private static final String mchId                    	 ="mchId";
    private static final String appId                  		 ="appId";
    private static final String productId                	 ="productId";
    private static final String mchOrderNo             		 ="mchOrderNo";
    private static final String amount                 		 ="amount";
    private static final String income              		 ="income";
    private static final String status              		 ="status";
    private static final String channelOrderNo               ="channelOrderNo";
    private static final String channelAttach                ="channelAttach";
    private static final String param1                		 ="param1";
    private static final String param2                		 ="param2";
    private static final String paySuccTime                  ="paySuccTime";
    private static final String backType                  	 ="backType";
    private static final String orderAmount           		 ="orderAmount";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mchId);
        String ordernumberR = API_RESPONSE_PARAMS.get(mchOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新亿达支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新亿达支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[新亿达支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新亿达支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新亿达支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新亿达支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}