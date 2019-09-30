package dc.pay.business.changjiangzhifu;

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
 * @author sunny
 * 04 09, 2019
 */
@ResponsePayHandler("CHANGJIANGZHIFU")
public final class ChangJiangZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    字段名				变量名			必填				类型			示例值		描述
//    附加数据			attach			是				string		说明	附加数据，在查询API和支付通知中原样返回，该字段主要用于商户携带订单的自定义数据
//    返回状态码			code			是				int			1	1为成功，其它值为失败
//    支付状态			status			是				string		1	支付状态：'1'为支付成功，'error:错误信息'为未支付成功。
//    支付方式			type			是				string		alipay2	可选的参数是：alipay2（支付宝）、 wechat2（微信）。
//    金额				money			是				string		0.01	
//    订单号				trade_no		是				string		O87f4NTor-Jm4nIMOJTL8yT9D9Sk57ZyD5rnlg_zjTs	在车轮支付系统中的订单号
//    商户订单号			out_trade_no	是				string		1530844815	该订单号在同步或异步地址中原样返回
//    时间				endtime			是				string		2018-01-02 20:20:20	完成交易时间
//    版本号				version			是				string		1	版本号，现在为1
//    商户ID				pid				是				string		10003	
//    签名				sign			是				string		ecb317051cee7103df66b452daca099c	签名算法请看下面示例
//    签名类型			sign_type		是				string		MD5	默认为MD5，不参与签名

    private static final String attach                   ="attach";
    private static final String code                     ="code";
    private static final String status                   ="status";
    private static final String type                	 ="type";
    private static final String money             		 ="money";
    private static final String trade_no                 ="trade_no";
    private static final String out_trade_no             ="out_trade_no";
    private static final String endtime             	 ="endtime";
    private static final String version             	 ="version";
    private static final String pid             	 	 ="pid";
    private static final String sign_type             	 ="sign_type";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(pid);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[长江支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s%s",
      		  attach+"="+api_response_params.get(attach)+"&",
      		  code+"="+api_response_params.get(code)+"&",
      		  endtime+"="+api_response_params.get(endtime)+"&",
      		  money+"="+api_response_params.get(money)+"&",
      		  out_trade_no+"="+api_response_params.get(out_trade_no)+"&",
      		  pid+"="+api_response_params.get(pid)+"&",
      		  status+"="+api_response_params.get(status)+"&",
      		  trade_no+"="+api_response_params.get(trade_no)+"&",
      		  type+"="+api_response_params.get(type)+"&",
      		  version+"="+api_response_params.get(version),
      		  channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[长江支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[长江支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[长江支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[长江支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[长江支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}