package dc.pay.business.shanxianfu;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Encoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RequestPayHandler("SHANXIANFUZHIFU")
public final class ShanXianFuPayRequestHandler extends PayRequestHandler {

     private static final String     version="version";    //	版本号，固定值：V3.0.0.0	String/8	是
     private static final String     merchNo="merchNo";    //	商户号	String/16	是
     private static final String     netwayCode="netwayCode";    //	支付网关代码，参考附录7.1	String/16	是
     private static final String     randomNum="randomNum";    //	随机数	String/8	是
     private static final String     orderNum="orderNum";    //	商户订单号，唯一	tring/32	是
     private static final String     amount="amount";    //	金额（单位：分）	String/16	是
     private static final String     goodsName="goodsName";    //	商品名称	String/20	是
     private static final String     callBackUrl="callBackUrl";    //	支付结果通知地址	String/128	是
     private static final String     callBackViewUrl="callBackViewUrl";    //	回显地址	String/128	是
     private static final String     charset="charset";    //	客户端系统编码格式，UTF-8、GBK	String/5	是
     private static final String     sign="sign";    //	签名（字母大写）	String/32	是

     private static final String     data="data";    //	请求数据	密文串
//     private static final String     merchNo="merchNo";    //	商户号	MJF开头的15位数
//     private static final String     version="version";    //	版本号	V3.0.0.0





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接【商户号】和【MD5秘钥】,如：商户号&MD5秘钥");
        }

        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"V3.0.0.0");
            payParam.put(merchNo,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(netwayCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(randomNum,randomNum);
            payParam.put(orderNum,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID() );
            payParam.put(callBackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(callBackViewUrl,channelWrapper.getAPI_WEB_URL() );
            payParam.put(charset, "UTF-8");
        }
        log.debug("[闪现付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        TreeMap<String, String> map = new TreeMap<String, String>(params);
        String metaSignJsonStr = ToolKit.mapToJson(map);
        String sign = ToolKit.MD5(metaSignJsonStr + channelWrapper.getAPI_MEMBERID().split("&")[1], ToolKit.CHARSET);// 32位
        return sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {

            Map<String, String> payParamNew = Maps.newHashMap();
            byte[] dataStr = ToolKit.encryptByPublicKey(ToolKit.mapToJson(payParam).getBytes(ToolKit.CHARSET), channelWrapper.getAPI_PUBLIC_KEY());
          //  rsamsgData = Base64.encode(RSAUtils.encryptByPublicKey(URLEncoder.encode(params.toString(), "utf-8").getBytes("utf-8"), channelWrapper.getAPI_PUBLIC_KEY(), "utf-8"));
            String param = new BASE64Encoder().encode(dataStr);
            payParamNew.put(data,param);
            payParamNew.put(merchNo,payParam.get(merchNo));
            payParamNew.put(version,payParam.get(version));

            if (1==2 && HandlerUtil.isWY(channelWrapper)  &&  HandlerUtil.isYLKJ(channelWrapper)   &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParamNew).toString().replace("method='post'","method='post'"));
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParamNew, String.class, HttpMethod.POST);
                if(StringUtils.isNotBlank(resultStr)) resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("stateCode") && "00".equalsIgnoreCase(jsonResultStr.getString("stateCode"))
                            && jsonResultStr.containsKey("qrcodeUrl") && StringUtils.isNotBlank(jsonResultStr.getString("qrcodeUrl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("qrcodeUrl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("qrcodeUrl"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[闪现付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[闪现付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[闪现付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}