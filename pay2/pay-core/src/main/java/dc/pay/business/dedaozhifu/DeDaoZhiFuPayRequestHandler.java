package dc.pay.business.dedaozhifu;

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
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("DEDAOZHIFU")
public final class DeDaoZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


     private static final String      mch_id = "mch_id";   //       商户号       String(32) 是 1234567890 平台分配的商户号
     private static final String      trade_type = "trade_type";   //       交易类型       String(10) 是 ALIH5 详见附录支付类型
     private static final String      nonce = "nonce";   //       随机字符串       String(32) 是 7VS264I5K8502SI8ZNMTM6LTKCH16CQ2    随机字符串，不长于32位
     private static final String      timestamp = "timestamp";   //       时间戳       String(32) 是 1524822584 UNIX时间戳
     private static final String      subject = "subject";   //       订单名称       String(200) 是 商品标题/交易标题/订单标题
     private static final String      out_trade_no = "out_trade_no";   //      商户订单号
     private static final String      total_fee = "total_fee";   //       总金额       Int 是 8 订单总金额，单位为分
     private static final String      spbill_create_ip = "spbill_create_ip";   //       终端IP       String(32) 是 123.12.12.123
     private static final String      notify_url = "notify_url";   //       异步地址       String(100) 是
     private static final String      jump = "jump";   //      跳转还是二维码
     private static final String      sign = "sign";   //      签名信息


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mch_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(nonce,HandlerUtil.getRandomStr(10));
            payParam.put(timestamp,System.currentTimeMillis()+"");
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,  channelWrapper.getAPI_AMOUNT());
            payParam.put(spbill_create_ip,channelWrapper.getAPI_Client_IP() );
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(jump, "0");
        }

        log.debug("[得到支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[得到支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonResultStr.getString("result_code"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)  && jsonResultStr.containsKey("pay_url") && StringUtils.isNotBlank(jsonResultStr.getString("pay_url"))){
                            result.put(JUMPURL, jsonResultStr.getString("pay_url"));
                        }else if(HandlerUtil.isWEBWAPAPP_SM(channelWrapper)&& jsonResultStr.containsKey("qrcode") && StringUtils.isNotBlank(jsonResultStr.getString("qrcode")) ){
                            result.put(QRCONTEXT, jsonResultStr.getString("qrcode"));
                        }else{
                            throw new PayException(resultStr);
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[得到支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[得到支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[得到支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}