package dc.pay.business.yunhuitong;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
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
import dc.pay.utils.kspay.AESUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("YUNHUITONG")
public final class YunHuiTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YunHuiTongPayRequestHandler.class);

    private static final String     requestNo ="requestNo";    //  商户请求号  String(64)  Y  商户请求号
    private static final String     payType ="payType";    //  支付类型  String(30)  Y  见附录
    private static final String     amount ="amount";    //  金额  String(11)  Y  金额
    private static final String     channelCode ="channelCode";    //  通道编码  String(32)  Y  固定值：GAMEPAY
    private static final String     productName ="productName";    //  产品名称  String(32)  Y  产品名称
    private static final String     productDesc ="productDesc";    //  产品描述  String(64)  Y  最长允许64个汉字
    private static final String     requestIp ="requestIp";    //  请求ip  String(18)  Y  客户端请求ip
    private static final String     serverCallbackUrl ="serverCallbackUrl";    //  回调地址  String(128)  Y

    private static final String     siteName ="siteName";    //  网站名称  String(18)  N  银联wap时必填
    private static final String     siteAddress ="siteAddress";    //  网站地址  String(64)  N  银联wap时必填
    private static final String     isFrame ="isFrame";    //  是否微信  String(1)  N  是否微信端打开，1表示微信  端打开
    private static final String     payTool ="payTool";    //  支付工具  String(2)  N 扫码支付时必填  q3：银联扫码,返回支付链接  q4：银联扫码，返回二维码 图片的二进制串
    private static final String     quickUserId ="quickUserId";    //  快捷用户ID  String(7)  N  快捷时必填，快捷用户id， 商户唯一，用于快捷绑卡对 应
    private static final String     bankChannelNo ="bankChannelNo";    //  银行编码  String(16)  N  网银支付时必填，见3.4
    private static final String     sign="sign";
    private static final String     appKey="appKey";
    private static final String     data="data";
    private static final String     code="code";
    private static final String     payurl="payurl";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(requestNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(channelCode,"GAMEPAY");
            payParam.put(productName,productName);
            payParam.put(productDesc,productDesc);
            payParam.put(requestIp,channelWrapper.getAPI_Client_IP());
            payParam.put(serverCallbackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL().concat("/").concat("?orderId=").concat(channelWrapper.getAPI_ORDER_ID()));
            if(HandlerUtil.isYLWAP(channelWrapper)){
                payParam.put(siteName,siteName);
                payParam.put(siteAddress,"http://www.baidu.com");
            }
        }
        log.debug("[云汇通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        log.debug("[云汇通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      //  payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String paramJsonStr = JSON.toJSONString(payParam);
        String dataStr = AESUtil.encrypt(paramJsonStr, channelWrapper.getAPI_KEY().substring(0, 16));

        Map<String, String> httpRequestParamMap = new HashMap<String, String>();
        httpRequestParamMap.put(appKey, channelWrapper.getAPI_MEMBERID());
        httpRequestParamMap.put(data, dataStr);

        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper) &&   HandlerUtil.isYLKJ(channelWrapper)   && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),httpRequestParamMap).toString().replace("method='post'","method='post'"));
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
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), httpRequestParamMap, String.class, HttpMethod.POST).trim();
				if(StringUtils.isNotBlank(resultStr) && !resultStr.contains("{")){
                    String responseJSONStr = null;
                    /** AES解密data, 使用secretKey的前16位 */
                    responseJSONStr = AESUtil.decrypt(resultStr, channelWrapper.getAPI_KEY().substring(0, 16));
                    Map<String, String> resultMap = JSON.parseObject(responseJSONStr,new TypeReference<TreeMap<String, String>>() {});
                    if(null!=resultMap && resultMap.containsKey(code) && resultMap.get(code).equalsIgnoreCase("200") &&  resultMap.containsKey(payurl) && StringUtils.isNotBlank(resultMap.get(payurl))){
                        result.put(HTMLCONTEXT,HandlerUtil.UrlDecode(resultMap.get(payurl)));
                    }else {
                        throw  new PayException(responseJSONStr);
                    }
                    payResultList.add(result);
                }
            }
        } catch (Exception e) { 
             log.error("[云汇通]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[云汇通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[云汇通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}