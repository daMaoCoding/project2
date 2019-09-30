package dc.pay.business.xinduobao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * 04 30, 2019
 */
@RequestPayHandler("XINDUOBAO")
public final class XinDuoBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinDuoBaoPayRequestHandler.class);

//    输入项					输入项名称			属性				注释				数据 类型
//    orderid				订单号				M				建议： 日期(YYYYMMDDHHMMSS)+商户首字母（4字节）+商户交易流水号（12字节））	as..32
//    value					订单金额				M				以元为单位。	as..4
//    parter				用户编号				M				商户号	As..20
//    type					业务代码				M				详见附录	N4
//    callbackurl			后台通知地址			M				商户接收支付结果通知的后台地址	as..256
//    hrefbackurl			前台页面通知地址		M				商户接收支付结果通知的页面地址	as..256
//    attach				附加信息域			C				备注字段原样返回	As..128
//    sign					签名值				M				Md5(parter={}&type={}&orderid={}&callbackurl={}key)	as..256

    private static final String orderid               		="orderid";
    private static final String value           			="value";
    private static final String parter           			="parter";
    private static final String type           				="type";
    private static final String callbackurl          		="callbackurl";
    private static final String hrefbackurl             	="hrefbackurl";
    private static final String attach            			="attach";
    
    private static final String key                 ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(parter, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(value,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(hrefbackurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[新多宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s", 
        		parter+"="+api_response_params.get(parter)+"&",
        		type+"="+api_response_params.get(type)+"&",
        		orderid+"="+api_response_params.get(orderid)+"&",
        		callbackurl+"="+api_response_params.get(callbackurl),
        		channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新多宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[新多宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新多宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}