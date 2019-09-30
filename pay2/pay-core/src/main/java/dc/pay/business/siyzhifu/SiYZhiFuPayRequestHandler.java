package dc.pay.business.siyzhifu;

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
 * 
 * @author andrew
 * Sep 3, 2019
 */
@RequestPayHandler("SIYZHIFU")
public final class SiYZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SiYZhiFuPayRequestHandler.class);

    //参数名称    参数名 类型  可否为空    说明
    //应用编号    app_id  Number  必填  合作商户的应用编号（支付平台分配）
    private static final String app_id                ="app_id";
    //支付方式    pay_type    Number  必填  支付方式 参考附录1
    private static final String pay_type                ="pay_type";
    //银行卡类型   card_type   Number  选填  银行卡类型。1：借记卡2：贷记卡 默认1
    private static final String card_type                ="card_type";
    //银行缩写    bank_code   String  选填，网银指直连支付必填    银行缩写参考附录4的支持银行的英文缩写    例农业银行缩写为ABC
    private static final String bank_code                ="bank_code";
    //银行卡号    bank_account    String  选填  快捷通道需要上传银行卡号
//    private static final String bank_account                ="bank_account";
    //商户订单号   order_id    String  必填  必须唯一，不超过30字符（商户系统生成）
    private static final String order_id                ="order_id";
    //订单金额    order_amt   Number  必填  订单金额，保留两位小数 单位：元
    private static final String order_amt                ="order_amt";
    //支付结果异步通知URL notify_url  String  必填  用于异步返回支付处理结果的接口
    private static final String notify_url                ="notify_url";
    //支付返回URL return_url  String  必填  支付成功跳转地址
    private static final String return_url                ="return_url";
    //商品名称    goods_name  String  必填  商品名称,长度最长50字符，不能为空（不参加签名）
    private static final String goods_name                ="goods_name";
    //扩展参数    extends String  选填  商户自定义参数，原样返回
//    private static final String my_extends                ="extends";
    //时间戳 time_stamp  String  必填  提交时间戳(格式为yyyyMMddHHmmss 4位年+2位月+2位日+2位时+2位分+2位秒)
    private static final String time_stamp                ="time_stamp";
    //用户ip    user_ip String  必填  客户端真实ip
    private static final String user_ip                ="user_ip";
    //签名  sign    String  必填  参数机制（参见2.4  HTTP参数签名机制）    参数组成（参见下面的签名参数说明）
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
//            log.error("[4y支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[4y支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(app_id, channelWrapper.getAPI_MEMBERID());
                if (SiYZhiFuPayRequestHandler.this.handlerUtil.isWY(channelWrapper)) {
                    put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(pay_type,"12");
                }else {
                    put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(card_type,"1");
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(order_amt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(goods_name,"name");
                put(time_stamp,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(user_ip,channelWrapper.getAPI_Client_IP());
                
            }
        };
        log.debug("[4y支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(app_id+"=").append(api_response_params.get(app_id)).append("&");
        signSrc.append(pay_type+"=").append(api_response_params.get(pay_type)).append("&");
        signSrc.append(order_id+"=").append(api_response_params.get(order_id)).append("&");
        signSrc.append(order_amt+"=").append(api_response_params.get(order_amt)).append("&");
        signSrc.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
        signSrc.append(return_url+"=").append(api_response_params.get(return_url)).append("&");
        signSrc.append(time_stamp+"=").append(api_response_params.get(time_stamp)).append("&");
        signSrc.append(key+"=").append(HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()).toLowerCase());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[4y支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[4y支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[4y支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[4y支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[4y支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            if (null != jsonObject && jsonObject.containsKey("status_code") && "0".equalsIgnoreCase(jsonObject.getString("status_code"))  && jsonObject.containsKey("pay_data") && StringUtils.isNotBlank(jsonObject.getString("pay_data"))) {
                String code_url = jsonObject.getString("pay_data");
                result.put(handlerUtil.isYLSM(channelWrapper) ? QRCONTEXT : JUMPURL, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[4y支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[4y支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[4y支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}