package dc.pay.business.mangguo;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.*;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;


/**
 * ************************
 * @author tony 3556239829
 */
@RequestPayHandler("MANGGUO")
public class MangGuoPayRequestHandler extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(MangGuoPayRequestHandler.class);
    private static final String  member_code  = "member_code";         //商户号
    private static final String  type_code    = "type_code";           //支付类型
    private static final String  down_sn      = "down_sn";             //商户订单号
    private static final String  subject      = "subject";             //主题
    private static final String  amount       = "amount";              //交易金额
    private static final String  notify_url   = "notify_url";           //异步通知地址
    private static final String  sign         = "sign";                //签名
    private static final String  card_type    = "card_type";            //银行卡类型1
    private static final String  bank_segment = "bank_segment";         //银行代号
    private static final String  user_type    = "user_type";             //用户类型1
    private static final String  agent_type   = "agent_type";            //渠道1
    private static final String JUMPURL = "JUMPURL";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误：商户号&交易私钥(来自第三方网站)");
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                String cFlag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
              //  put("member_code",channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put("down_sn", channelWrapper.getAPI_ORDER_ID());
                put("subject","PAY");
                put("amount", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put("notify_url", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put("type_code", cFlag.split(",")[1]);
                if("gateway".equalsIgnoreCase(cFlag.split(",")[0])){
                    put("type_code", "gateway");
                    put("card_type", "1");
                    put("bank_segment", cFlag.split(",")[1]);
                    put("user_type", "1");
                    put("agent_type", "1");
//                  put("mobile", "");
//                  put("account_name", "");
//                  put("id_card_no", "");
//                  put("account_no", "");
//                  put("return_url", "");
                }
            }
        };
        log.debug("[芒果]-[请求支付]-1.组装请求参数完成："+JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam) throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&") ||channelWrapper.getAPI_MEMBERID().split("&").length!=2 || StringUtil.isBlank(channelWrapper.getAPI_MEMBERID().split("&")[1])){
            throw new PayException("商户号格式错误：商户号&交易私钥(来自第三方网站)");
        }
        String pay_md5sign = null;
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtil.isBlank(payParam.get(paramKeys.get(i)))|| "member_code".equalsIgnoreCase(paramKeys.get(i))  || "sign".equalsIgnoreCase(paramKeys.get(i))  || "code".equalsIgnoreCase(paramKeys.get(i))  || "msg".equalsIgnoreCase(paramKeys.get(i)) )
                continue;
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append("key="+channelWrapper.getAPI_MEMBERID().split("&")[1]);//
        pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        log.debug("[在线宝]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try{
            String payParamStr =  RsaUtil.encryptToBase64(JSON.toJSONString(payParam), channelWrapper.getAPI_PUBLIC_KEY()) ;
            HashMap<String, String> paramP = Maps.newHashMap();
            paramP.put("cipher_data", payParamStr);
            paramP.put(member_code,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            String resultJsonStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), paramP, String.class, HttpMethod.POST).trim();
            JSONObject resultJsonObj = JSONObject.fromObject(resultJsonStr);
            String stateCode = resultJsonObj.getString("code");
            if (!stateCode.equals("0000")) {
                String resultMsg = resultJsonObj.getString("msg");
                log.error("[芒果请求支付订单创建失败]-["+channelWrapper.getAPI_CHANNEL_BANK_NAME()+"]-["+channelWrapper.getAPI_ORDER_ID()+"]-"+resultMsg);
                throw new PayException(resultMsg);
            }
            if (HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isFS(channelWrapper)) {
                  HashMap<String, String> resultMap = Maps.newHashMap();
                  resultMap.put(JUMPURL, resultJsonObj.getString("code_url"));
                   payResultList.add(resultMap);
            }else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM")) {
                HashMap<String, String> resultMap = Maps.newHashMap();
                resultMap.put(QRCONTEXT, resultJsonObj.getString("code_url"));
                payResultList.add(resultMap);
            }else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_")){
                HashMap<String, String> resultMap = Maps.newHashMap();
                resultMap.put(JUMPURL, resultJsonObj.getString("code_url"));
                payResultList.add(resultMap);
            }else{
                log.error("[注意：芒果请求支付订单创建失败]-["+channelWrapper.getAPI_CHANNEL_BANK_NAME()+"]-["+channelWrapper.getAPI_ORDER_ID()+"]-"+"签名验证失败。");
                throw new PayException(SERVER_MSG.RESPONSE_PAY_VALDATA_SIGN_ERROR);
            }
        } catch (Exception e) {
            log.error("[芒果]-[请求支付]-3.发送支付请求，及获取支付请求结果失败，"+ e.getMessage());
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[芒果]-[请求支付]-3.发送支付请求，及获取支付请求结果成功："+ JSON.toJSONString(payResultList));
        return payResultList;
    }
    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> result) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(!result.isEmpty() && result.size()==1){
            requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
            requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            requestPayResult.setRequestPayQRcodeURL(null);
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());

            if(result.get(0).containsKey(JUMPURL)){
                 requestPayResult.setRequestPayJumpToUrl(result.get(0).get(JUMPURL));
            }else if(result.get(0).containsKey(QRCONTEXT)){
                 requestPayResult.setRequestPayQRcodeContent(result.get(0).get(QRCONTEXT));
            }else if(result.get(0).containsKey(HTMLCONTEXT)){
                requestPayResult.setRequestPayHtmlContent(result.get(0).get(HTMLCONTEXT));
            }


            if(ValidateUtil.requestesultValdata(requestPayResult)){
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            }else{
                throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
         log.debug("[芒果]-[请求支付]-4.处理请求响应成功："+ JSON.toJSONString(requestPayResult));
         return requestPayResult;
    }
}