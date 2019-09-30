package dc.pay.business.yiyouku;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("YIYOUKUZHIFU")
public final class YiYouKuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

     private static final String     bargainor_id = "bargainor_id";   //	整数	15173交易	商户ID
     private static final String     sp_billno = "sp_billno";   //	字符串	商家生成	商户订单号
     private static final String     total_fee = "total_fee";   //	小数（1.00）	商户	交易金额
     private static final String     pay_type = "pay_type";   //	字符	15173交易	g 支付宝
     private static final String     return_url = "return_url";   //	字符串	商户	同步跳转回调页面
     private static final String     select_url = "select_url";   //	字符串	商户	异步通知回调页面（推荐使用IP形式）
     private static final String     attach = "attach";   //	字符串	商户	自定义字符串1原样返回
     private static final String     zidy_code = "zidy_code";   //	字符串	商户	自定义字符串2原样返回
     private static final String     czip = "czip";   //	字符串	商户	提交用户IP
     private static final String     return_form = "return_form";   //	字符串	商户	接口返回形式4,支付宝H5
     private static final String     agent = "agent";   //	字符串	商户	代理商id
     private static final String     time_stamp = "time_stamp";   //	 不参与签名
     private static final String     sign = "sign";   //	字符串	商户	MD5签名，：把请求交易的部分参数与KEY拼凑成字符串经过国际标准32位MD5加密转换成大写后的值 签名顺序例子  bargainor_id=商户id&sp_billno=商户单号&total_fee=支付金额&pay_type=支付类型&return_url=同步返回地址&select_url=异步通知地址&attach=自定义1&zidy_code=自定义2&agent=代理商id&key=商户密钥代理商密钥






    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用[&]符号链接[商户号]和[代理商id],如：商户号&代理商id");
        }

        if(!channelWrapper.getAPI_KEY().contains("&")){
            throw new PayException("密钥格式错误，正确格式请使用[&]符号链接[提交数据key]和[接收(查询)结果key],如：提交数据key&接收(查询)结果key");
        }


        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(bargainor_id,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(sp_billno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(select_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(attach, channelWrapper.getAPI_ORDER_ID()  );
            payParam.put(zidy_code, channelWrapper.getAPI_ORDER_ID()  );
            payParam.put(czip, channelWrapper.getAPI_Client_IP()  );
            payParam.put(return_form, "4"  );
            payParam.put(agent, channelWrapper.getAPI_MEMBERID().split("&")[1]  );
            payParam.put(time_stamp, System.currentTimeMillis()+""  );
        }
        log.debug("[易游酷]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //bargainor_id=商户id&sp_billno=商户单号&total_fee=支付金额&pay_type=支付类型&return_url=同步返回地址&select_url=异步通知地址&attach=自定义1&zidy_code=自定义2&agent=代理商id&key=商户密钥代理商密钥
        //bargainor_id=%s&sp_billno=%s&total_fee=%s&pay_type=%s&return_url=%s&select_url=%s&attach=%s&zidy_code=%s&agent=%s&key=%s
        String paramsStr = String.format("bargainor_id=%s&sp_billno=%s&total_fee=%s&pay_type=%s&return_url=%s&select_url=%s&attach=%s&zidy_code=%s&agent=%s&key=%s",
                params.get(bargainor_id),
                params.get(sp_billno),
                params.get(total_fee),
                params.get(pay_type),
                params.get(return_url),
                params.get(select_url),
                params.get(attach),
                params.get(zidy_code),
                params.get(agent),
                channelWrapper.getAPI_KEY().split("&")[0]);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[易游酷]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "1".equalsIgnoreCase(jsonResultStr.getString("status"))
                            && jsonResultStr.containsKey("payurl") && StringUtils.isNotBlank(jsonResultStr.getString("payurl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("payurl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("payurl"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[易游酷]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[易游酷]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[易游酷]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}