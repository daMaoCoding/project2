package dc.pay.business.mianqianzhifu;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.tongsao.TongSaoRequestHandler;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @author Cobby
 * June 12, 2019
 */
@RequestPayHandler("MIANQIANZHIFU")
public final class MianQianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongSaoRequestHandler.class);

    private static final String merchant_id          ="merchant_id";  //   必填    商户ID、在平台首页右边获取商户ID    10000
    private static final String content_type         ="content_type"; //   必填    请求过程中返回的网页类型，form    json
    private static final String pay_type             ="pay_type";     //   必填    支付方式，支付宝:alipay，微信:wechat    wechat
    private static final String out_trade_no         ="out_trade_no"; //   必填    商户订单号，需保证在商户平台唯一    2018062668945
    private static final String amount               ="amount";       //   必填    支付金额    1.00
    private static final String robin                ="robin";        //   必填    轮训，1：多个支付宝/微信账户轮询，2：只是用单个支付宝/微信    1
    private static final String keyId                ="keyId";        //   选填    robin为2时必填    设备KEY，在账户管理中的DEVICE Key    785D2397
    private static final String notify_url           ="notify_url";   //   必填    异步通知地址，在支付完成时，本平台服务器系统会自动向该地址发起一条支付成功的回调请求, 对接方接收到回调后，
    private static final String return_url            ="return_url";  //   选填    支付成功后网页自动跳转地址，仅在网页类型为form下有效    http://pay.9fubaopay.com/Return/Success


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_id, channelWrapper.getAPI_MEMBERID());
                put(content_type,"form");
                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(robin,"1");
                put(keyId,"");
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[免签支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //amount={}&content_type={}&merchant_id={}&notify_url={}&out_trade_no={}&pay_type={}{key}
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(content_type+"=").append(api_response_params.get(content_type)).append("&");
        signSrc.append(merchant_id+"=").append(api_response_params.get(merchant_id)).append("&");
        signSrc.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
        signSrc.append(out_trade_no+"=").append(api_response_params.get(out_trade_no)).append("&");
        signSrc.append(pay_type+"=").append(api_response_params.get(pay_type));
        signSrc.append(channelWrapper.getAPI_KEY());

        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[免签支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[免签支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[免签支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[免签支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}