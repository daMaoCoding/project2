package dc.pay.business.shunfukejizhifu;

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

import java.util.*;

/**
 * @author Cobby
 * May 9, 2019
 */
@RequestPayHandler("SHUNFUKEJIZHIFU")
public final class ShunFuKeJiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShunFuKeJiZhiFuPayRequestHandler.class);

//    商品名称    goods_name        否    String(100)    您的商品名称，用来显示在后台的订单名称
//    银行卡号    bank_card_no      否    String(30)    银行卡号(注意: 当使用支付方式(pay_way=7，9)时该字段必填，不为空时参与签名)
//    账号名称    bank_card_name    否    String(20)    账号名称(注意: 当使用支付方式(pay_way=9)时该字段必填,不为空时参与签名)
//    身份证号码  id_card           否    String(20)    身份证号码(注意: 当使用支付方式(pay_way=9)时该字段必填,不为空时参与签名)
//    手机号码    mobile            否    String(20)    手机号码(注意: 当使用支付方式(pay_way=9)时该字段必填,不为空则时参与签名)
    private static final String uid               ="uid";        //商户号         是    String(20)    您的商户唯一标识，注册后在“我的资料”里获得
    private static final String price             ="price";      //价格           是    Number(12,2)    单位：元。精确小数点后2位
    private static final String pay_way           ="pay_way";    //支付方式        是    Int    1：微信扫码2：支付宝扫码 3：网银 4: 微信H5 5: 支付宝H5 6：支付宝手机 7：快捷支付 8：银联扫码 9：银联H5  10: 银联快捷
    private static final String notify_url        ="notify_url"; //通知回调网址     是    String(255)    用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www.ccc.com/notify
    private static final String return_url        ="return_url"; //跳转网址        是    String(255)    用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.ccc.com/ return
    private static final String order_id          ="order_id";   //商户自定义订单号  是    String(50)    我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201810162541,不同的订单订单号不能重复
    private static final String order_uid         ="order_uid";  //商户自定义客户号  是    String(100)    我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。强烈建议填写。可以填用户名，也可以填您数据库中的用户uid。例：xxx, xxx@ccc.com

    private static final String token        ="token";
//签名        是    String(32)    通过指定参数，连商户密钥(token)一起，按参数名字母升序排序，将非空参数值以key1=value1&key2=value2方式拼接在一起。做md5-32位加密，取字符串小写。得到签名。网址类型的参数值不要urlencode　

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(price,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(order_uid,channelWrapper.getAPI_ORDER_ID());
                if ("2".equalsIgnoreCase(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG())) {
                    put("clientIp", channelWrapper.getAPI_Client_IP());
                }
            }
        };
        log.debug("[顺福科技]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        api_response_params.put(token,channelWrapper.getAPI_KEY());
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[顺福科技]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[顺福科技]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))
                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    jsonObject = JSONObject.parseObject(jsonObject.getString("data"));
//                    String code_url = jsonObject.getString("pay_url");
//                    result.put( JUMPURL , code_url);
                    if (handlerUtil.isWapOrApp(channelWrapper)) {
                        String code_url = jsonObject.getString("pay_html");
                        result.put(JUMPURL, code_url);
                    } else {
                        String code_url = jsonObject.getString("pay_url");
                        result.put(JUMPURL, code_url);
                    }
                }else {
                    log.error("[顺福科技]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[顺福科技]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[顺福科技]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[顺福科技]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}