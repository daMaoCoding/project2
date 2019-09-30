package dc.pay.business.aobang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 24, 2018
 */
@RequestPayHandler("AOBANG")
public final class AoBangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AoBangPayRequestHandler.class);

    //序号 参数 名称 数据类型 必填 参数说明及样例
    //1 trx_key 商户支付KEY String(32) M 商户支付KEY，在商户后台(信息接入)获得
    private static final String trx_key                ="trx_key";
    //2 ord_amount 订单金额 String(12) M 订单金额(元)，保留小数点后两位，如：99.99
    private static final String ord_amount                ="ord_amount";
    //3 request_id 订单 ID String(30) M 商户支付请求的订单号
    private static final String request_id                ="request_id";
    //4 request_ip 请求 IP String(15) M 下单时客户端请求的 IP
    private static final String request_ip                ="request_ip";
    //5 product_type 产品类型 String(8) M 支付产品类型，详见附录中“产品类型”项
    private static final String product_type                ="product_type";
    //6 request_time 请求时间 String(14) M 格式yyyyMMddHHmmss 如 20180111210635
    private static final String request_time                ="request_time";
    //7 goods_name 商品名称 String(200) M 商品名称
    private static final String goods_name                ="goods_name";
    //8 return_url 页面通知地址 String(300) M 页面通知地址
    private static final String return_url                ="return_url";
    //9 callback_url 后台异步通知地址 String(300) M 后台异步通知地址
    private static final String callback_url                ="callback_url";
    //10 remark 备注信息 String(200) C 备注信息
    private static final String remark                ="remark";
    
    //8 bank_code 银行编码 String(10) M银行编码，请查看附录“银行编码列表”（当为网关时，此参数必传。其他支付方式，此参数不    用
    private static final String bank_code                ="bank_code";
    
    //11 sign 签名 String(50) M 签名，此字段不参与签名
//    private static final String sign                ="sign";

    private static final String key        ="secret_key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(trx_key, channelWrapper.getAPI_MEMBERID());
                put(ord_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(request_id,channelWrapper.getAPI_ORDER_ID());
                put(request_ip,channelWrapper.getAPI_Client_IP());
                put(request_time, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(goods_name,"name");
                if (handlerUtil.isWY(channelWrapper)) {
                    put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(product_type,"50103");
                }else{
                    put(product_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(remark, channelWrapper.getAPI_MEMBERID());
//                put(merchno, channelWrapper.getAPI_MEMBERID());
            }
        };
        log.debug("[奥邦]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[奥邦]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if ((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper) || handlerUtil.isWEBWAPAPP_SM(channelWrapper)) && !handlerUtil.isZFB(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[奥邦]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[奥邦]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[奥邦]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[奥邦]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("rsp_code") && "0000".equalsIgnoreCase(jsonObject.getString("rsp_code"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                String code_url = jsonObject.getString("data");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[奥邦]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[奥邦]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[奥邦]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}