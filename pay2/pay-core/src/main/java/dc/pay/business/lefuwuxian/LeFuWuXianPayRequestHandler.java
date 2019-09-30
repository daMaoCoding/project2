package dc.pay.business.lefuwuxian;

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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("LEFUWUXIAN")
public final class LeFuWuXianPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


    private static final String     cpId = "cpId";   // 是 商户ID
    private static final String     serviceId = "serviceId";   // 是 服务ID（见后台服务管理）
    private static final String     payType = "payType";   // 是 支付类型（见附录2）
    private static final String     fee = "fee";   // 是 支付金额（单位分）
    private static final String     subject = "subject";   // 是 商品名
    private static final String     description = "description";   // 是 商品说明
    private static final String     orderIdCp = "orderIdCp";   // 是 商户订单号
    private static final String     notifyUrl = "notifyUrl";   // 是 异步通知地址
    private static final String     callbackUrl = "callbackUrl";   // 是 前端回调地址
    private static final String     timestamp = "timestamp";   // 是 当前时间戳（13位）
    private static final String     ip = "ip";   // 是 用户ip
    private static final String     version = "version";   // 是 版本号（固定1）
    private static final String     sign = "sign";   // 是 签名（规则见附录1）


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链[商户ID]和[服务ID（见后台服务管理）], 例如：商户ID&服务ID");
        }


        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(cpId,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(serviceId,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(fee,channelWrapper.getAPI_AMOUNT());
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(description,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderIdCp,channelWrapper.getAPI_ORDER_ID() );
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(callbackUrl,channelWrapper.getAPI_WEB_URL() );
            payParam.put(timestamp,System.currentTimeMillis()+"" );
            payParam.put(ip,channelWrapper.getAPI_Client_IP() );
            payParam.put(version,"1" );
        }

        log.debug("[乐付无限]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString().replaceFirst("key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[乐付无限]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
/*				
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getValueAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
				
*/				
				
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "0".equalsIgnoreCase(jsonResultStr.getString("status"))
                            && jsonResultStr.containsKey("payUrl") && StringUtils.isNotBlank(jsonResultStr.getString("payUrl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("payUrl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("payUrl"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[乐付无限]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[乐付无限]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[乐付无限]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}