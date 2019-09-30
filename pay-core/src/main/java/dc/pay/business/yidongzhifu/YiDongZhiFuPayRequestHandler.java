package dc.pay.business.yidongzhifu;

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
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("YIDONGZHIFU")
public final class YiDongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiDongZhiFuPayRequestHandler.class);

     private static final String      method  = "method";//         接口名称   是  String(20)  固定为mbupay.alipay.sqm
     private static final String      version  = "version";//         版本信息   否  String(8)  若不填写，则默认为2.0.0
     private static final String      charset  = "charset";//         字符集   否  String(8)  字符集，目前仅支持UTF-8
     private static final String      sign_type  = "sign_type";//         签名方式   否  String(8)  签名类型，目前仅支持MD5
     private static final String      appid  = "appid";//         应用ID   是  String(32)  支付平台分配的 APPID
     private static final String      mch_id  = "mch_id";//         商户号   是  String(32)  支付平台分配的商户号
     private static final String      nonce_str  = "nonce_str";//         随机字符串   是  String(32)  随机字符串，不长于32位。    推荐随机数生成算法
     private static final String      sign  = "sign";//         签名   是  String(32)  签名,详细签名方法见1.2 节
     private static final String      body  = "body";//         商品描述   是  String(128)  对交易或商品的描述
     private static final String      out_trade_no  = "out_trade_no";//         商户订单号   是  String(64)  商户系统 内 部 的订单    号,64 个字符内、可包含字    母。注意：请务必确保该订单    号在商户系统的唯一性。
     private static final String      total_fee  = "total_fee";//         总金额   是  Int  订单总金额，单位为分，不 能带小数点
     private static final String      notify_url  = "notify_url";//         通知地址   否  String(256)  接收支付宝支付结果通知





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接商户号和appid,如：商户号&appid");
        }
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(method,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(version,"2.0.0");
            payParam.put(charset,"UTF-8");
            payParam.put(sign_type,"MD5");
            payParam.put(appid,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(mch_id,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(nonce_str,"1234567890");
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,channelWrapper.getAPI_AMOUNT());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[移动支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = SignUtil.sign(params, channelWrapper.getAPI_KEY());
        log.debug("[移动支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
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

                String reqXml = XmlUtils.beanToXml(payParam);
                System.out.println(reqXml);
                String rtnContent = HttpUtil.sendPost(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqXml);

                System.out.println("返回:" + rtnContent);
                Map<String, String> rtnMap = XmlUtils.xmlToMap(rtnContent);
                String sign = rtnMap.get("sign");
                rtnMap.remove("sign");
                if ("SUCCESS".equals(rtnMap.get("result_code"))) {
                    if (SignUtil.verify(rtnMap, channelWrapper.getAPI_KEY(), sign)) {
                        // 验签通过
                        result.put(JUMPURL, rtnMap.get("code_url"));
                        payResultList.add(result);

                    } else {
                        throw new PayException("请求验签失败："+rtnContent);
                    }
                } else {
                    throw new PayException(rtnContent);
                }




//                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
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
             log.error("[移动支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[移动支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[移动支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}