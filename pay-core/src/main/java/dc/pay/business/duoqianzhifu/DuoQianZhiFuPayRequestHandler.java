package dc.pay.business.duoqianzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * @author Cobby
 * Mar 12, 2019
 */
@RequestPayHandler("DUOQIANZHIFU")
public final class DuoQianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DuoQianZhiFuPayRequestHandler.class);

    private static final String userid          ="userid"; //  商户ID      是    商户在中的唯一标识，在直通车中获得
    private static final String orderid         ="orderid";//  商户订单号   是    提交的订单号在商户系统中必须唯一
    private static final String money           ="money";  //  订单金额     是    订单金额 （单位：元）
    private static final String url             ="url";    //  商户接收后台返回扫码结果的地址     是    扫码成功后，向该网址发送三次成功通知。
    private static final String aurl            ="aurl";   //  商户商城取货地址     否    用户扫码成功，跳转到取货地址。可为空，为空则不跳转。
    private static final String bankid          ="bankid"; //  银行编号     否    银行的编号，详见附录1
    private static final String sign            ="sign";   //  签名数据     是    32位小写的组合加密验证串
    private static final String ext             ="ext";    //  商户扩展信息，返回时原样返回，此参数如用到中文，请注意转码
    private static final String sign2           ="sign2";  //  签名数据     否    32位小写的组合加密验证串，此参数非常重要，请务必重视，如果有开发能力，请一定加上！！！

    private static final String keyvalue        ="keyvalue";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(userid, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(aurl,channelWrapper.getAPI_WEB_URL());
                put(bankid,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(ext,"ext");
            }
        };
        log.debug("[多乾支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //sign:userid={}&orderid={}&bankid={}&keyvalue={}
        StringBuffer signSrc1= new StringBuffer();
        signSrc1.append(userid+"=").append(api_response_params.get(userid)).append("&");
        signSrc1.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc1.append(bankid+"=").append(api_response_params.get(bankid)).append("&");
        signSrc1.append(keyvalue+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr1 = signSrc1.toString();
        String sign1Md5 = HandlerUtil.getMD5UpperCase(paramsStr1).toLowerCase();

        //sign2:money={}&userid={}&orderid={}&bankid={}&keyvalue={}
        StringBuffer signSrc2= new StringBuffer();
        signSrc2.append(money+"=").append(api_response_params.get(money)).append("&");
        signSrc2.append(userid+"=").append(api_response_params.get(userid)).append("&");
        signSrc2.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc2.append(bankid+"=").append(api_response_params.get(bankid)).append("&");
        signSrc2.append(keyvalue+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr2= signSrc2.toString();
        String sign2Md5 = HandlerUtil.getMD5UpperCase(paramsStr2).toLowerCase();
        StringBuffer signMd5Str= new StringBuffer();
        signMd5Str.append(sign1Md5).append("&").append(sign2Md5);
        String signMd5 = signMd5Str.toString();
        log.debug("[多乾支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(sign, pay_md5sign.split("&")[0]);
        payParam.put(sign2, pay_md5sign.split("&")[1]);

        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));

        } catch (Exception e) {
            log.error("[多乾支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[多乾支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[多乾支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}