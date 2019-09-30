package dc.pay.business.baolianzhifu;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.ruijietong.RuiJieTongUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * @author andrew
 * Sep 2, 2019
 */
@RequestPayHandler("BAOLIANZHIFU")
public final class BaoLianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaoLianZhiFuPayRequestHandler.class);

    //字段名 字段说明    数据类型    最大长度    备注  必填
    //mchtid  商户ID    int     商户ID（不参与reqdata加密）  是
    private static final String mchtid                ="mchtid";
    //reqdata RSA加密，    UrlEncode编码 String  4000    RSA公钥加密    UrlEncode(RSA("{\"p1_mchtid\":\"22222\",\"p2_paytype\":\"QQPAYWAP\",\"p3_paymoney\":\"1.10\",\"p4_orderno\":\"2018070815s441w\",\"p5_callbackurl\":\"http://www.baidu.com/PayCallback.aspx\",\"p6_notifyurl\":\"http://www.baidu.com/jump.aspx\",\"p7_version\":\"v2.9\",\"p8_signtype\":\"1\",\"p9_attach\":\"attach\",\"p10_appname\":\"appname\",\"p11_isshow\":\"0\",\"p12_orderip\":\"127.0.0.1\",\"sign\":\"4f50326291350abab6f243d281dcdec9\"}"))    是
    private static final String reqdata                ="reqdata";

    //reqdata数据RSA加密前的数据参数列表:
    //字段名 变量名 必填  类型  签名  实例值 说明
    // 商户ID   p1_mchtid   是   int 是       商户ID,由宝联支付分配
    private static final String p1_mchtid                ="p1_mchtid";
    //支付方式    p2_paytype  是   String(20)  是   WEIXIN  支付网关(参见附录说明4.3)
    private static final String p2_paytype                ="p2_paytype";
    //支付金额    p3_paymoney 是   decimal 是   0.01    订单金额最小0.01(以元为单位）
    private static final String p3_paymoney                ="p3_paymoney";
    //商户平台唯一订单号   p4_orderno  是   String(50)  是       商户系统内部订单号，要求50字符以内，同一商户号下订单号唯一
    private static final String p4_orderno                ="p4_orderno";
    //商户异步回调通知地址  p5_callbackurl  是   String(200) 是       商户异步回调通知地址
    private static final String p5_callbackurl                ="p5_callbackurl";
    //商户同步通知地址    p6_notifyurl    否   String(200) 是       商户同步通知地址
    private static final String p6_notifyurl                ="p6_notifyurl";
    //版本号 p7_version  是   String(4)   是   v2.9    v2.9
    private static final String p7_version                ="p7_version";
    //签名加密方式  p8_signtype 是   int 是   2   签名加密方式(2.RSA)
    private static final String p8_signtype                ="p8_signtype";
    //备注信息，上行中attach原样返回  p9_attach   否   String(128) 是       备注信息，上行中attach原样返回
    private static final String p9_attach                ="p9_attach";
    //分成标识    p10_appname 否   Strng(25)   是       分成标识
    private static final String p10_appname                ="p10_appname";
    //是否显示收银台 p11_isshow  是   int 是   0   是否显示PC收银台
    private static final String p11_isshow                ="p11_isshow";
    //商户的用户下单IP   p12_orderip 否   String(20)  是   192.168.10.1    商户的用户下单IP
    private static final String p12_orderip                ="p12_orderip";
    //商户系统用户唯一标识  p13_memberid    否   String(20)  p2_paytype=“FASTPAY”时参与签名，其它类型不参与签名 123456  商户用户标识，暂时只有快捷支付使用。p2_paytype=“FASTPAY”的时候必传
    private static final String p13_memberid                ="p13_memberid";
    //签名  sign    是   String(40)  否       MD5签名
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
//            log.error("[宝联支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[宝联支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[宝联支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：Md5密钥-Rsa密钥" );
            throw new PayException("[宝联支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：Md5密钥-Rsa密钥" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(p1_mchtid, channelWrapper.getAPI_MEMBERID());
                put(p2_paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(p3_paymoney,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(p4_orderno,channelWrapper.getAPI_ORDER_ID());
                put(p5_callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(p6_notifyurl,channelWrapper.getAPI_WEB_URL());
                put(p7_version,"v2.9");
                put(p8_signtype,"2");
                put(p9_attach,channelWrapper.getAPI_MEMBERID());
                put(p10_appname,"name");
                put(p11_isshow,"0");
                if (handlerUtil.isWebYlKjzf(channelWrapper)) {
                    put(p13_memberid,handlerUtil.getRandomStr(8));
                }
                put(p12_orderip,channelWrapper.getAPI_Client_IP());
                
            }
        };
        log.debug("[宝联支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(p1_mchtid+"=").append(api_response_params.get(p1_mchtid)).append("&");
        signSrc.append(p2_paytype+"=").append(api_response_params.get(p2_paytype)).append("&");
        signSrc.append(p3_paymoney+"=").append(api_response_params.get(p3_paymoney)).append("&");
        signSrc.append(p4_orderno+"=").append(api_response_params.get(p4_orderno)).append("&");
        signSrc.append(p5_callbackurl+"=").append(api_response_params.get(p5_callbackurl)).append("&");
        signSrc.append(p6_notifyurl+"=").append(api_response_params.get(p6_notifyurl)).append("&");
        signSrc.append(p7_version+"=").append(api_response_params.get(p7_version)).append("&");
        signSrc.append(p8_signtype+"=").append(api_response_params.get(p8_signtype)).append("&");
        signSrc.append(p9_attach+"=").append(api_response_params.get(p9_attach)).append("&");
        signSrc.append(p10_appname+"=").append(api_response_params.get(p10_appname)).append("&");
        signSrc.append(p11_isshow+"=").append(api_response_params.get(p11_isshow)).append("&");
        signSrc.append(p12_orderip+"=").append(api_response_params.get(p12_orderip));
        if (StringUtils.isNotBlank(api_response_params.get(p13_memberid))) {
            signSrc.append("&").append(p13_memberid+"=").append(api_response_params.get(p13_memberid));
        }
        signSrc.append(channelWrapper.getAPI_KEY().split("-")[0]);
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        System.out.println("通道："+channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()+"     |   "+"md5源串："+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[宝联支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        String step2 = null;
        try {
            byte[] contentAndPubkeyBytes = RuiJieTongUtil.encryptByPublicKey(JSON.toJSONString(new TreeMap<>(payParam)).getBytes(RuiJieTongUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
            step2 = Base64.getEncoder().encodeToString(contentAndPubkeyBytes);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[好多钱]-[请求支付]-3.1.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        Map<String,String> result = Maps.newHashMap();
//        if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)){
        if(HandlerUtil.isWapOrApp(channelWrapper)){
            Map<String,String> map_tmp = Maps.newHashMap();
            map_tmp.put(mchtid, channelWrapper.getAPI_MEMBERID());
            map_tmp.put(reqdata, step2);

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map_tmp).toString());
            //保存第三方返回值
//            result.put(HTMLCONTEXT,htmlContent.toString());
        }else{
            StringBuilder sb = new StringBuilder();
            sb.append(mchtid+"=").append(channelWrapper.getAPI_MEMBERID()).append("&");
            sb.append(reqdata+"=").append(step2);

            System.out.println("sb.toString()=========>"+sb.toString());
            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), sb.toString(),MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            System.out.println("resultStr=========>"+resultStr);
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[宝联支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[宝联支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //    log.error("[宝联支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //}
            JSONObject jsonObject = null;
            try {
//                jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                log.error("[宝联支付]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(e.getMessage(),e);
            }          
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("rspCode") && "1".equalsIgnoreCase(jsonObject.getString("rspCode"))  && 
            (jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data")) || 
             jsonObject.getJSONObject("data").containsKey("r6_qrcode") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("r6_qrcode")))
            ){
//            if (null != jsonObject && jsonObject.containsKey("rspCode") && "1".equalsIgnoreCase(jsonObject.getString("rspCode"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject.getJSONObject("data").getString("r6_qrcode"));
                    //if (handlerUtil.isWapOrApp(channelWrapper)) {
                    //    result.put(JUMPURL, code_url);
                    //}else{
                    //    result.put(QRCONTEXT, code_url);
                    //}
                //按不同的请求接口，向不同的属性设置值
                //if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name())||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAPAPP_.name())) {
                //    result.put(JUMPURL, jsonObject.getString("barCode"));
                //}else{
                //    result.put(QRCONTEXT, jsonObject.getString("barCode"));
                //}
//                result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
            }else {
                log.error("[宝联支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[宝联支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[宝联支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}