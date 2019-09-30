package dc.pay.business.lixinsaomazhifu;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RequestPayHandler("LIXINSAOMAZHIFU")
public final class LiXinSaoMaZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LiXinSaoMaZhiFuPayRequestHandler.class);

    private static final String mer_num         ="mer_num";    //参与签名    商户号
    private static final String pay_way         ="pay_way";    //参与签名    支付通道:ZFB, ZFBWAP
    private static final String money           ="money";      //参与签名    金额，以分为单位，一元等于一百分
    private static final String order_num       ="order_num";  //参与签名    商户订单号，订单号必须保持唯一，不能重复，由商户网站产生(建议顺序累加)
    private static final String goods_name      ="goods_name"; //参与签名    商品名称
    private static final String notify_url      ="notify_url"; //参与签名    回调通知地址
    private static final String return_url      ="return_url"; //参与签名    返回的页面地址
    private static final String version         ="version";    //参与签名    系统版本号,  3.0
    private static final String pattern         ="pattern";    //不参与签名     支付模式，form

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mer_num, channelWrapper.getAPI_MEMBERID());
                put(pay_way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(money,  channelWrapper.getAPI_AMOUNT());
                put(order_num,  channelWrapper.getAPI_ORDER_ID());
                put(goods_name, channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().contains("WX_SM")) {
                    put(version, "1.0");
                } else {
                    put(version, "3.0");
                }
                put(pattern, "form");
            }
        };
        log.debug("[立信扫码支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
//       sign=md5(mer_num&pay_way&money&order_num&goods_name&notify_url&return_url&version&key)
//       以上拼凑值不要有空格！  使用标准MD5算法对该字符串进行加密，并严格按照此顺序排序
//       加密结果全部转换成大写后，即为我们所需的订单MD5 校验码，将其写入sign字段即可
        String signSrc = String.format("%s&%s&%s&%s&%s&%s&%s&%s&%s",
                params.get(mer_num),
                params.get(pay_way),
                params.get(money),
                params.get(order_num),
                params.get(goods_name),
                params.get(notify_url),
                params.get(return_url),
                params.get(version),
                channelWrapper.getAPI_KEY());

        pay_md5sign = HandlerUtil.getMD5UpperCase(signSrc);
        log.debug("[立信扫码支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {

        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();

        try {
//            if (1==2 && HandlerUtil.isYLSM(channelWrapper)){
//                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//                JSONObject jsonObject = null;
//                try {
//                    jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
//                } catch (Exception e) {
//                    log.error("[立信扫码支付]-[请求支付]-3.1.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
//                    throw new PayException(e.getMessage(),e);
//                }
//                if (null != jsonObject && jsonObject.containsKey("status") && "200".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("qrUrl") && StringUtils.isNotBlank(jsonObject.getString("qrUrl"))) {
//                    result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject.getString("qrUrl"));
//                    result.put("第三方返回", jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
//                }else {
//                    log.error("[立信扫码支付]3.3.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//            }else {
                    result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//            }
            payResultList.add(result);
        } catch (Exception e) {
            log.error("[立信扫码支付]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[立信扫码支付]-[请求支付]-3..发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));

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
        log.debug("[立信扫码支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}