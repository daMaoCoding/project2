package dc.pay.business.juyuanzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("JUYUANZHIFU")
public final class JuYuanZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuYuanZhiFuPayRequestHandler.class);

    private static final String      app_id	 = "app_id";     // √	√	参数名称：商家号    商户签约时，591分配给商家的唯一身份标识。例如：1803110116
    private static final String      trade_type	 = "trade_type";     // √	√	参数名称：交易类型    参见附录中的支付类型代码对照表。
    private static final String      total_amount	 = "total_amount";     // √	√	参数名称：订单金额    订单总金额，单位为分
    private static final String      out_trade_no	 = "out_trade_no";     // √	√	参数名称：商家订单号    商家网站生成的订单号，由商户保证其唯一性，要求32个字符内，由字母、数字、下划线组成。
    private static final String      notify_url	 = "notify_url";     // √	√	参数名称：异步通知地址    下行异步通知的地址，需要以http://开头且没有任何参数
    private static final String      interface_version	 = "interface_version";     // √	×	参数名称：接口版本    固定值：V2.0(大写)。
    private static final String      sign	 = "sign";





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(app_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(total_amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(interface_version,"V2.0");
        }
        log.debug("[聚源支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())  || interface_version.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString().replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[聚源支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                if(StringUtils.isNotBlank(resultStr)){
                    // <a id="btnOK2" class="visible-xs-block btn btn-primary order-btn" href="https://qr.alipay.com/fkx03884pkcpc8kyqcregeb?t=1541393258255">已复制，打开 支付宝 付款</a>
                    Document document = Jsoup.parse(resultStr);
                    Element bodyEl = document.getElementById("btnOK2");
                    if(bodyEl==null) throw new PayException("第三方二维码页面错误："+resultStr);
                    String  imgSrc="";
                      try{
                          imgSrc =bodyEl.attr("href");
                      }catch (Exception e){
                          throw new PayException("解析第三方二维码失败："+resultStr);
                      }

                    if(StringUtils.isNotBlank(imgSrc)){
                        result.put(QRCONTEXT, imgSrc);
                        payResultList.add(result);
                    }else{
                        throw new PayException("解析第三方二维码失败："+resultStr);
                    }
                }else{
                    throw new PayException("第三方返回异常:返回空，参数："+ JSON.toJSONString(payParam));
                }


//                JSONObject jsonResultStr = JSON.parseObject(resultStr);
//
//                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
//                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
//                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
//                                payResultList.add(result);
//                            }
//                    }else {
//                        throw new PayException(resultStr);
//                    }

            }
        } catch (Exception e) { 
             log.error("[聚源支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[聚源支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[聚源支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}