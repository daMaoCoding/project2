package dc.pay.business.qianhai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 13, 2018
 */
@RequestPayHandler("QIANHAI")
public final class QianHaiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianHaiPayRequestHandler.class);

    //参数名称          变量名           类型             说明
    //版本号            version          String           默认 1.0
    //商户编号          merId            String           商户后台获取
    //商户订单号        orderId          String(32)           商户唯一订单号
    //订单金额          totalMoney       String           单位:分， 必须大于 100 分
    //支付类型          tradeType        String           支付宝： alipay
    //请求ip            ip               String           请求IP， 部分支付请求IP与支付IP必须一致，所以请上传用户请求的真实IP
    //商品描述          describe         String           商品描述
    //异步通知 URL      notify           String           数据异步通知(可在后台配置，后台配置的通知地址优先)
    //同步跳转URL       redirectUrl      String           不能带有任何参数(某些通道无效)
    //订单备注说明      remark           String(64)           可为空， 如果传递必须为字符串或者数据组合
    //支付来源          fromtype         String           wap : 普通 wapweixinwap : 微信内 wap
    //md5签名串         sign             String           参照签名校验规则
    private static final String version                 ="version";
    private static final String merId                   ="merId";
    private static final String orderId                 ="orderId";
    private static final String totalMoney              ="totalMoney";
    private static final String tradeType               ="tradeType";
    private static final String ip                      ="ip";
    private static final String describe                ="describe";
    private static final String notify                  ="notify";
    private static final String redirectUrl             ="redirectUrl";
    private static final String remark                  ="remark";
    private static final String fromtype                ="fromtype";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                //参数名称          变量名           类型             说明
                //版本号                      String           默认 1.0
                //商户编号                      String           商户后台获取
                //商户订单号                  String(32)           商户唯一订单号
                //订单金额                 String           单位:分， 必须大于 100 分
                //支付类型                  String           支付宝： alipay
                //请求ip                           String           请求IP， 部分支付请求IP与支付IP必须一致，所以请上传用户请求的真实IP
                //商品描述                   String           商品描述
                //异步通知 URL                 String           数据异步通知(可在后台配置，后台配置的通知地址优先)
                //同步跳转URL             String           不能带有任何参数(某些通道无效)
                //订单备注说明                 String(64)           可为空， 如果传递必须为字符串或者数据组合
                //支付来源                   String           wap : 普通 wap		weixinwap : 微信内 wap
                //md5签名串         sign             String           参照签名校验规则
            	put(version,"1.0");
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(orderId,channelWrapper.getAPI_ORDER_ID());
                put(totalMoney,  channelWrapper.getAPI_AMOUNT());
                put(tradeType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(ip,channelWrapper.getAPI_Client_IP());
                put(describe,"name");
                put(notify,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(redirectUrl,channelWrapper.getAPI_WEB_URL());
                put(remark,"name");
                //支付来源                   String           wap : 普通 wap		weixinwap : 微信内 wap
                put(fromtype,"wap");
            }
        };
        log.debug("[前海]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merId+"=").append(api_response_params.get(merId)).append("&");
        signSrc.append(orderId+"=").append(api_response_params.get(orderId)).append("&");
        signSrc.append(totalMoney+"=").append(api_response_params.get(totalMoney)).append("&");
        signSrc.append(tradeType+"=").append(api_response_params.get(tradeType)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toUpperCase();
        log.debug("[前海]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE,"Keep-Alive").trim();
        if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
            result.put(HTMLCONTEXT,resultStr);
            payResultList.add(result);
        }else{
             JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "0".equalsIgnoreCase(jsonResultStr.getString("code"))
                    && jsonResultStr.containsKey("object") && null!=jsonResultStr.getJSONObject("object")
                    && jsonResultStr.getJSONObject("object").containsKey("data")){
                if(HandlerUtil.isWapOrApp(channelWrapper)){
                    result.put(JUMPURL,  jsonResultStr.getJSONObject("object").getString("data"));
                }else{
                    //QIANHAI_BANK_WEBWAPAPP_WX_SM ,注：第三方只有这一个扫码类通道，扫码类变跳转后，第三方页面有截图图示。
                    result.put(JUMPURL,  jsonResultStr.getJSONObject("object").getString("data"));
                }
                payResultList.add(result);
            }else if(StringUtils.isBlank(resultStr) ){  throw new PayException(EMPTYRESPONSE);  }else {throw new PayException(resultStr); }

        }
        log.debug("[前海]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[前海]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}