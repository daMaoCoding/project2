package dc.pay.business.superpayzhifu2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 01, 2019
 */
@RequestPayHandler("SUPERPAYZHIFU2")
public final class SuperPayZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SuperPayZhiFu2PayRequestHandler.class);

    private static final String uid                ="uid";       //    用户ID       必填    您的唯一标识，注册后在“接口参数配置”页面里获得。一个24位字符串
    private static final String money              ="money";     //    金额         必填    发起付款的金额，单位：元，精确到小数点后两位。
    private static final String channel            ="channel";   //    渠道         必填    支付宝-参数:alipay、微信-参数:wechat、云闪付-参数:unionpay、支付宝转银行卡-参数:alipaybank、支付宝当面付-参数:alipayf2f、支付宝红包-参数:alipaybag、聊天宝-参
    private static final String post_url           ="post_url";  //    通知接口地址  必填    用户支付成功后，Super支付服务器会POST调用这个链接，并携带一系统参数，具体参数请查看付款成功回调接口。
    private static final String return_url         ="return_url";//    跳转地址     必填    用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。
    private static final String order_id           ="order_id";  //    订单号       必填    您这边的订单号，这个参数在通知接口里会回传给您的通知接口地址
//  private static final String order_uid          ="order_uid"; //    用户编号     选填    我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。建议填写。可以填用户名，也可以填您数据库中的用户id。例：xxx, xxx@aaa.com
//  private static final String goods_name         ="goods_name";//    商品名       选填    您的商品名称，用来显示在后台的订单名称。

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(post_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(order_id,channelWrapper.getAPI_ORDER_ID());
//              put(order_uid,channelWrapper.getAPI_ORDER_ID());
//              put(goods_name,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[SuperPay2支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //伪代码示例：md5(uid+auth_code+money+channel+post_url+return_url+order_id+order_uid+goods_name)
        String paramsStr = String.format("%s%s%s%s%s%s%s",
                api_response_params.get(uid),
                channelWrapper.getAPI_KEY(),
                api_response_params.get(money),
                api_response_params.get(channel),
                api_response_params.get(post_url),
                api_response_params.get(return_url),
                api_response_params.get(order_id)/*,
                api_response_params.get(order_uid),
                api_response_params.get(goods_name)*/);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[SuperPay2支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
               if (HandlerUtil.isZFB(channelWrapper)){
                   result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
               }else {
                   String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                   JSONObject jsonObject;
                   try {
                       jsonObject = JSONObject.parseObject(resultStr);
                   } catch (Exception e) {
                       e.printStackTrace();
                       log.error("[SuperPay2支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                       throw new PayException(resultStr);
                   }
                   if (null != jsonObject && jsonObject.containsKey("code") && "200".equalsIgnoreCase(jsonObject.getString("code"))
                           && jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode"))) {
                       String code_url = jsonObject.getString("qrcode");
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                       if (HandlerUtil.isYL(channelWrapper)) {
                           result.put(JUMPURL, code_url);
                       }else{
                           result.put(QRCONTEXT, code_url);
                       }
                   }else {
                       log.error("[SuperPay2支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                       throw new PayException(resultStr);
                   }
               }

        } catch (Exception e) {
            log.error("[SuperPay2支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[SuperPay2支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[SuperPay2支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}