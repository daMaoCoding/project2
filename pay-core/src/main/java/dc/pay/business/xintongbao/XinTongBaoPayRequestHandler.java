package dc.pay.business.xintongbao;

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
 * @author sunny
 * Dec 18, 2019
 */
@RequestPayHandler("XINTONGBAO")
public final class XinTongBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinTongBaoPayRequestHandler.class);

//    参数名称		参数名				类型				*为必填
//    版本号			bb					varchar(5)		*
//    商户编号		shid				int(8)			*
//    商户订单号		ddh					varchar(20)		*
//    订单金额（保留2位小数）	je				decimal(18,2)	*
//    支付通道		zftd				varchar(10)		*
//    银行代码		bankcode			varchar(10)	
//    异步通知URL		ybtz				varchar(255)	*
//    同步跳转URL		tbtz				varchar(255)	*
//    订单名称		ddmc				varchar(50)		*
//    订单备注		ddbz				varchar(50)		*请填写付款人姓名！！！
//    md5签名串		sign				varchar(32)		*
    private static final String bb               	="bb";
    private static final String shid           		="shid";
    private static final String ddh           		="ddh";
    private static final String je           		="je";
    private static final String zftd          		="zftd";
    private static final String bankcode            ="bankcode";
    private static final String ybtz            	="ybtz";
    private static final String tbtz           		="tbtz";
    private static final String ddmc            	="ddmc";
    private static final String ddbz                ="ddbz";
    private static final String sign                ="sign";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(shid, channelWrapper.getAPI_MEMBERID());
                put(ddh,channelWrapper.getAPI_ORDER_ID());
                put(je,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ybtz,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(zftd,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(tbtz,channelWrapper.getAPI_WEB_URL());
                put(ddmc,channelWrapper.getAPI_ORDER_ID());
                put(ddbz,channelWrapper.getAPI_ORDER_ID());
                put(bb,"1.0");
            }
        };
        log.debug("[新通宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s",
        		shid+"="+api_response_params.get(shid)+"&",
        		bb+"="+api_response_params.get(bb)+"&",
        		zftd+"="+api_response_params.get(zftd)+"&",
        		ddh+"="+api_response_params.get(ddh)+"&",
        		je+"="+api_response_params.get(je)+"&",
        		ddmc+"="+api_response_params.get(ddmc)+"&",
        		ddbz+"="+api_response_params.get(ddbz)+"&",
        		ybtz+"="+api_response_params.get(ybtz)+"&",
        		tbtz+"="+api_response_params.get(tbtz)+"&",
        		channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新通宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[新通宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新通宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}