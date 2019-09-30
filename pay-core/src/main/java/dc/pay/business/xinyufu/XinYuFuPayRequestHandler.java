package dc.pay.business.xinyufu;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XINYUFU")
public final class XinYuFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinYuFuPayRequestHandler.class);


     private static final String   V = "V";  //   	String	是	5	版本号,默认V4.0	V4.0
     private static final String   UserNo = "UserNo";  //   	String	是	10	商户号	在平台账户管理里查看
     private static final String   ordNo = "ordNo";  //   	String	是	50	请求订单号
     private static final String   ordTime = "ordTime";  //   	datetime	是	14	商户提交订单时间,必须为14位正整数数字,格式为:yyyyMMddHHmmss	如:ordTime=20110808112233（允许误差为1小时）
     private static final String   amount = "amount";  //   	INT	是		支付金额,以分为单位	如1元：amount=100
     private static final String   pid = "pid";  //   	String	否	10	支付编码	wxzf：微信 wxgzhzf: 微信公众号 apzf：支付宝 qqzf：QQ钱包 jdzf： 京东 cxkzf：网银支付 wxh5zf: 微信H5 ylzf: 银联支付
     private static final String   notifyUrl = "notifyUrl";  //   	String	是		异步通知地址
     private static final String   frontUrl = "frontUrl";  //   	String	是		支付成功后跳转到商户指定的地址
     private static final String   ip = "ip";  //   	String	是	20	终端用户的真实IP
     private static final String   sign = "sign";  //   	String	是		签名串
     private static final String   remark = "remark";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(V,"V4.0");
            payParam.put(UserNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(ordNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(ordTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(pid,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(frontUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(ip,channelWrapper.getAPI_Client_IP());
            payParam.put(remark,remark);
        }
        log.debug("[信誉付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // V|UserNo|ordNo|ordTime|amount|pid|notifyUrl|frontUrl|remark|ip
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                params.get(V),
                params.get(UserNo),
                params.get(ordNo),
                params.get(ordTime),
                params.get(amount),
                params.get(pid),
                params.get(notifyUrl),
                params.get(frontUrl),
                params.get(remark),
                params.get(ip),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[信誉付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("resCode") && "10000".equalsIgnoreCase(jsonResultStr.getString("resCode")) && jsonResultStr.containsKey("Payurl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("Payurl"))){
                                result.put(QRCONTEXT,jsonResultStr.getString("Payurl"));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[信誉付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[信誉付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[信誉付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}