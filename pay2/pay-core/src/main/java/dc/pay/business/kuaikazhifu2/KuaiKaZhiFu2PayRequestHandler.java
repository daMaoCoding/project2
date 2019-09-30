package dc.pay.business.kuaikazhifu2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author mikey
 * Jun 4, 2019
 */
@RequestPayHandler("KUAIKAZHIFU2")
public final class KuaiKaZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiKaZhiFu2PayRequestHandler.class);

    /*
    	参数名称			参数含义		是否必填		参与签名	参数说明
    pay_memberid		商户号			是			是		平台分配商户号
    pay_orderid			订单号			是			是		上送订单号唯一, 字符长度20
    pay_types			聚合类型		是			否		pay_bankcode是919是 pay_types=1微信0支付宝
    pay_applydate		提交时间		是			是		时间格式：2016-12-26 18:18:18
    pay_bankcode		银行编码		是			是		参考后续说明
    pay_notifyurl		服务端通知		是			是		服务端返回地址.（POST返回数据）
    pay_callbackurl		页面跳转通知		是			是		页面跳转返回地址（POST返回数据）
    pay_amount			订单金额		是			是		商品金额
    pay_md5sign			MD5签名		是			否		请看MD5签名字段格式
    pay_productname		商品名称		是			否	
     */
	private static final String pay_memberid		=	"pay_memberid";			//商户号			
    private static final String pay_orderid			=	"pay_orderid";			//订单号			
    private static final String pay_types			=	"pay_types";			//聚合类型		
    private static final String pay_applydate		=	"pay_applydate";		//提交时间		
    private static final String pay_bankcode		=	"pay_bankcode";			//银行编码		
    private static final String pay_notifyurl		=	"pay_notifyurl";		//服务端通知		
    private static final String pay_callbackurl		=	"pay_callbackurl";		//页面跳转通知	
    private static final String pay_amount			=	"pay_amount";			//订单金额		
    private static final String pay_productname		=	"pay_productname";		//商品名称
    private static final String key        			=	"key";

    
    /**
     * 		參數封裝
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>();
    	payParam.put(pay_memberid, channelWrapper.getAPI_MEMBERID());
    	payParam.put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
    	payParam.put(pay_types,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
    	payParam.put(pay_applydate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
    	payParam.put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
    	payParam.put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    	payParam.put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
    	payParam.put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
    	payParam.put(pay_productname,"name");
        log.debug("[快卡支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 		簽名製作
     */
     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);	//小到大排序
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_types.equals(paramKeys.get(i)) && !pay_productname.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[快卡支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

     /**
      * 	發送請求
      */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[快卡支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 		封裝結果
     */
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
        log.debug("[快卡支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}