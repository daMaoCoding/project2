package dc.pay.business.fuhengzhifu;

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

import java.util.*;

/**
 * @author Cobby
 * June 04, 2019
 */
@RequestPayHandler("FUHENGZHIFU")
public final class FuHengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FuHengZhiFuPayRequestHandler.class);


    private static final String mchid            ="mchid";    //   商户ID        是    商户在平台的 商户ID号
    private static final String mchno            ="mchno";    //   商户订单号     是    商户自己生成的订单号 由英文、数字、_、- 、组成
    private static final String tradetype        ="tradetype";//   订单类型       是    alipay 表示发起支付宝扫码支付    alipayh5 表示发起支付宝h5支付
    private static final String totalfee         ="totalfee"; //   支付金额       是    订单需要支付的金额，单位：分（人民币）
    private static final String descrip          ="descrip";  //   订单描述       是     长度不能超过127位 可以由中文、英文、数字、_、- 、
    private static final String attach           ="attach";   //   附加数据       否     可为空， 商户的附加数据，回调的时候会原样返回，
    private static final String clientip         ="clientip"; //   终端IP         是    长度不能超过46位， 订单生成的机器 IP
    private static final String notifyurl        ="notifyurl";//   异步通知地址    是     接收平台异步通知回调地址，通知url必须为直接可访问的url，不能携带参数。
    private static final String returnurl        ="returnurl";//   同步通知地址    否     可为空， 订单支付成功后同步跳转的地址,

    private static final String key        ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchid, channelWrapper.getAPI_MEMBERID());
                put(mchno,channelWrapper.getAPI_ORDER_ID());
                put(tradetype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(totalfee,  channelWrapper.getAPI_AMOUNT());
                put(descrip,channelWrapper.getAPI_ORDER_ID());
                put(attach,channelWrapper.getAPI_ORDER_ID());
                put(clientip,channelWrapper.getAPI_Client_IP());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[博恒支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //mchid=10000&mchno=201803051730&tradetype=alipayh5&totalfee=1000&descrip=xxxx&attach=xxxx&clientip=127.0.0.1&
//        notifyurl=http://xxxx.cn/wxpay/pay.php&returnurl=http://xxxx.cn/wxpay/pay.php&key=c4b70b766ea78fe1689f4e4e1afa291a
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(mchid+"=").append(api_response_params.get(mchid)).append("&");
        signSrc.append(mchno+"=").append(api_response_params.get(mchno)).append("&");
        signSrc.append(tradetype+"=").append(api_response_params.get(tradetype)).append("&");
        signSrc.append(totalfee+"=").append(api_response_params.get(totalfee)).append("&");
        signSrc.append(descrip+"=").append(api_response_params.get(descrip)).append("&");
        signSrc.append(attach+"=").append(api_response_params.get(attach)).append("&");
        signSrc.append(clientip+"=").append(api_response_params.get(clientip)).append("&");
        signSrc.append(notifyurl+"=").append(api_response_params.get(notifyurl)).append("&");
        signSrc.append(returnurl+"=").append(api_response_params.get(returnurl)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[博恒支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            
        } catch (Exception e) {
            log.error("[博恒支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[博恒支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[博恒支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}