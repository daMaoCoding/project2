package dc.pay.business.juxin;

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
 * Nov 10, 2018
 */
@RequestPayHandler("JUXIN")
public final class JuXinPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuXinPayRequestHandler.class);

    //序号        参数名             参数名称            类型           是否必填           说明
    //1.          version             版本号              String           否           默认：V1
    //2.          mer_no              商户号              String           是           平台分配的唯一商户编号
    //3.          mer_order_no        商户订单号          String           是           商户必须保证唯一
    //4.          ccy_no              币种                String           否           CNY:人民币 USD:美元 默认CNY
    //5.          order_amount        交易金额            Number           是           分为单位，整数
    //6.          busi_code           业务编码            String           是           详情见：附件业务编码
    //7.          goods               商品名称            String           是           商品名称
    //8.          reserver            保留信息            String           否           原样返回
    //9.          bg_url              异步通知地址        String           是           支付成功后，平台主动通知商家系统，商家系统必须指定接收通知的地址。
    //10.   bankCode    银行编码    String  否   网关业务必填
    //10.         page_url            页面跳转地址        String           否           支付成功后，页面跳转至此地址
    //11.         sign                数字签名            String           是           详情见：数字签名
    private static final String version                                  ="version";
    private static final String mer_no                                   ="mer_no";
    private static final String mer_order_no                             ="mer_order_no";
    private static final String ccy_no                                   ="ccy_no";
    private static final String order_amount                             ="order_amount";
    private static final String busi_code                                ="busi_code";
    private static final String goods                                    ="goods";
    private static final String reserver                                 ="reserver";
    private static final String bg_url                                   ="bg_url";
    private static final String bankCode                                   ="bankCode";
    private static final String page_url                                 ="page_url";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"V1");
                put(mer_no, channelWrapper.getAPI_MEMBERID());
                put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
                put(ccy_no,"CNY");
                put(order_amount,  channelWrapper.getAPI_AMOUNT());
                put(goods,"name");
                put(reserver, channelWrapper.getAPI_MEMBERID());
                put(bg_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(page_url,channelWrapper.getAPI_WEB_URL());
                if (handlerUtil.isWY(channelWrapper)) {
                    put(busi_code,"100301");
                    put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                    put(busi_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        log.debug("[聚信]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚信]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[聚信]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[聚信]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[聚信]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[聚信]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("status") && "SUCCESS".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("order_data") && StringUtils.isNotBlank(resJson.getString("order_data"))) {
            String code_url = resJson.getString("order_data");
            if (handlerUtil.isWY(channelWrapper)) {
                result.put( HTMLCONTEXT, code_url);
            }else {
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper) && code_url.contains("form") && code_url.contains("action")) {
                  //支付宝扫码，不允许电脑上直接扫码  这方法靠谱
                    if ((HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())) || channelWrapper.getAPI_ORDER_ID().startsWith("T") || 
                        handlerUtil.isWapOrApp(channelWrapper)) {
                        result.put( HTMLCONTEXT, code_url);
                    }else {
                        throw new PayException("请在APP或者WAP应用上使用通道......");
                    }
                }else if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                    result.put( QRCONTEXT, code_url);
                }else {
                    result.put( JUMPURL, code_url);
                }
            }
        }else {
            log.error("[聚信]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[聚信]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[聚信]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}