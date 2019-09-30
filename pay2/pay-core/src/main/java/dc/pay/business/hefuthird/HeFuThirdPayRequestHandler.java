package dc.pay.business.hefuthird;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 25, 2018
 */
@RequestPayHandler("HEFUTHIRD")
public final class HeFuThirdPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeFuThirdPayRequestHandler.class);

    //参数                  类型                   说明                                                 可否为空
    //merchant_id           Integer                合付分配的商户号。                                   必填
    //order_id              String(30)             商户订单号，必须为唯一订单号。                       必填
    //amount                String(18)             金额支持随机金额 0.01～5000，整数成功率更高。        必填
    //notify_url            String(256)            服务器异步通知地址，用于通知商 户系统处理业务(数据库更新等)。 域名必须为商户后台设置之白名单域名。注意: 该 URL 建议不包含 GET 参数，即形如 ?name=value的内容， 合付支付网关不确保这些 GET 参数通过后台方式向商户反馈时能被保留。        必填
    //return_url            String(256)            用户明文跳转地址，用于告知付款人支付结果。域名必须为商户后台设置之白名单域名。注意:该 URL 建议不包含GET参数，即形如 ?name=value的内容，合付支付网不确保这些GET参数通过后台方式向商户反馈时能被保留。        可空
    //pay_method            Integer                支付方式编号 1:微信直转2:支付宝直转3:云闪付直转4:微信中转5:支付宝中转6:云闪付中转        必填
    //sign                  String(256)            商户传递参数加密值，目前只限定 md5 加密。        必填
    private static final String merchant_id            ="merchant_id";
    private static final String order_id               ="order_id";
    private static final String amount                 ="amount";
    private static final String notify_url             ="notify_url";
//    private static final String return_url             ="return_url";
    private static final String pay_method             ="pay_method";

    private static final String key        ="sign";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_id, channelWrapper.getAPI_MEMBERID());
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_method,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[合付3]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merchant_id+"=").append(api_response_params.get(merchant_id)).append("&");
        signSrc.append(order_id+"=").append(api_response_params.get(order_id)).append("&");
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[合付3]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
//            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[合付3]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[合付3]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            System.out.println("请求返回=========>"+resultStr);
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//               log.error("[合付3]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//               throw new PayException(resultStr);
//            }
//            JSONObject resJson = JSONObject.parseObject(resultStr);
//            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                String code_url = resJson.getString("codeimg");
//                result.put(QRCONTEXT, code_url);
//            }else {
//                log.error("[合付3]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[合付3]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[合付3]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}