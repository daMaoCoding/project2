package dc.pay.business.jinniuzhifu;

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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RequestPayHandler("JINNIUZHIFU")
@Slf4j
public class JinNiuZhiFuPayRequestHandler extends PayRequestHandler {
    //参数名称	参数含义	是否必填	参与签名	参数说明
    //	商户号	是	是	平台分配商户号
    private static final String PAY_MEMBERID = "pay_memberid";
    //	订单号	是	是	上送订单号唯一, 字符长度20
    private static final String PAY_ORDERID = "pay_orderid";
    //	提交时间	是	是	时间格式：2016-12-26 18:18:18
    private static final String PAY_APPLYDATE = "pay_applydate";
    //	银行编码	是	是	参考后续说明
    private static final String PAY_BANKCODE = "pay_bankcode";
    //	服务端通知	是	是	服务端返回地址.（POST返回数据）
    private static final String PAY_NOTIFYURL = "pay_notifyurl";
    //	页面跳转通知	是	是	页面跳转返回地址（POST返回数据）
    private static final String PAY_CALLBACKURL = "pay_callbackurl";
    //	订单金额	是	是	商品金额
    private static final String PAY_AMOUNT = "pay_amount";


    //	MD5签名	是	否	请看MD5签名字段格式
    private static final String PAY_MD5SIGN = "pay_md5sign";
    //	商品名称	是	否
    private static final String PAY_PRODUCTNAME = "pay_productname";

    //	附加字段	否	否	此字段在返回时按原样返回 (中文需要url编码)
    private static final String PAY_ATTACH = "pay_attach";
    //	商户品数量	否	否
    private static final String PAY_PRODUCTNUM = "pay_productnum";
    //	商品描述	否	否
    private static final String PAY_PRODUCTDESC = "pay_productdesc";
    //	商户链接地址	否	否
    private static final String PAY_PRODUCTURL = "pay_producturl";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(PAY_MEMBERID, channelWrapper.getAPI_MEMBERID());
                put(PAY_ORDERID, channelWrapper.getAPI_ORDER_ID());
                put(PAY_APPLYDATE, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(PAY_BANKCODE, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(PAY_NOTIFYURL, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(PAY_CALLBACKURL, channelWrapper.getAPI_WEB_URL());
                //金牛技术说 单位是 元 2019-08-17
                put(PAY_AMOUNT, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));

                //put(PAY_MD5SIGN, "pay_md5sign");
                put(PAY_PRODUCTNAME, "123");

                put(PAY_ATTACH, "");
                put(PAY_PRODUCTNUM, "");
                put(PAY_PRODUCTDESC, "");
                put(PAY_PRODUCTURL, "");
            }
        };
        log.debug("[金牛支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 签名算法：
     * 签名生成的通用步骤如下：
     * 第一步，设所有发送或者接收到的数据为集合M，将集合M内非空参数值的参数按照参数名ASCII码从小到大排序（字典序），
     * 使用URL键值对的格式（即key1=value1&key2=value2…）拼接成字符串。
     * 第二步，在stringA最后拼接上key得到stringSignTemp字符串，并对stringSignTemp进行MD5运算，
     * 再将得到的字符串所有字符转换为大写，得到sign值signValue。
     *
     * @param payParam
     * @return
     * @throws PayException
     */
    @Override
    protected String buildPaySign(Map<String, String> payParam) throws PayException {
        if (CollectionUtils.isEmpty(payParam)) return StringUtils.EMPTY;
        StringBuilder paramStr = new StringBuilder();
        for (Map.Entry<String, String> entry : payParam.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue()) && !PAY_PRODUCTNAME.equals(entry.getKey())) {
                paramStr.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        paramStr.append("key=").append(channelWrapper.getAPI_KEY());

        String signMd5 = HandlerUtil.getMD5UpperCase(paramStr.toString());
        log.debug("[金牛支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) + "，参数：" + JSON.toJSONString(paramStr));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        if (CollectionUtils.isEmpty(payParam) || StringUtils.isBlank(pay_md5sign)) return Lists.newArrayList();
        Map<String, String> map = new LinkedHashMap<>(16);
        for (Map.Entry<String, String> entry : payParam.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue())) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        map.put(PAY_MD5SIGN, pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper)) {
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //保存第三方返回值
            result.put(HTMLCONTEXT, htmlContent.toString());
        } else {
            String tmpStr;
            try {
                tmpStr = RestTemplateUtil.sendByRestTemplate(api_CHANNEL_BANK_URL, map, String.class, HttpMethod.POST);
                if (StringUtils.isBlank(tmpStr)) {
                    log.error("[金牛支付]3.1.发送支付请求，获取支付请求返回值异常:返回空");
                    throw new PayException("返回空");
                }
                tmpStr = new String(tmpStr.trim().getBytes("UTF-8"), "UTF-8");
            } catch (Exception e) {
                log.error("[金牛支付]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                throw new PayException(e.getMessage(), e);
            }
            JSONObject jsonObject = JSONObject.parseObject(tmpStr);
            if (jsonObject.containsKey("status") && "success".equals(jsonObject.getString("status"))) {
                if (jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    JSONObject data = JSONObject.parseObject(jsonObject.getString("data"));
                    result.put(JUMPURL, data.getString("payurl"));
                    result.put("第三方返回", tmpStr); //保存全部第三方信息，上面的拆开没必要
                    payResultList.add(result);
                } else {
                    log.error("[金牛支付]3.3.发送支付请求， 返回值为空");
                    throw new PayException(jsonObject.getString("message"));
                }
            } else {
                log.error("[金牛支付]3.2.发送支付请求，获取支付请求返回值异常:" + jsonObject.getString("message"));
                throw new PayException(jsonObject.getString("message"));
            }
        }
        log.debug("[金牛支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (CollectionUtils.isEmpty(resultListMap) || resultListMap.size() != 1) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }

        Map<String, String> qrMap = resultListMap.get(0);
        if (null != qrMap) {
            if (qrMap.containsKey(QRCONTEXT)) {
                requestPayResult.setRequestPayQRcodeContent(qrMap.get(QRCONTEXT));
            } else if (qrMap.containsKey(HTMLCONTEXT)) {
                requestPayResult.setRequestPayHtmlContent(qrMap.get(HTMLCONTEXT));
            } else if (qrMap.containsKey(JUMPURL)) {
                requestPayResult.setRequestPayJumpToUrl(qrMap.get(JUMPURL));
            }
        }

        requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
        requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
        requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
        requestPayResult.setRequestPayQRcodeURL(null);
        requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
        if (!ValidateUtil.requestesultValdata(requestPayResult)) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        log.debug("[金牛支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}
