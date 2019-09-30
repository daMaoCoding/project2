package dc.pay.business.caifubao;

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
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("CAIFUBAO")
public final class CaiFuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CaiFuBaoPayRequestHandler.class);

    private static final String P_UserId = "P_UserId";
    private static final String P_OrderId = "P_OrderId";
    private static final String P_FaceValue = "P_FaceValue";
    private static final String P_CustormId = "P_CustormId";
    private static final String P_Type = "P_Type";
    private static final String P_RequestType = "P_RequestType";
    private static final String P_SDKVersion = "P_SDKVersion";
    private static final String P_Subject = "P_Subject";
    private static final String P_Result_URL = "P_Result_URL";
    private static final String P_Notify_URL = "P_Notify_URL";
    private static final String P_PostKey = "P_PostKey";
    private static final String NM = "1001";
//    private static final String QRCONTEXT = "QRCONTEXT";
//    private static final String PARSEHTML = "PARSEHTML";
//    private static final String JUMPURL = "JUMPURL";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String p_custormId =  NM + "_" + CaiFuBaoUtil.Md5(channelWrapper.getAPI_MEMBERID() + "|" + channelWrapper.getAPI_KEY() + "|" + NM);

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(P_UserId, channelWrapper.getAPI_MEMBERID());
                put(P_OrderId, channelWrapper.getAPI_ORDER_ID());
                put(P_FaceValue, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(P_CustormId,p_custormId);
                put(P_Type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(P_RequestType,"0");
                put(P_SDKVersion,"3.1.3");
                put(P_Subject,"PAY");
                put(P_Result_URL,channelWrapper.getAPI_WEB_URL());
                put(P_Notify_URL, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[彩富宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        String signStr = payParam.get(P_UserId) + "|" + payParam.get(P_OrderId) + "|" + payParam.get(P_FaceValue) + "|" + payParam.get(P_Type) + "|" + payParam.get(P_SDKVersion) + "|"  + payParam.get(P_RequestType);
        String pay_md5sign = CaiFuBaoUtil.Md5(signStr + "|" + channelWrapper.getAPI_KEY());
        // pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[彩富宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        HashMap<String, String> result = Maps.newHashMap();
        if(HandlerUtil.isWY(channelWrapper)|| HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper) ||  HandlerUtil.isWebJdKjzf(channelWrapper)) {
            result.put(HTMLCONTEXT,HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else {
            String firstPayresult = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
            if (firstPayresult.length() < 10) {
                log.error("[彩富宝]3.发送支付请求，及获取支付请求结果：" + firstPayresult + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(firstPayresult);
            }
            JSONObject resJSON = JSON.parseObject(firstPayresult);
            if (resJSON.containsKey("status") && resJSON.getString("status").equals("0") && resJSON.containsKey("code_url") && StringUtils.isNotBlank(resJSON.getString("code_url"))) {
				String wxQRContext =resJSON.getString("code_url");
				result.put(QRCONTEXT, HandlerUtil.UrlDecode(wxQRContext));
				result.put(PARSEHTML, firstPayresult);
            } else {
                throw new PayException(firstPayresult);
            }
        }
		payResultList.add(result);
        log.debug("[彩富宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[彩富宝]-[请求支付]-4.处理请求支付成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}