package dc.pay.business.xinjuhezhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 18, 2019
 */
@RequestPayHandler("XINJUHEZHIFU")
public final class XinJuHeZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinJuHeZhiFuPayRequestHandler.class);

    private static final String service   = "service";    //  业务类型 是 Max(20) 固定值 “pay” 1
    private static final String appid     = "appid";      //  商户编号 是 Max(11)    是商户在[API 聚合支付]系统的唯一身份标识            请登录商户后台查看2
    private static final String orderid   = "orderid";    //  商户订单号 是 Max(20)    提交的订单号必须在自身账户交易中唯一； 不能重复提交，不得包含符号等特殊字符3
    private static final String money     = "money";      //  支付金额 是 Max(20) 单位：元，精确到分，保留小数点后两位 4
    private static final String code      = "code";       //  产品编码 是 Max(10) 以商户后台开通的产品为准 5
    private static final String time      = "time";       //  请求时间 是 Max(20) 请求时间戳（北京时间） 6
    private static final String version   = "version";    //  接口版本 是 Max(10) 固定2.1 9
    private static final String nonce_str = "nonce_str";  //  随机数 是 Max(32) 随机数，不能超过32位 10
    private static final String noticeurl = "noticeurl";  //  异步通知地址 是 Max(200) 异步通知地址，接收支付成的通知，接收方式 post 11
//  private static final String attach            ="attach";     //  扩展 否 Max(10) 扩展字段 8
//  private static final String returnurl         ="returnurl";  //  同步跳转地址 否 Max(200) 部分产品无效 12
//  private static final String productname       ="productname";//  商品名称 否 Max(100) 商品名称 13
//  private static final String productnum        ="productnum"; //  商品数量 否 Max(11) 商品数量 14

    private static final String key       = "key";
    //signature    数据签名    32    是    　
    private static final String signature = "sign"; //  请求密钥 是 Max(32) 请查看《安全规范》（文档末尾） 7

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(service, "pay");
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(orderid, channelWrapper.getAPI_ORDER_ID());
                put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(code, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(time, System.currentTimeMillis() + "");
                put(version, "2.1");
                put(nonce_str, HandlerUtil.randomStr(10));
                put(noticeurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(nonce_str, HandlerUtil.randomStr(10));
            }
        };
        log.debug("[新聚合支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
//        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新聚合支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新聚合支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("code") && "0".equalsIgnoreCase(resJson.getString("code"))
                    && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
                resJson = JSONObject.parseObject(resJson.getString("data"));
                String code_url = resJson.getString("payurl");
                result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            } else {
                log.error("[新聚合支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[新聚合支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新聚合支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[新聚合支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}