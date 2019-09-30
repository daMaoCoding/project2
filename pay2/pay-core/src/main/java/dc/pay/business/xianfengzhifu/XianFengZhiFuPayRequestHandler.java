package dc.pay.business.xianfengzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.excel.channelConfig.ExcelHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XIANFENGZHIFU")
public final class XianFengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XianFengZhiFuPayRequestHandler.class);

    private static final String       merchant	  = "merchant";             //String	是	商户号，请到商户后台查询
    private static final String       m_orderNo	  = "m_orderNo";            //String	是	商户订单号，请确保本商户内唯一
    private static final String       tranAmt	  = "tranAmt";              //String	是	交易金额，单位元四舍五入保留2位小数点,所有金额的单位为“元”，
    private static final String       pname	  = "pname";                    //String	是	商品名称，最多255个字符
    private static final String       notifyUrl	  = "notifyUrl";             //String	是	异步回调地址,不超过128字符
    private static final String       sign	  = "sign";                      //String	是	签名,签名方法见上面文档说明
    private static final String       pnum	  = "pnum";                        //Int	否	商品数量




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
            payParam.put(merchant,channelWrapper.getAPI_MEMBERID());
            payParam.put(m_orderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(tranAmt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(pname,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pnum,"1");
        log.debug("[先疯]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[先疯]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 &&HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("retCode") && "000000".equalsIgnoreCase(jsonResultStr.getString("retCode")) && jsonResultStr.containsKey("retMsg")){
                            if(null!=jsonResultStr.getJSONObject("retMsg")&& jsonResultStr.getJSONObject("retMsg").containsKey("paymentInfo") && StringUtils.isNotBlank(jsonResultStr.getJSONObject("retMsg").getString("paymentInfo"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, HandlerUtil.UrlDecode(jsonResultStr.getJSONObject("retMsg").getString("paymentInfo")));
                                }else{
                                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getJSONObject("retMsg").getString("paymentInfo")));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(ExcelHelper.unicodeToUtf8(resultStr));
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[先疯]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[先疯]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[先疯]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}