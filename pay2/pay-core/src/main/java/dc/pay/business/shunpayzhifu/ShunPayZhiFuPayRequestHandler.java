package dc.pay.business.shunpayzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.yunqingzhifu.YunQingZhiFuPayRequestHandler;
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
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("SHUNPAYZHIFU")
public final class ShunPayZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YunQingZhiFuPayRequestHandler.class);

//  变量名称				变量名				长度定义
//  接口名字				apiName				ans(.30)
//  接口版本				apiVersion			ans(7)
//  商户(合作伙伴)ID		platformID			ans(..16)
//  商户账号				merchNo				ans(..32)
//  商户订单号			orderNo				n(..32)
//  交易日期				tradeDate			n(8)
//  订单金额				amt					n(12,2)
//  支付结果通知地址		merchUrl			ans(..128)
//  商户参数				merchParam			ans(..256)
//  交易摘要				tradeSummary		ans（..120）
//  签名					signMsg				ans（..300）
//  银行代码				bankCode			n(..8)
//  选择支付方式			choosePayType		n(..2)

private static final String apiName               ="apiName";
private static final String apiVersion            ="apiVersion";
private static final String platformID            ="platformID";
private static final String merchNo               ="merchNo";
private static final String orderNo          	  ="orderNo";
private static final String tradeDate             ="tradeDate";
private static final String amt            		  ="amt";
private static final String merchUrl              ="merchUrl";
private static final String merchParam            ="merchParam";
private static final String tradeSummary          ="tradeSummary";
private static final String bankCode          	  ="bankCode";
private static final String choosePayType         ="choosePayType";

private static final String signType            ="signType";
private static final String signMsg             ="signMsg";
private static final String key                 ="key";



@Override
protected Map<String, String> buildPayParam() throws PayException {
    Map<String, String> payParam = new TreeMap<String, String>() {
        {
            put(platformID, channelWrapper.getAPI_MEMBERID());
            put(merchNo, channelWrapper.getAPI_MEMBERID());
            put(orderNo,channelWrapper.getAPI_ORDER_ID());
            put(amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            put(merchUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            put(tradeDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            put(apiVersion,"1.0.0.0");
            put(tradeSummary,channelWrapper.getAPI_ORDER_ID());
            put(merchParam,channelWrapper.getAPI_ORDER_ID());
            if(HandlerUtil.isWapOrApp(channelWrapper)){
          	  put(apiName, "WAP_PAY_B2C");
            }else{
          	  put(apiName, "WEB_PAY_B2C"); 
            }
            if(HandlerUtil.isWY(channelWrapper)&&!HandlerUtil.isWebWyKjzf(channelWrapper)){
          	  put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
          	  put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            }else{
          	  put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
            
        }
    };
    log.debug("[顺通PAY支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
    return payParam;
}

 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s%s", 
  		  apiName+"="+api_response_params.get(apiName)+"&",
  		  apiVersion+"="+api_response_params.get(apiVersion)+"&",
  		  platformID+"="+api_response_params.get(platformID)+"&",
  		  merchNo+"="+api_response_params.get(merchNo)+"&",
  		  orderNo+"="+api_response_params.get(orderNo)+"&",
  		  tradeDate+"="+api_response_params.get(tradeDate)+"&",
  		  amt+"="+api_response_params.get(amt)+"&",
  		  merchUrl+"="+api_response_params.get(merchUrl)+"&",
  		  merchParam+"="+api_response_params.get(merchParam)+"&",
  		  tradeSummary+"="+api_response_params.get(tradeSummary),
  		  channelWrapper.getAPI_KEY()
    );
    String paramsStr = signSrc.toString();
    String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
    log.debug("[顺通PAY支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
    return signMD5;
}

protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
    HashMap<String, String> result = Maps.newHashMap();
    ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
    result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
    payResultList.add(result);
    log.debug("[顺通PAY支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
    log.debug("[顺通PAY支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
    return requestPayResult;
}
}