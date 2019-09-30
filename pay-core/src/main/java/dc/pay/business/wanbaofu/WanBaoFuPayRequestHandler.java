package dc.pay.business.wanbaofu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("WANBAOFU")
public final class WanBaoFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WanBaoFuPayRequestHandler.class);

     private static final String  	pid = "pid";    //	  商户ID   是	int	10003
     private static final String  	type = "type";    //	  支付方式   是	string	alipay2	可选的参数是：alipay2（支付宝）、wechat2（微信）、alipay2qr（支付宝，只提供json数据）、wechat2qr（微信，只提供json数据）。
     private static final String  	out_trade_no = "out_trade_no";    //	  商户订单号   是	string	1530844815	该订单号在同步或异步地址中原样返回
     private static final String  	notify_url = "notify_url";    //	  异步通知地址   是	string	http://www.example.com/notify_url.php	服务器异步通知地址
     private static final String  	return_url = "return_url";    //	  跳转通知地址   是	string	http://www.example.com/notify_url.php	页面跳转通知地址
     private static final String  	name = "name";    //	  商品名称   string	string	VIP会员
     private static final String  	money = "money";    //	  商品金额   是	string	0.01
     private static final String  	sign = "sign";    //	  签名字符串   是	string	202cb962ac59075b964b07152d234b70	签名算法与 支付宝签名算法 相同
     private static final String  	sign_type = "sign_type";    //	  签名类型   是	string	MD5	默认为MD5，不参与签名
     private static final String  	MD5 = "MD5";

    private static final String  code = "code";  //  "": 1,
    private static final String  msg = "msg";  //  "": "获取成功",
    private static final String  payurl = "payurl" ;  //   https://qr.alipay.com/fkx093613t2l3pob1l2zsf9?t=1538458998548"
    private static final String  mark = "mark";  //  "": "2018100213431555945",
    //private static final String  money = "money";  //  "": "1.00",
    //private static final String  type = "type";  //  "": "alipay",
    private static final String  account = "account";  //  "":

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(pid,channelWrapper.getAPI_MEMBERID());
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(name,channelWrapper.getAPI_ORDER_ID());
            payParam.put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(sign_type, MD5 );
        }
        log.debug("[万宝付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())  ||sign_type.equalsIgnoreCase(paramKeys.get(i).toString())   )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString().replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[万宝付]-[请求支付]-2.生成加密URL签名完成：" + pay_md5sign);
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        if (HandlerUtil.isWapOrApp(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
            if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                result.put(HTMLCONTEXT,resultStr);
            }else if(StringUtils.isNotBlank(resultStr) ){
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                if(null!=jsonResultStr && jsonResultStr.containsKey(code) && "1".equalsIgnoreCase(jsonResultStr.getString(code)) && jsonResultStr.containsKey(payurl) && StringUtils.isNotBlank(jsonResultStr.getString(payurl))){
                        String qrContextStr = jsonResultStr.getString(payurl);
                        if(HandlerUtil.isWapOrApp(channelWrapper)){  result.put(JUMPURL, qrContextStr);  }else{  result.put(QRCONTEXT, qrContextStr); }
                }else {throw new PayException(resultStr); }
            }else{ throw new PayException(EMPTYRESPONSE);}
             
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
    
        log.debug("[万宝付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[万宝付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}