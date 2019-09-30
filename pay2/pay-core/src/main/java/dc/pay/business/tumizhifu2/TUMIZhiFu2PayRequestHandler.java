package dc.pay.business.tumizhifu2;

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
 * Mar 30, 2019
 */
@RequestPayHandler("TUMIZHIFU2")
public final class TUMIZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TUMIZhiFu2PayRequestHandler.class);

    //请求参数
    //字段名                            参数                     类型(字节长度）        是否必填         示例值            备注
    //商户号                            merchant_no              String(20)                是                               请求交易的商户编号，由系统提供此数据
    //请求订单编号                      merchant_req_no          String(20)                是                               下游请求订单编号，保持该商户中的唯一即可，建议采用日期+随机数字
    //交易金额                          order_amt                String(20)                是             10.00             订单交易金额，以元为单位。
    //后台通知地址地址                  bg_url                   String(255)               是                               交易结束时候的通知地址
    //后台通知目标服务器所在地区        bg_server_area           String(255)               否             CHN               下游接收服务器为境内大陆地区，则填写CHN或者为空即可；若接收通知服务器为境外则上传其他任意字符即可
    //前台通知地址                      return_url               String(255)               否                               若支持前台通知，则此参数必填
    //业务代码                          biz_code                 String(5)                 是             10001             业务代码类型：   10001：支付宝WAP  10002：支付宝扫码
    //商品名称                          subject                  String(20)                是             测试商品          购买的商品名称
    //付款人IP                          payer_ip                 String(20)                否                               付款人IP
    //是否支持信用卡支付                is_support_cd            String(20)                否             1                 是否允许信用卡支付订单，若为1，则表示支持；0：表示不支持。默认为1
    //签名                              sign                     String(20)                是                               签名(采用交易密钥进行签名)
    private static final String merchant_no                ="merchant_no";
    private static final String merchant_req_no            ="merchant_req_no";
    private static final String order_amt                  ="order_amt";
    private static final String bg_url                     ="bg_url";
    private static final String bg_server_area             ="bg_server_area";
//    private static final String return_url                 ="return_url";
    private static final String biz_code                   ="biz_code";
    private static final String subject                    ="subject";
//    private static final String payer_ip                   ="payer_ip";
    private static final String is_support_cd              ="is_support_cd";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_no, channelWrapper.getAPI_MEMBERID());
                put(merchant_req_no,channelWrapper.getAPI_ORDER_ID());
                put(order_amt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(bg_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(bg_server_area,"CHN");
                put(biz_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(subject,"name");
                put(is_support_cd,"0");
            }
        };
        log.debug("[TUMI支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        System.out.println(paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[TUMI支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[TUMI支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[TUMI支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[TUMI支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("code") && "00".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("pay_url") && StringUtils.isNotBlank(resJson.getString("pay_url"))) {
            String code_url = resJson.getString("pay_url");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else{
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[TUMI支付2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[TUMI支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[TUMI支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}