package dc.pay.business.xinrongbao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
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
 * Dec 17, 2018
 */
@RequestPayHandler("XINRONGBAO")
public final class XinRongBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinRongBaoPayRequestHandler.class);

    //参数  参数名称    类型（长度）  参数说明    是否必填    样例
    //ApiMethod   业务标识    String(20)  固定值:OnLinePay   是   OnLinePay
    private static final String ApiMethod                     ="ApiMethod";
    //Version 版本号 String(5)   固定值:V2.0    是   V2.0
    private static final String Version                     ="Version";
    //MerID   商户号 String(4)   商户号，四位数字，商户后台可查看    是   1111
    private static final String MerID                     ="MerID";
    //TradeNum    商户订单号   String(10,50)   商户订单号   是   201812121212121234
    private static final String TradeNum                     ="TradeNum";
    //Amount  交易金额    String(20)  该笔订单的资金总额，单位为：RMB。取值范围为[1，1000000.00]，精确到小数点后两位。    是   12.21
    private static final String Amount                     ="Amount";
    //GoodsName   商品名称    String（50）  用户购买的商品名称，如参数值有中文需要进行url编码后发送到支付平台  否   手机
//    private static final String GoodsName                     ="GoodsName";
    //GoodsDesc   商品描述    String(50)  用户购买商品的描述信息，如参数值有中文需要进行url编码后发送到支付平台    否   华为
//    private static final String GoodsDesc                     ="GoodsDesc";
    //NotifyUrl   服务器异步通知路径   String(200) 服务器主动异步通知商户网站指定的路径  是   https://1.com/NotifyUrl.html
    private static final String NotifyUrl                     ="NotifyUrl";
    //ReturnUrl   页面跳转同步通知页面  String（200） 支付完成后web页面跳转显示支付结果  否   https://1.com/ReturnURL.htm
//    private static final String ReturnUrl                     ="ReturnUrl";
    //TransTime   请求时间    String(14)  请求时间，格式：yyyyMMddHHmmss  是   20181212121212
    private static final String TransTime                     ="TransTime";
    //Ext1    商户扩展信息  String(20)  通知时原样返回，由字母和数字组成。   否   Ext1
//    private static final String Ext1                     ="Ext1";
    //PayType 支付方式    String(15)  参考8.2支付方式设置 是   请参考7.2支付方式设置
    private static final String PayType                     ="PayType";
    //BankCode    银行编码    String(10)  支付方式为【onlinebank】时必填，其他支付方式可空   可选  ICBC    请参考7.1银行编码
//    private static final String BankCode                     ="BankCode";
    //SignType    签名方式    String(7)   参数的签名方式，固定值：MD5 是   MD5
    private static final String SignType                     ="SignType";
    //IsImgCode   是否返回支付链接    String(1)   值只能为“1”或不传值    值为“1”：返回支付链接，商户根据支付链接自行生成二维码；    不传值：跳转到【支付平台】进行支付；  可选  1
    private static final String IsImgCode                     ="IsImgCode";
    //UserIP  用户IP    String(20)  用户支付时IP地址，【IsImgCode】值为“1”时必填   可选  127.0.0.1
    private static final String UserIP                     ="UserIP";
    //Openid  公众号openid   String(28)  公众号openid，公众号支付时必填  可选  
//    private static final String Openid                     ="Openid";
    //Sign    数字签名    String(32)  签名规则请参考5.2签名算法  是   请参考5.2签名算法private static final String p0_Cmd                       ="p0_Cmd";
//    private static final String Sign                     ="Sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="hmac";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(ApiMethod,"OnLinePay");
                put(Version,"V2.0");
                put(MerID, channelWrapper.getAPI_MEMBERID());
                put(TradeNum,channelWrapper.getAPI_ORDER_ID());
                put(Amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(TransTime, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(PayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(SignType,"MD5");
                put(IsImgCode,"1");
                put(UserIP,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[新融宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
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
        signSrc.append(key +"=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新融宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWapOrApp(channelWrapper)) {
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[新融宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
             }
             //JSONObject jsonObject = JSONObject.parseObject(resultStr);
             JSONObject jsonObject;
             try {
                 jsonObject = JSONObject.parseObject(resultStr);
             } catch (Exception e) {
                 e.printStackTrace();
                 log.error("[新融宝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 throw new PayException(resultStr);
             }
             //只取正确的值，其他情况抛出异常
             if (null != jsonObject && jsonObject.containsKey("RespCode") && "1111".equalsIgnoreCase(jsonObject.getString("RespCode"))  && jsonObject.containsKey("PayUrl") && StringUtils.isNotBlank(jsonObject.getString("PayUrl"))) {
                 String code_url = jsonObject.getString("PayUrl");
                 result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                 //if (handlerUtil.isWapOrApp(channelWrapper)) {
                 //    result.put(JUMPURL, code_url);
                 //}else{
                 //    result.put(QRCONTEXT, code_url);
                 //}
             }else {
                 log.error("[新融宝]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 throw new PayException(resultStr);
             }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新融宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新融宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}