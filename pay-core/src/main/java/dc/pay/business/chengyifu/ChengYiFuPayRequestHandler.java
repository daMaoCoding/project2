package dc.pay.business.chengyifu;

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
import dc.pay.business.yuerongzhuang.EncryptUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("CHENGYIZHIFU")
public final class ChengYiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChengYiFuPayRequestHandler.class);

    private static final String     mchtid = "mchtid";//    商户ID    int        商户ID
    private static final String     reqdata = "reqdata";//    UrlEncode编码



    private static final String     p1_mchtid           = "p1_mchtid";//商户ID        是    int    是        商户ID,由支付分配
    private static final String     p2_paytype          = "p2_paytype";//支付方式        是    String(20)    是    FASTPAY    支付网关(参见附录说明4.3)
    private static final String     p3_paymoney         = "p3_paymoney";//支付金额        是    decimal    是    0.01    订单金额最小0.01(以元为单位）
    private static final String     p4_orderno          = "p4_orderno";//商户平台唯一订单号        是    String(50)    是        商户系统内部订单号，要求50字符以内，同一商户号下订单号唯一
    private static final String     p5_callbackurl      = "p5_callbackurl";//商户异步回调通知地址        是    String(200)    是        商户异步回调通知地址
    private static final String     p6_notifyurl        = "p6_notifyurl";//商户同步通知地址        否    String(200)    是        商户同步通知地址
    private static final String     p7_version          = "p7_version";//版本号        是    String(4)    是    v2.9    v2.9
    private static final String     p8_signtype         = "p8_signtype";//签名加密方式        是    int    是    2    值为2
    private static final String     p9_attach           = "p9_attach";//备注信息，上行中attach原样返回        否    String(128)    是        备注信息，上行中attach原样返回
    private static final String     p10_appname         = "p10_appname";//分成标识        否    Strng(25)    是
    private static final String     p11_isshow          = "p11_isshow";//是否显示收银台        是    int    是    0    0
    private static final String     p12_orderip         = "p12_orderip";//商户的用户下单IP        否    String(20)    是    192.168.10.1    商户的用户下单IP
    private static final String     p13_memberid        = "p13_memberid";//商户系统用户唯一标识        是    String(5-32)    是    123456    商户用户ID（唯一）


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[诚意支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&交易MD5秘钥" );
            throw new PayException("[诚意支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&交易MD5秘钥" );
        }
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(p1_mchtid,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(p2_paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(p3_paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(p4_orderno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(p5_callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(p6_notifyurl,"");
            payParam.put(p7_version,"v2.9");
            payParam.put(p8_signtype,"2");
            payParam.put(p9_attach,"");
            payParam.put(p10_appname,"");
            payParam.put(p11_isshow,"0");
            payParam.put(p12_orderip, channelWrapper.getAPI_Client_IP());
            payParam.put(p13_memberid,HandlerUtil.randomStr(8));
        }
        log.debug("[诚意支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(p1_mchtid+"=").append(api_response_params.get(p1_mchtid)).append("&");
        signSrc.append(p2_paytype+"=").append(api_response_params.get(p2_paytype)).append("&");
        signSrc.append(p3_paymoney+"=").append(api_response_params.get(p3_paymoney)).append("&");
        signSrc.append(p4_orderno+"=").append(api_response_params.get(p4_orderno)).append("&");
        signSrc.append(p5_callbackurl+"=").append(api_response_params.get(p5_callbackurl)).append("&");
        signSrc.append(p6_notifyurl+"=").append(api_response_params.get(p6_notifyurl)).append("&");
        signSrc.append(p7_version+"=").append(api_response_params.get(p7_version)).append("&");
        signSrc.append(p8_signtype+"=").append(api_response_params.get(p8_signtype)).append("&");
        signSrc.append(p9_attach+"=").append(api_response_params.get(p9_attach)).append("&");
        signSrc.append(p10_appname+"=").append(api_response_params.get(p10_appname)).append("&");
        signSrc.append(p11_isshow+"=").append(api_response_params.get(p11_isshow)).append("&");
        signSrc.append(p12_orderip+"=").append(api_response_params.get(p12_orderip)).append("&");
        signSrc.append(p13_memberid+"=").append(api_response_params.get(p13_memberid));
        signSrc.append(channelWrapper.getAPI_MEMBERID().split("&")[1]);
        String paramsStr = signSrc.toString();

        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[诚意支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String, String> payParam1 = Maps.newHashMap();
        payParam1.put(mchtid,channelWrapper.getAPI_MEMBERID().split("&")[0]);
        try {
            String dataStr = EncryptUtil.encryptRSAByPublicKey(channelWrapper.getAPI_PUBLIC_KEY(), JSON.toJSONString(payParam));
            dataStr = URLEncoder.encode(dataStr,"UTF-8");
            payParam1.put(reqdata,dataStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        try {
            if (HandlerUtil.isYLKJ(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam1).toString());
            } else {
                String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam1, "UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[诚意支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("rspCode") && "1".equalsIgnoreCase(jsonObject.getString("rspCode"))
                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    jsonObject = JSONObject.parseObject(jsonObject.getString("data"));
                    String code_url = jsonObject.getString("r6_qrcode");
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                    if (handlerUtil.isWapOrApp(channelWrapper)) {
                        result.put(JUMPURL, code_url);
                    }else{
                        result.put(QRCONTEXT, code_url);
                    }
                }else {
                    log.error("[诚意支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }

        } catch (Exception e) {
             log.error("[诚意支付]3.3. 发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        payResultList.add(result);
        log.debug("[诚意支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[诚意支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}