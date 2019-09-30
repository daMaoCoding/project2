package dc.pay.business.qianjiezhifu;

import java.util.*;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author cobby
 * Jan 16, 2019
 */
@RequestPayHandler("QIANJIEZHIFU")
public final class QianJieZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianJieZhiFuPayRequestHandler.class);

    private static final String partner	  ="partner"; //商户ID		N	Y	商户id，由支付分配
    private static final String paytype	  ="paytype";//银行类型		N	N	支付类型，具体请参考附录1
    private static final String paymoney	  ="paymoney";//金额		N	Y	单位元（人民币），2位小数，最小支付金额为10.00
    private static final String sdorderno	  ="sdorderno";//商户订单号		N	Y	商户系统订单号，该订单号将作为接口的返回数据。该值需在商户系统内唯一，系统暂时不检查该值是否唯一
    private static final String notifyurl	  ="notifyurl";//下行异步通知地址		N	Y	下行异步通知过程的返回地址，需要以http://开头且没有任何参数
    private static final String returnurl	="returnurl";//下行同步通知地址		N	Y	下行同步通知过程的返回地址(在支付完成后接口将会跳转到的商户系统连接地址)。

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String sign  ="sign";//32位小写MD5签名值，utf-8编码

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner, channelWrapper.getAPI_MEMBERID());
	            put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            put(paymoney,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
	            put(sdorderno,channelWrapper.getAPI_ORDER_ID());
	            put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
	            put(returnurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[千捷支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }


     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
//	     notifyurl={}&partner={}&paymoney={}&returnurl={}&sdorderno={}&key
//	     其中，key为商户签名。
	     List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!paytype.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }

        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[千捷支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
	        //  禁止web扫码
	        if ((handlerUtil.isWEBWAPAPP_SM(channelWrapper) && HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())) ||
			        channelWrapper.getAPI_ORDER_ID().startsWith("T") || handlerUtil.isWapOrApp(channelWrapper)) {
		        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
	        }else {
		        throw new PayException("请在APP或者WAP应用上使用通道......");
	        }
            
        } catch (Exception e) {
            log.error("[千捷支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[千捷支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[千捷支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}