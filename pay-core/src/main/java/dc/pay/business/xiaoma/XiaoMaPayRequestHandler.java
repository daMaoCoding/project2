package dc.pay.business.xiaoma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 1, 2018
 */
@RequestPayHandler("XIAOMA")
public final class XiaoMaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XiaoMaPayRequestHandler.class);

  //参数名称               中文说明                     是否必填          描述          
    //customer_id            商户号                        是               平台下发的商户编号          
    //out_trade_no           商户订单编号                  是               客户方生成的订单编号，不能重复          
    //attach                 附加参数                      是               成功回调是原样返回          
    //notify_url             后台回调地址                  是               支付成功后异步回调地址          
    //return_url             前端返回地址                  是               支付完成时，跳转地址          
    //pay_type               支付类型示例：alipay          是               alipay          :          支付宝          wechatpay          ：微信          qqpay          :          QQ          gatewaypay          :          网关支付          quickpay          :          快捷支付          wechatgzhpay          ：          微信公众号支付
    //request_type           请求类型示例：1               是               1：扫码          2：WEB/H5          3：网关/快捷/公众号          
    //total_fee              支付金额                      是               支付金额，单位：分          
    //subject                商品简单描述                  是               商品简单描述          
    //detail                 商品详细描述                  是               商品详细描述          
    //body                   交易说明                      是               wechatpay          时必填          
    //sign                   签名                          是               根据规则加签以后的结果
    private static final String customer_id              ="customer_id";
    private static final String out_trade_no             ="out_trade_no";
    private static final String attach                   ="attach";
    private static final String notify_url               ="notify_url";
    private static final String return_url               ="return_url";
    private static final String pay_type                 ="pay_type";
    private static final String request_type             ="request_type";
    private static final String total_fee                ="total_fee";
    private static final String subject                  ="subject";
    private static final String detail                   ="detail";
    private static final String body                     ="body";
//    private static final String sign                     ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(customer_id, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(attach, channelWrapper.getAPI_MEMBERID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                //请求类型示例：1               是               1：扫码          2：WEB/H5          3：网关/快捷/公众号
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                    put(request_type,"1");
                }else if (handlerUtil.isWapOrApp(channelWrapper)) {
                    put(request_type,"2");
                }else if (handlerUtil.isWY(channelWrapper) || handlerUtil.isYLKJ(channelWrapper)) {
                    put(request_type,"3");
                }
                put(total_fee,  channelWrapper.getAPI_AMOUNT());
                put(subject,"name");
                put(detail,"name");
                put(body,"name");
            }
        };
        log.debug("[小马]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //去除最后一个&符
        //paramsStr = paramsStr.substring(0,paramsStr.length()-1);
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[小马]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
         String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
         if (StringUtils.isBlank(resultStr)) {
             log.error("[小马]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
             //log.error("[小马]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
             //throw new PayException("返回空,参数："+JSON.toJSONString(map));
         }
         if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[小马]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
         }
         //JSONObject resJson = JSONObject.parseObject(resultStr);
         JSONObject resJson;
         try {
             resJson = JSONObject.parseObject(resultStr);
         } catch (Exception e) {
             e.printStackTrace();
             log.error("[小马]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
         }
         //只取正确的值，其他情况抛出异常
         if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && 
                 (resJson.containsKey("qrcode") && StringUtils.isNotBlank(resJson.getString("qrcode")) ||
                  resJson.containsKey("pay_url") && StringUtils.isNotBlank(resJson.getString("pay_url")))) {
             if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                 result.put(QRCONTEXT , resJson.getString("qrcode"));
//             }else if (handlerUtil.isWapOrApp(channelWrapper)) {
//                 result.put(QRCONTEXT , resJson.getString("qrcode"));
//             }else if (handlerUtil.isWY(channelWrapper) || handlerUtil.isYLKJ(channelWrapper)) {
             }else {
                 result.put(JUMPURL , resJson.getString("pay_url"));
             }
         }else {
             log.error("[小马]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
         }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[小马]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[小马]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}