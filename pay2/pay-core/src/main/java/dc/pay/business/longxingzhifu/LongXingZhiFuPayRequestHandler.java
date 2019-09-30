package dc.pay.business.longxingzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Mikey
 * Jun 13, 2019
 */
@Slf4j
@RequestPayHandler("LONGXINGZHIFU")
public final class LongXingZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LongXingZhiFuPayRequestHandler.class);
/*
    参数名称		变量名				类型长度			是否必填		说明
    版本号		version			varchar(5)		Y			默认1.0
    商户编号		customerid		int(8)			Y			商户后台获取
    商户订单号		sdorderno		varchar(20)		Y			商户订单号
    订单金额		total_fee		decimal(10,2)	Y			精确到小数点后两位，例如10.24
    支付编号		paytype			varchar(10)		Y			详见附录1
    异步通知URL	notifyurl		varchar(50)		Y			不能带有任何参数
    同步跳转URL	returnurl		varchar(50)		Y			不能带有任何参数
    支付模式		pay_model		Int(1)			Y			支付宝2必填 1: 银行转账 2：支付宝转红包, 3:口令支付,4:钉钉 5：点点虫 6:旺信红包 8：撩呗红包 9:支付宝通码 默认 2 红包转账
 md5签名串		sign			varchar(32)		Y			参照md5签名说明
*/

    private static final String version		= "version";		//varchar(5)		Y			默认1.0
    private static final String customerid	= "customerid";		//int(8)			Y			商户后台获取
    private static final String sdorderno	= "sdorderno";		//varchar(20)		Y			商户订单号
    private static final String total_fee	= "total_fee";		//decimal(10,2)		Y			精确到小数点后两位，例如10.24
    private static final String paytype		= "paytype";		//varchar(10)		Y			详见附录1
    private static final String notifyurl	= "notifyurl";		//varchar(50)		Y			不能带有任何参数
    private static final String returnurl	= "returnurl";		//varchar(50)		Y			不能带有任何参数
    private static final String pay_model	= "pay_model";		//Int(1)			Y			支付宝2必填 1: 银行转账 2：支付宝转红包, 3:口令支付,4:钉钉 5：点点虫 6:旺信红包 8：撩呗红包 9:支付宝通码 默认 2 红包转账
    private static final String sign		= "sign";			//varchar(32)		Y			参照md5签名说明

    
    /**
     *	 參數封裝
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = Maps.newHashMap();
    	
    	payParam.put(version,"1.0");
    	payParam.put(customerid,channelWrapper.getAPI_MEMBERID());
    	payParam.put(sdorderno,channelWrapper.getAPI_ORDER_ID());
    	payParam.put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
    	payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
    	payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    	payParam.put(returnurl,channelWrapper.getAPI_WEB_URL());
    	payParam.put(pay_model,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
    	
        log.debug("[龙杏支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 	簽名製作
     */
	protected String buildPaySign(Map<String, String> params) throws PayException {
		String pay_md5sign = null;
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(version+"=").append(params.get(version)).append("&");
        signSrc.append(customerid+"=").append(params.get(customerid)).append("&");
        signSrc.append(total_fee+"=").append(params.get(total_fee)).append("&");
        signSrc.append(sdorderno+"=").append(params.get(sdorderno)).append("&");
        signSrc.append(notifyurl+"=").append(params.get(notifyurl)).append("&");
        signSrc.append(returnurl+"=").append(params.get(returnurl)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[龙杏支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
		return signMd5;
	}

    /**
     * 	發送請求
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(sign,pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {
        	result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        } catch (Exception e) {
            log.error("[龙杏支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[龙杏支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 	封裝結果
     */
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[龙杏支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}