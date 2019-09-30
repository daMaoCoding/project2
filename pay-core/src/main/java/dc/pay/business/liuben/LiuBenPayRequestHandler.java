package dc.pay.business.liuben;

import java.util.ArrayList;
import java.util.Base64;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 5, 2017
 */
@RequestPayHandler("LIUBEN")
public final class LiuBenPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LiuBenPayRequestHandler.class);

    //字段名            变量名              必填          类型          示例值        描述
    //版本号            pay_version          是          String          2.0        
    //商户ID            pay_memberid         是          String          1911        
    //商户订单号        pay_orderid          是          String          20160806151343349        只能是数字或字母或者两者组合,最大长度是32字节
    //商品金额          pay_amount           是          float          1.00        
    //银行编号          pay_bankcode         是          String          wxpay        详见：相关编码->银行编码
    //支付场景          pay_scene            是          String          sm        详见：相关编码->支付场景
    //异步通知地址      pay_notifyurl        是          String                  服务器异步通知地址
    //跳转通知地址      pay_callbackurl      是          String                  页面跳转通知地址
    //随机数            pay_rand             是          String          256255        32字节以内
    //返回类型          pay_returntype       是          String          HTML        HTML：表示自动跳转到收银台（建议使用），JSON：表示返回支付url相关数据自己定义收银台
    //用户标识          pay_openid           否          String          oUpF8uMuAJO_M2pxb1Q9zNjWeS6o        公众号支付，小微信程序必传
    //终端ip            pay_create_ip        否          String          192.168.1.1        微信H5或微信wap，微信app,微信刷卡,必传
    //网站名称          pay_mahname          否          String          爱美商城        微信H5或微信wap，微信app必传必传
    //场景信息          pay_scene_info       否          String          256255        微信H5或微信wap，微信app,公众号支付必传必传
    //扩展字段          pay_extend           否          String                  扩展里提交什么在异步通知原样返回，最大长度128字节，允许数字或字母或:/.类型的组合，其他符号禁止传入
    //签名字符串        pay_sign             是          String          202cb962ac59075b964b07152d234b70        请看MD5签名字段格式
    private static final String pay_version                   ="pay_version";
    private static final String pay_memberid                  ="pay_memberid";
    private static final String pay_orderid                   ="pay_orderid";
    private static final String pay_amount                    ="pay_amount";
    private static final String pay_bankcode                  ="pay_bankcode";
    private static final String pay_scene                     ="pay_scene";
    private static final String pay_notifyurl                 ="pay_notifyurl";
    private static final String pay_callbackurl               ="pay_callbackurl";
    private static final String pay_rand                      ="pay_rand";
    private static final String pay_returntype                ="pay_returntype";
    private static final String pay_openid                    ="pay_openid";
    private static final String pay_create_ip                 ="pay_create_ip";
    private static final String pay_mahname                   ="pay_mahname";
    private static final String pay_scene_info                ="pay_scene_info";
    private static final String pay_extend                    ="pay_extend";

    private static final String key        ="pay_key";
    //signature    数据签名    32    是    　
    private static final String signature  ="pay_sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_version,"2.0");
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(pay_scene,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
                put(pay_rand,handlerUtil.getRandomStr(8));
                put(pay_returntype,"JSON");
                //@不再以为                  就先接入扫码就行了
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("LIUBEN_BANK_WAP_WX_SM")) {
                    put(pay_create_ip,channelWrapper.getAPI_Client_IP());
                    put(pay_mahname,"name");
                    put(pay_scene_info,"1");
                }
//                put(pay_extend,channelWrapper.getAPI_MEMBERID());
            }
        };
        log.debug("[六本]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(pay_version+"=").append(api_response_params.get(pay_version)).append("&");
        signSrc.append(pay_amount+"=").append(api_response_params.get(pay_amount)).append("&");
        signSrc.append(pay_bankcode+"=").append(api_response_params.get(pay_bankcode)).append("&");
        signSrc.append(pay_scene+"=").append(api_response_params.get(pay_scene)).append("&");
        signSrc.append(pay_memberid+"=").append(api_response_params.get(pay_memberid)).append("&");
        signSrc.append(pay_orderid+"=").append(api_response_params.get(pay_orderid)).append("&");
        signSrc.append(pay_notifyurl+"=").append(api_response_params.get(pay_notifyurl)).append("&");
        signSrc.append(pay_callbackurl+"=").append(api_response_params.get(pay_callbackurl)).append("&");
        signSrc.append(pay_rand+"=").append(api_response_params.get(pay_rand)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase()).toLowerCase();
        log.debug("[六本]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }


    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String data = Base64.getEncoder().encodeToString(JSONObject.toJSONString(payParam).getBytes());
        Map<String,String> map = new TreeMap<>();
        map.put("body", data);
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), map);
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST,defaultHeaders);
            if (StringUtils.isBlank(resultStr)) {
                log.error("[六本]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
               log.error("[六本]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
               throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[六本]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("return_state") && "ok".equalsIgnoreCase(resJson.getString("return_state"))  && resJson.containsKey("return_payurl") && StringUtils.isNotBlank(resJson.getString("return_payurl"))) {
                String code_url = resJson.getString("return_payurl");
                if (handlerUtil.isWapOrApp(channelWrapper)) {
                    result.put(JUMPURL, code_url);
                }else{
//                    result.put(QRCONTEXT, code_url);
                    //699彩票_六本-3251
                    result.put(handlerUtil.isZfbSM(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                }
            }else {
                log.error("[六本]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[六本]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[六本]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}