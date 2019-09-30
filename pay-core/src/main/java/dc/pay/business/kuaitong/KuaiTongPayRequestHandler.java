package dc.pay.business.kuaitong;

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
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
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
@RequestPayHandler("KUAITONG")
public final class KuaiTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiTongPayRequestHandler.class);

//    参数					说明					批注
//    MerID					商户号ID				由 快通支付 提供
//    MerTradeID			店家交易编号			店家自行设定，不得小于6个字符，不得重复。只支持英文及数字
//    MerProductID			店家商品代号			店家自行设定，不得小于6个字符。只支持英文及数字
//    MerUserID				店家消费者ID			店家自行设定，限英文或数字组合，最少6个字符
//    Amount				交易金额				目前只支持整数金额。例如 1.00,10.00,20.00,30.00, 100.00…
//    TradeDesc				交易描述	
//    ItemName				商品名称	
//    Sign					MD5签名串			编码方式请参阅备注一
//    NotifyUrl				交易异步回调网址		需含带http或https的标头。当使用者支付成功时，会呼叫该网址

    private static final String MerID                	="MerID";
    private static final String MerTradeID           	="MerTradeID";
    private static final String MerProductID            ="MerProductID";
    private static final String MerUserID           	="MerUserID";
    private static final String Amount          		="Amount";
    private static final String TradeDesc               ="TradeDesc";
    private static final String ItemName            	="ItemName";
    private static final String NotifyUrl           	="NotifyUrl";
    private static final String sign                ="sign";
    private static final String key                 ="key";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerID, channelWrapper.getAPI_MEMBERID());
                put(MerTradeID,channelWrapper.getAPI_ORDER_ID());
                put(Amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(MerProductID,channelWrapper.getAPI_ORDER_ID());
                put(MerUserID,channelWrapper.getAPI_ORDER_ID());
                put(TradeDesc,channelWrapper.getAPI_ORDER_ID());
                put(ItemName,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[快通支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s%s", 
        		api_response_params.get(MerID),
        		api_response_params.get(MerTradeID),
        		api_response_params.get(MerProductID),
        		api_response_params.get(MerUserID),
        		api_response_params.get(Amount),
        		channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[快通支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[快通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[快通支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}