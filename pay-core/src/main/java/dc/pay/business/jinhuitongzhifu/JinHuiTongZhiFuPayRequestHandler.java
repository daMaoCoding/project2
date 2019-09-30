package dc.pay.business.jinhuitongzhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("JINHUITONGZHIFU")
public final class JinHuiTongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinHuiTongZhiFuPayRequestHandler.class);

  private static final String    return_type  = "return_type";     //"return_type": "json",
  private static final String    is_type  = "is_type";     //"is_type": "alipay2",
  private static final String    price  = "price";     //"price": "1.00",
  private static final String    time  = "time";     //"time": "1528291584",
  private static final String    notify_url  = "notify_url";     //"notify_url": "http://www.angelpay.vip/paytest/paynotify.php",
  private static final String    return_url  = "return_url";     //"return_url": "http://www.angelpay.vip/paytest/payreturn.php",
  private static final String    order_id  = "order_id";     //"order_id": "H606915842327656",
  private static final String    mark  = "mark";     //"mark": "mark",
  private static final String    api_code  = "api_code";     //"api_code": "33396133",
  private static final String    sign  = "sign";     //"sign": "62098C75CE3EDD59817437C7A2ED05C2"


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(return_type,"json");
            payParam.put(is_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(time,String.valueOf(System.currentTimeMillis()));
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(order_id,channelWrapper.getAPI_ORDER_ID());
            payParam.put(mark,channelWrapper.getAPI_ORDER_ID());
            payParam.put(api_code,channelWrapper.getAPI_MEMBERID());
        }
        log.debug("[今汇通支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[今汇通支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                String returncode="";
                if(null!=jsonResultStr && jsonResultStr.containsKey("messages")){
                    JSONObject messagesJson = jsonResultStr.getJSONObject("messages");
                    if(null!=messagesJson && messagesJson.containsKey("returncode")) returncode = messagesJson.getString("returncode");
                }
                if(!"SUCCESS".equalsIgnoreCase(returncode)) throw new PayException(resultStr);

                    if(null!=jsonResultStr && jsonResultStr.containsKey("payurl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payurl"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL,  jsonResultStr.getString("payurl"));
                                }else{
                                    result.put(QRCONTEXT,  jsonResultStr.getString("payurl"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
            }
        } catch (Exception e) { 
             log.error("[今汇通支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[今汇通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[今汇通支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}