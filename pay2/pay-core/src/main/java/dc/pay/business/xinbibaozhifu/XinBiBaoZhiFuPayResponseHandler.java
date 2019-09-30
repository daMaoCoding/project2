package dc.pay.business.xinbibaozhifu;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 23, 2019
 */
@ResponsePayHandler("XINBIBAOZHIFU")
public final class XinBiBaoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //注意事项：参数名称严格区分大小写
    // 请求说明：
    //参数名 必需 类型 说明
    //UserName Y String 用户名称
//    private static final String UserName                ="UserName";
    //OrderId Y String OTC 订单号
//    private static final String OrderId                ="OrderId";
    //OrderNum Y String 商户平台订单号
    private static final String OrderNum                ="OrderNum";
    //Type Y Int 订单类型 1-买，2-卖
//    private static final String Type                ="Type";
    //Coin Y String 币种
//    private static final String Coin                ="Coin";
    //CoinAmount Y String 交易虚拟币金额
    private static final String CoinAmount                ="CoinAmount";
    //LegalAmount Y String 交易法币金
    private static final String LegalAmount                ="LegalAmount";
    //State1 Y Int 订单状态 0-初始创建 1-收款方未收款 2-收款方已收款（即订单完成）3-被投诉关闭 4-订单关闭 9-验证失败而关闭 10-等待审核
    private static final String State1                ="State1";
    //State2 Y Int 支付状态 1-未支付 2-已支付
    private static final String State2                ="State2";
    //CreateTime Y String 交易创建时间
//    private static final String CreCateTime                ="CreateTime";
    //Remark Y String 备注，UTF-8 转码。
//    private static final String Remark                ="Remark";
    //Price Y String 价格
//    private static final String Price                ="Price";
    //Token Y String 登录 tok
//    private static final String Token                ="Token";
    //Sign Y String 签名方式参照 5.5
    private static final String Sign                ="Sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="Sign";

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"Success\":true,\"Code\":1,\"Message\":\"xx\"}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(OrderNum );
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新币宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!Sign.equals(paramKeys.get(i)) && !"FinishTime".equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
//        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
//        signSrc.append(channelWrapper.getAPI_KEY());
        signSrc.append(channelWrapper.getAPI_MEMBERID().split("&")[1]);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
//        String signMd5 = new MD5().GetMD5Code(paramsStr).toLowerCase();
        log.debug("[新币宝支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
//        Zer0
//        state1和state2 都要看，都等于2 才是支付成功
        //State1 Y Int 订单状态 0-初始创建 1-收款方未收款 2-收款方已        收款（即订单完成）3-被投诉关闭 4-订单关闭 9-验证        失败而关闭 10-等待审核
        //State2 Y Int 支付状态 1-未支付 2-已
        String payStatusCode1 = api_response_params.get(State1);
        String payStatusCode2 = api_response_params.get(State2);

        //String responseAmount = HandlerUtil.getFen(api_response_params.get(LegalAmount));

        //这个请求金额字段与回调金额字段 ，最大偏差范围你边是不能确定的了吧？
        //我知道，你不知道 2019/3/12 10:31:36
        //或者说，你们有没有这样的字段：请求金额字段A与回调金额字段B，能保证A=B的。有这样的条件的话我们能也使用。
        //10:33:12
        //有的，amount=CoinAmount 
        String responseAmount = HandlerUtil.getFen(api_response_params.get(CoinAmount));
//        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //2代表第三方支付成功
        if (checkAmount && payStatusCode1.equalsIgnoreCase("2") && payStatusCode2.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[新币宝支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode1 +","+payStatusCode2+ " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新币宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode1 +","+payStatusCode2+ " ,计划成功：state1和state2 都要看，都等于2 才是支付成功");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新币宝支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新币宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}