package dc.pay.business.daddyzhifu;

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

@RequestPayHandler("DADDYZHIFU")
public final class DaddyPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DaddyPayRequestHandler.class);


     private static final String      parter	   = "parter" ;          //是	string	接口调用ID
     private static final String      value	   = "value" ;               //是	int	金额，元为单位
     private static final String      type	   = "type" ;                //是	string	支付类型：wx=微信,wxwap=微信WAP,ali=支付宝,aliwap=支付宝WAP,qq=QQ,qqwap=QQWAP
     private static final String      orderid	   = "orderid" ;         //是	string	商家订单号
     private static final String      notifyurl	   = "notifyurl" ;       //是	string	异步通知地址
     private static final String      callbackurl	   = "callbackurl" ; //是	string	支付成功后跳转到该页面
     private static final String      getcode	   = "getcode" ;         //否	int	默认0，为1的时候返回二维码内容，自行使用Curl方式获取，获取例子：{“code”:”200”,”codeimg”:”二维码内容”}。非常建议跳转到我方页面，不建议获取内容
     private static final String      sign	   = "sign" ;                //是	string	签名


    /**
     *
     *  文档写的太啰嗦，没接。。。。
     *  文档写的太啰嗦，没接。。。。
     *  文档写的太啰嗦，没接。。。。
     *  文档写的太啰嗦，没接。。。。
     *
     */

//
//
//    company_id	平台id	2	数字编码，DP系统提供给平台的company_id	否	20
//    bank_id	银行id	2	客户发起充值申请的银行编码，（银行编码详见8.4附件四）	否	例如ICBC:1
//    amount	用户申请的充值金额	10	数字，需保留两位小数点，无千位符，单位为CNY	否	1000.00
//    company_order_num	平台订单号	64	字符串，订单号是唯一的	否	DDD123456789
//    company_user	平台用户	32	字符串，玩家在平台的昵称，不能重复	否	ABC123
//    key	动态密钥
//    estimated_payment_bank	预计付款银行	2	客户预计使用的银行编码，实际上与bank_id一致
//    deposit_mode	充值渠道  1银行卡  2第三方、QQ钱包、企业版微信与支付宝   3移动电子钱包（个人版支付宝二维码）   4 HTML5（WAP）模式  6收银台模式（目前只开放支付宝）
//    group_id	群组id  例如默认：0
//    web_url	平台访问地址
//    note_model	匹配模式   1平台附言2 DP系统附言3 DP系统金额4平台金额   金额模式，如果deposit_mode为3时，则此字段为3或者4，（详见2.4附言格式规范） 如果deposit_mode为2、4、5、6时，该参数可默认为2
//    terminal	使用终端  1电脑端  2手机端  3平板  9其他 平台需要传输客户的真实使用终端，以防止客户会无法成功发起充值申请
//








    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(parter,channelWrapper.getAPI_MEMBERID());
            payParam.put(value,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(callbackurl,channelWrapper.getAPI_WEB_URL());
            payParam.put(getcode,"1");
        }

        log.debug("[DADDY支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[DADDY支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||( HandlerUtil.isWapOrApp(channelWrapper)  && !channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("GEFU_BANK_WAP_QQ_SM")  )  ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(1==1 ) {  //扫码（银联扫码&微信&支付宝&QQ）
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[DADDY支付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            payResultList.add(result);
            //log.error("[DADDY支付]3.发送支付请求，及获取支付请求结果出错：", e);
            //throw new PayException(e.getMessage(), e);
        }
        log.debug("[DADDY支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[DADDY支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}