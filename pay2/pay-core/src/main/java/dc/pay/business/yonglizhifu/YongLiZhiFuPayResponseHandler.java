package dc.pay.business.yonglizhifu;

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
 * Jan 9, 2019
 */
@ResponsePayHandler("YONGLIZHIFU")
public final class YongLiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private static final String  src_code        = "src_code";        //是   商户唯一标识
    private static final String  sign            = "sign";            //是   签名
    private static final String  out_trade_no    = "out_trade_no";    //是   接入方交易订单号
    private static final String  total_fee       = "total_fee";       //是   订单总金额
//    private static final String  time_start        = "time_start";      //是   发起交易的时间
//    private static final String  goods_name        = "goods_name";      //是   商品名称
//    private static final String  trade_type        = "trade_type";      //是   交易类型，网关支付：80103
//    private static final String  finish_url        = "finish_url";      //是   支付完成页面的url，有效性根据实际通道而定
    private static final String  mchid           = "mchid";           //是   商户号

//    private static final String  extend            = "extend";          //是   扩展域，此字段是一个json格式，具体参数如下表
//    private static final String  bankName      = "bankName";        //是   银行名称总行名称，值范围：北京农村商业银行, 农业银行, 华夏银行, 交通银行, 广发银行, 邮政储蓄银行, 中国银行, 兴业银行, 中信银行, 招商银行, 银联通道, 光大银行, 建设银行, 平安银行, 浦发银行, 北京银行, 民生银行, 上海银行, 工商银行
//    private static final String  cardType      = "cardType";        //是   卡类型，目前只支持借记卡，取值“借记卡”
    //订单状态 → 1:下单中；2:等待支付；3:支付成功；4:支付失败；6:用户未支付
    private static final String  order_status        = "order_status";
    private static final String  key         = "key";
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String memberId = API_RESPONSE_PARAMS.get(mchid)+"&"+API_RESPONSE_PARAMS.get(src_code);
        String orderId = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[永利支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(!"sign".equals(paramKeys.get(i).toString()) && !StringUtils.isBlank(payParam.get(paramKeys.get(i)))){
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
        }
        sb.append(key+"=" + channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());
        log.debug("[永利支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        //订单状态 → 1:下单中；2:等待支付；3:支付成功；4:支付失败；6:用户未支付
        String payStatusCode = api_response_params.get(order_status);
        String responseAmount = api_response_params.get(total_fee);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("3")) {
            result = true;
        } else {
            log.error("[永利支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[永利支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：3");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[永利支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[永利支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}