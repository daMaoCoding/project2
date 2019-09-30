package dc.pay.business.laifubei;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 12, 2018
 */
@RequestPayHandler("LAIFUBEI")
public final class LaiFuBeiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LaiFuBeiPayRequestHandler.class);

    //请求参数：
    //#   参数名 含义  类型  说明
    //1   uid 商户uid   int(10) 必填。您的商户唯一标识，注册后在设置里获得。
    private static final String uid                ="uid";
    //2   istype  支付渠道    int 必填。1：微信；2支付宝
    private static final String istype                ="istype";
    //3   notify_url  通知回调网址  string(255) 必填。用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www .aaa.com/pay_notify
    private static final String notify_url                ="notify_url";
    //4   orderid 商户自定义订单号    string(50)  必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201710192541
    private static final String orderid                ="orderid";
    //5   orderuid    商户自定义客户号    string(100) 选填。我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。强烈建议填写。可以填用户名，也可以填您数据库中的用户uid。例：xxx, xxx@aaa.com
    private static final String orderuid                ="orderuid";
    //6   goodsname   金额  string(100) 必填。传入用户在收银台输入的金额（任意金额，但上限3000）
    private static final String goodsname                ="goodsname";
    //7   key 秘钥  string(32)  必填。把使用到的所有参数，连Token一起。把参数值拼接在一起。做md5-32位加密，取字符串小写。得到key。网址类型的参数值不要urlencode。
//    private static final String key                ="key";
    //key的拼接顺序：如用到了所有参数，就按这个顺序拼接：goodsname + istype + notify_url + orderid + orderuid + token + uid

//    private static final String Token        ="Token";
    //signature    数据签名    32    是    　
//    private static final String signature  ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(istype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(orderuid,handlerUtil.getRandomStr(6));
                put(goodsname,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            }
        };
        log.debug("[来付呗]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(goodsname));
        signSrc.append(api_response_params.get(istype));
        signSrc.append(api_response_params.get(notify_url));
        signSrc.append(api_response_params.get(orderid));
        signSrc.append(api_response_params.get(orderuid));
        signSrc.append(channelWrapper.getAPI_KEY());
        signSrc.append(api_response_params.get(uid));
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[来付呗]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[来付呗]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[来付呗]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[来付呗]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[来付呗]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("code") && "1".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
            JSONObject jsonObject = resJson.getJSONObject("data");
            result.put( JUMPURL, jsonObject.getString("pay_url"));
        }else {
            log.error("[来付呗]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[来付呗]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[来付呗]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}