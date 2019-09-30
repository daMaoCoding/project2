package dc.pay.business.mengma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 11, 2018
 */
@RequestPayHandler("MENGMA")
public final class MengMaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MengMaPayRequestHandler.class);

    //字段 ⻓度 可否为空 注释
    //service 32 否 S1007
    private static final String service                                 ="service";
    //partner 32 否 商户合作号，由平台注册提供
    private static final String partner                                 ="partner";
    //input_charset 10 否 编码格式:UTF-8
    private static final String input_charset                                 ="input_charset";
    //sign_type 3 否 签名⽅式:MD5
    private static final String sign_type                                 ="sign_type";
    //sign 256 否 签名字符串
//    private static final String sign                                 ="sign";
    //request_time 20 否 YYMMDDHHmmss
    private static final String request_time                                 ="request_time";
    //notify_url 1024 否 后台通知地址
    private static final String notify_url                                 ="notify_url";
    //out_trade_no 40 否 原始商户订单
    private static final String out_trade_no                                 ="out_trade_no";
    //amount 40 否 ⾦额(0.01~9999999999.99)
    private static final String amount                                 ="amount";
    //tran_ip 20 否 ***.***.***.***
    private static final String tran_ip                                 ="tran_ip";
    //subject 40 否 商品名称
    private static final String subject                                 ="subject";
    //sub_body 40 否 商品详情 
    private static final String sub_body                                 ="sub_body";
    //buyer_name  20  可   买家姓名
//    private static final String buyer_name                                 ="buyer_name";
//    //buyer_contact   20  可   买家联系⽅式
//    private static final String buyer_contact                                 ="buyer_contact";
    //bank_sn   32  否   银⾏编码
    private static final String bank_sn                                 ="bank_sn";

    private static final String key        ="verfication_code";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[猛犸]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：md5Key-Rsa私钥" );
            throw new PayException("[猛犸]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：md5Key-Rsa私钥" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(service ,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(partner,channelWrapper.getAPI_MEMBERID());
                put(input_charset,"UTF-8");
                put(sign_type,"MD5");
                put(bank_sn,"ABC");
                put(request_time, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(notify_url ,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(tran_ip,channelWrapper.getAPI_Client_IP());
                put(subject,"name");
                put(sub_body,"name");
            }
        };
        log.debug("[猛犸]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
        signSrc.append(out_trade_no+"=").append(api_response_params.get(out_trade_no)).append("&");
        signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
        signSrc.append(request_time+"=").append(api_response_params.get(request_time)).append("&");
        signSrc.append(service+"=").append(api_response_params.get(service)).append("&");
        signSrc.append(subject+"=").append(api_response_params.get(subject)).append("&");
        signSrc.append(tran_ip+"=").append(api_response_params.get(tran_ip)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY().split("-")[0]);
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[猛犸]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[猛犸]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[猛犸]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[猛犸]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
             }
             //JSONObject jsonObject = JSONObject.parseObject(resultStr);
             JSONObject jsonObject;
             try {
                 jsonObject = JSONObject.parseObject(resultStr);
             } catch (Exception e) {
                 e.printStackTrace();
                 log.error("[猛犸]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 throw new PayException(resultStr);
             }
             //只取正确的值，其他情况抛出异常
             if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                 String code_url = jsonObject.getString("codeimg");
                 result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                 //if (handlerUtil.isWapOrApp(channelWrapper)) {
                 //    result.put(JUMPURL, code_url);
                 //}else{
                 //    result.put(QRCONTEXT, code_url);
                 //}
             }else {
                 log.error("[猛犸]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 throw new PayException(resultStr);
             }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[猛犸]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[猛犸]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}
