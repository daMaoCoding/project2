package dc.pay.business.xincaifubao;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XINCAIFUBAO")
public final class XinCaiFuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinCaiFuBaoPayRequestHandler.class);


     private static final  String	version = "version";	// 版本号     是	String(32)	4.0	版本号固定 4.0
     private static final  String	app_id = "app_id";	// 商户APP_ID     是	String(36)	wxd678efh567hg6787	分配的商户APP_ID
     private static final  String	pay_type = "pay_type";	// 充值渠道     是	Integer(2)	2	查看充值渠道对应值
     private static final  String	nonce_str = "nonce_str";	// 随机字符串     是	String(32)	C380BEC2BFD727A4B6845133519F3AD6	随机字符串，不长于32位。
     private static final  String	sign_type = "sign_type";	// 签名类型     是	String(64)	HMAC-SHA256	签名类型，目前支持HMAC-SHA256和MD5，默认为MD5
     private static final  String	body = "body";	// 商品描述     是	String(128)	i6pay充值中心-i6pay会员充值  商品简单描述，该字段请按照规范传递
     private static final  String	out_trade_no = "out_trade_no";	// 商户订单号     是	String(32)	20150806125346
     private static final  String	fee_type = "fee_type";	// 标价币种     是	String(16)	CNY
     private static final  String	total_fee = "total_fee";	// 标价金额     是	Int	88	订单总金额，单位为分
     private static final  String	return_url = "return_url";	// 充值后网页跳转地址     是	String(256)	https://pay.i6pay.com/pay/return
     private static final  String	notify_url = "notify_url";	// 通知地址     是	String(256)	https://pay.i6pay.com/pay/notify
     private static final  String	system_time = "system_time";	// 交易结束时间     是	String(14)	20091227091010
     private static final  String	sign = "sign";	// 签名     是	String(64)	5K8264ILTKCH16CQ2502SI8ZNMTM67VS	通过签名算法计算得出的签名值，详见详见签名生成算法
     private static final  String	quick_user_id = "quick_user_id";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version ,  "4.1"  );
            payParam.put(app_id ,   channelWrapper.getAPI_MEMBERID() );
            payParam.put(pay_type ,  channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(nonce_str ,HandlerUtil.getRandomStrStartWithDate(20)    );
            payParam.put(sign_type ,   "MD5" );
            payParam.put(body , channelWrapper.getAPI_ORDER_ID()   );
            payParam.put(out_trade_no , channelWrapper.getAPI_ORDER_ID()    );
            payParam.put(total_fee,  channelWrapper.getAPI_AMOUNT()  );
            payParam.put( fee_type ,  "CNY"  );
            payParam.put(return_url ,  channelWrapper.getAPI_WEB_URL()  );
            payParam.put(notify_url ,  channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()  );
            payParam.put(system_time ,  DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss")  );
            payParam.put(quick_user_id ,  DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss")  );
        }
        log.debug("[新彩富宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //sign=MD5(app_id=1000&nonce_str=f3cfac6450c9424ea8ecd5cca85a5148&out_trade_no=180102195626001&sign_type=MD5&total_fee=1000&version=4.0&key=123456).toUpperCase()
        //sign=MD5(app_id=1000&nonce_str=f3cfac6450c9424ea8ecd5cca85a5148&out_trade_no=180102195626001&sign_type=MD5&total_fee=1000&version=4.0&key=123456).toUpperCase()
        String paramsStr = String.format("app_id=%s&nonce_str=%s&out_trade_no=%s&sign_type=MD5&total_fee=%s&version=%s&key=%s",
                params.get(app_id),
                params.get(nonce_str),
                params.get(out_trade_no),
                params.get(total_fee),
                params.get(version),
                channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr.trim());
        log.debug("[新彩富宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
        	if (1==2 ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString()); //.replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                    resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                    if(StringUtils.isBlank(resultStr)){
                        throw new PayException("[新彩富宝]接口返回空，参数:"+JSON.toJSONString(payParam)+",URL:"+channelWrapper.getAPI_CHANNEL_BANK_URL());
                    }

                    if(resultStr.contains("<form")){
                        result.put(HTMLCONTEXT, resultStr); //.replace("method='post'","method='get'"));
                        payResultList.add(result);
                    }else{
                        JSONObject jsonResultStr = JSON.parseObject(resultStr);
                        if(null!=jsonResultStr && jsonResultStr.containsKey("return_code") && "true".equalsIgnoreCase(jsonResultStr.getString("return_code"))
                                && jsonResultStr.containsKey("result_code") && "true".equalsIgnoreCase(jsonResultStr.getString("result_code")) && jsonResultStr.containsKey("code_url")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("code_url"))){
                                if( HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)){  //HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) ||
                                    result.put(JUMPURL, HandlerUtil.UrlDecode(jsonResultStr.getString("code_url")));
                                    payResultList.add(result);
                                }else{
                                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("code_url")));
                                    payResultList.add(result);
                                }
                            }
                        }else {
                            throw new PayException(resultStr);
                        }
                    }
                }



        } catch (Exception e) {
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //payResultList.add(result);
            log.error("[新彩富宝]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[新彩富宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


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
        log.debug("[新彩富宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}