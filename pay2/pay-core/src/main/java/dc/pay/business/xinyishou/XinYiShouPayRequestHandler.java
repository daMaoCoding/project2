package dc.pay.business.xinyishou;

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
 * 07 02, 2019
 */
@RequestPayHandler("XINYISHOU")
public final class XinYiShouPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinYiShouPayRequestHandler.class);

//    参数名称		变量名			类型长度			是否可空			说明
//    版本号			version			varchar(5)		默认1.0
//    商户编号		customerid		int(8)			商户后台获取
//    商户订单号		sdorderno		varchar(20)		
//    订单金额		total_fee		decimal(10,2)	精确到小数点后两位，例如10.24
//    支付编号		paytype			varchar(10)		详见附录1
//    银行编号		bankcode		varchar(10)		网银直连不可为空，其他支付方式可为空	详见附录2
//    异步通知URL		notifyurl		varchar(50)		不能带有任何参数
//    同步跳转URL		returnurl		varchar(50)		不能带有任何参数
//    订单备注说明	remark			varchar(50)		Y	可为空
//    获取微信二维码	get_code		tinyint(1)		Y	如果只想获取被扫二维码，请设置get_code=1
//    md5签名串		sign			varchar(32)		参照md5签名说明

    private static final String version               	="version";
    private static final String customerid            	="customerid";
    private static final String sdorderno           	="sdorderno";
    private static final String total_fee           	="total_fee";
    private static final String paytype          		="paytype";
    private static final String bankcode              	="bankcode";
    private static final String notifyurl            	="notifyurl";
    private static final String returnurl           	="returnurl";
    
    private static final String remark            		="remark";
    private static final String get_code                ="get_code";
    private static final String sign                 	="sign";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(customerid, channelWrapper.getAPI_MEMBERID());
                put(sdorderno,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
                put(version,"1.0");
            }
        };
        log.debug("[新易收付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signStr=String.format("%s%s%s%s%s%s%s",
        		version+"="+api_response_params.get(version)+"&",
        		customerid+"="+api_response_params.get(customerid)+"&",
        		total_fee+"="+api_response_params.get(total_fee)+"&",
        		sdorderno+"="+api_response_params.get(sdorderno)+"&",
        		notifyurl+"="+api_response_params.get(notifyurl)+"&",
        		returnurl+"="+api_response_params.get(returnurl)+"&",
        		channelWrapper.getAPI_KEY()
        		);
        String signMD5 = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[新易收付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[新易收付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新易收付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}