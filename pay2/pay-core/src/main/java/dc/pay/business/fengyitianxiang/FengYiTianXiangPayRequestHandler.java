package dc.pay.business.fengyitianxiang;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 23, 2018
 */
@RequestPayHandler("FENGYITIANXIANG")
public final class FengYiTianXiangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FengYiTianXiangPayRequestHandler.class);

    //参数               参数说明                         参与签名          类型                          是否必须提交，及备注说明
    //return_type        返回数据类型                       是              字符串                        必填参数json， html（详情请看，返回说明）
    //api_code           商户号                             是              字符串                        必须
    //is_type            支付类型                           是              字符串                        必须，支付渠道：alipay支付宝，wechat微信，alipay_wap支付宝h5..........等等可通过http://ip地址/channel/common/api_query查询，有显示的英文都是可用接口
    //price              订单定价                           是              float，保留2位小数            必须，保留2位小数，不能传0
    //order_id           您的自定义单号                     是              字符串，最长50位              必须，在商户系统中保持唯一
    //time               发起时间                           是              时间戳，最长10位              必须 时间戳
    //mark               描述                               是              字符串，最长100位             必须 粗略说明支付目的（例如 购买食杂）
    //return_url         成功后网页跳转地址                 是              字符串，最长255位             必须，成功后网页跳转地址（例如 http://www.qq.com）
    //notify_url         通知状态异步回调接收地址           是              字符串，最长255位             必须
    //api_key            我们签名秘钥                       是              字符串，最长32位              这个不需要提交，但是这个参数参与签名
    //sign               签名认证串                         －              字符串，最长32位              必须，一定存在。我们把使用到的所有参数，按照参数值首字母ASCII升序排序，并以url传参格式拼接在一起，最后加上您的商户秘钥。一起做md5-32位加密，取字符串大写。得到sign。您需要在您的服务端按照同样的算法，自己验证此sign是否正确。只在正确时，执行您自己逻辑中代码。
    private static final String return_type             ="return_type";
    private static final String api_code                ="api_code";
    private static final String is_type                 ="is_type";
    private static final String price                   ="price";
    private static final String order_id                ="order_id";
    private static final String time                    ="time";
    private static final String mark                    ="mark";
    private static final String return_url              ="return_url";
    private static final String notify_url              ="notify_url";
//    private static final String api_key                 ="api_key";
//    private static final String sign                    ="sign";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(return_type,"json");
                put(api_code, channelWrapper.getAPI_MEMBERID());
                put(is_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(price,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(mark,"name");
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[凤翼天翔]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
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
        log.debug("[凤翼天翔]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[凤翼天翔]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[凤翼天翔]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[凤翼天翔]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[凤翼天翔]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("messages")) {
            JSONObject resJson2 = resJson.getJSONObject("messages");
            if (null != resJson2 && resJson2.containsKey("returncode") && "SUCCESS".equalsIgnoreCase(resJson2.getString("returncode"))  && resJson.containsKey("payurl") && StringUtils.isNotBlank(resJson.getString("payurl"))) {
                String code_url = resJson.getString("payurl");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[凤翼天翔]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }else {
            log.error("[凤翼天翔]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[凤翼天翔]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[凤翼天翔]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}