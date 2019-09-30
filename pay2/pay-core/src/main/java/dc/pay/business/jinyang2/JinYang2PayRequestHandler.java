package dc.pay.business.jinyang2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
@RequestPayHandler("JINYANG2")
public final class JinYang2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinYang2PayRequestHandler.class);

//    字段名			变量名			必填			类型				实例值			说明
//    商户ID			p1_mchtid		是			int				商户ID,由金阳支付分配
//   支付方式			p2_paytype		是			String(20)		WEIXIN	支付网关(参见附录说明4.3)
//   支付金额			p3_paymoney		是			decimal	0.01	订单金额最小0.01(以元为单位）
//   商户平台唯一订单号p4_orderno		是			String(50)		商户系统内部订单号，要求50字符以内，同一商户号下订单号唯一
//   商户异步回调通知地址p5_callbackurl	是			String(200)		商户异步回调通知地址
//   商户同步通知地址	p6_notifyurl	否			String(200)		商户同步通知地址
//   版本号			p7_version		是			String(4)		v2.8	V2.8
//   签名加密方式		p8_signtype		是			int	1			签名加密方式
//   备注信息，上行中	attach			原样返回	
//    				p9_attach		否			String(128)		备注信息，上行中attach原样返回URL Encode （UTF-8）
//   分成标识			p10_appname		否			Strng(25)				分成标识
//   是否显示收银台	p11_isshow		是			int	0			是否显示PC收银台
//   商户的用户下单IP	p12_orderip		否			String(20)		192.168.10.1	商户的用户下单IP
//   商户系统用户唯一标识	p13_memberid否		String(20)		123456	商户用户标识，快捷(FASTPAY)、银联前台快捷(UNIONFASTPAY)，必传，且参与签名，非快捷支付不传、不参与签名
//   签名			sign			是			String(40)		MD5签名

    private static final String p1_mchtid               ="p1_mchtid";
    private static final String p2_paytype           	="p2_paytype";
    private static final String p3_paymoney           	="p3_paymoney";
    private static final String p4_orderno           	="p4_orderno";
    private static final String p5_callbackurl          ="p5_callbackurl";
    private static final String p7_version            	="p7_version";
    private static final String p8_signtype           	="p8_signtype";
    private static final String p11_isshow          	="p11_isshow";
    private static final String p12_orderip          	="p12_orderip";
    
    private static final String sign                ="sign";
    private static final String key                 ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(p1_mchtid, channelWrapper.getAPI_MEMBERID());
                put(p2_paytype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(p3_paymoney, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) );
                put(p4_orderno, channelWrapper.getAPI_ORDER_ID());
                //put(p4_orderno, HandlerUtil.getRandomStr(8));  //// TODO: 2017/11/21 开发
                put(p5_callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(p7_version,"v2.8");
                put(p8_signtype,"1");
                put(p11_isshow,"0");
                put(p12_orderip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[金阳2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String paramsStr = "";
        paramsStr = String.format("p1_mchtid=%s&p2_paytype=%s&p3_paymoney=%s&p4_orderno=%s&p5_callbackurl=%s&p6_notifyurl=&p7_version=%s&p8_signtype=%s&p9_attach=&p10_appname=&p11_isshow=%s&p12_orderip=%s%s",
                api_response_params.get(p1_mchtid),
                api_response_params.get(p2_paytype),
                api_response_params.get(p3_paymoney),
                api_response_params.get(p4_orderno),
                api_response_params.get(p5_callbackurl),
                api_response_params.get(p7_version),
                api_response_params.get(p8_signtype),
                api_response_params.get(p11_isshow),
                api_response_params.get(p12_orderip),
                channelWrapper.getAPI_KEY());
        //最后一个&转换成#
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[金阳2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[金阳2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[金阳2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}