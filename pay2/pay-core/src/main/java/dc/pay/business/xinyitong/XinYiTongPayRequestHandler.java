package dc.pay.business.xinyitong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DigestUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Oct 27, 2018
 */
@RequestPayHandler("XINYITONG")
public final class XinYiTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinYiTongPayRequestHandler.class);

    //参数名称                  参数含义                            是否必填          参数长度             参数说明          顺序
    //p0_Cmd                    业务类型                            是                 Max(20)             固定值 “Buy”           1
    //p1_MerId                  商户编号                            是                 Max(11)             4位数字，是商户在[API支付平台]系统的唯一身份标识请登录商户后台查看          2
    //p2_Order                  商户订单号                          是                 Max(50)             若不为“”，提交的订单号必须在自身账户交易中唯一；为“”时，[API支付平台]会自动生成随机的商户订单号.[API支付平台]系统中对于已付或者撤销的订单，商户端不能重复提交          3
    //p3_Amt                    支付金额                            是                 Max(20)             单位：元，精确到分，保留小数点后两位          4
    //p4_Cur                    交易币种                            是                 Max(10)             固定值 “CNY”          5
    //p5_Pid                    商品名称                            否                 Max(20)             用于支付时显示在[API支付平台]网关左侧的订单产品信息；此参数如用到中文，请注意转码.          6
    //p6_Pcat                   商品种类                            否                 Max(20)             商品种类； 此参数如用到中文，请注意转码          7
    //p7_Pdesc                  商品描述                            否                 Max(20)             商品描述； 此参数如用到中文，请注意转码          8
    //p8_Url                    商户接收支付成功数据的地址          是                 Max(200)            支付成功后[API支付平台]会向该地址发送两次成功通知，该地址不可以带参数，如:“ www.yeepay.com/callback.action”.注意不建议使用过长的URL地址          9
    //p9_SAF                    送货地址                            是                 Max(1)              为“1”：需要用户将送货地址留在[API支付平台]系统； 为“0”：不需要，默认为“0”          10
    //pa_MP                     商户扩展信息                        否                 Max(200)            返回时原样返回；此参数如用到中文，请注意转码          11
    //pd_FrpId                  支付通道编码                        是                 Max(50)             该字段不能为空值；  请按照文档下方“支付通道编码列表”所示参数进行提交          12
    //pr_NeedResponse           应答机制                            是                 Max(1)              固定值为“1”：需要应答机制；收到[API支付平台]服务器点对点支付成功通知，必须回写“success”（无关大小写），即使您收到成功通知时发现该订单已经处理过，也要正确回写“success”，否则[API支付平台]将认为您的系统没有收到通知，启动重发机制，直到收到“success”为止          13
    //hmac                      签名数据                            是                 Max(32)             产生hmac需要两个参数，并调用相关API；  参数1： STR，列表中的参数值按照签名顺序拼接所产生的字符串，注意null要转换为 “”，并确保无乱码.  参数2：商户密钥；请登录商户后台查看  各语言范例已经提供封装好了的方法用于生成此参数  如果以上两个参数有错误，则该参数必然错误，提示“签名失败”
    private static final String p0_Cmd                       ="p0_Cmd";
    private static final String p1_MerId                     ="p1_MerId";
    private static final String p2_Order                     ="p2_Order";
    private static final String p3_Amt                       ="p3_Amt";
    private static final String p4_Cur                       ="p4_Cur";
    private static final String p5_Pid                       ="p5_Pid";
    private static final String p6_Pcat                      ="p6_Pcat";
    private static final String p7_Pdesc                     ="p7_Pdesc";
    private static final String p8_Url                       ="p8_Url";
    private static final String p9_SAF                       ="p9_SAF";
    private static final String pa_MP                        ="pa_MP";
    private static final String pd_FrpId                     ="pd_FrpId";
    private static final String pr_NeedResponse              ="pr_NeedResponse";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="hmac";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(p0_Cmd,"Buy");
                put(p1_MerId, channelWrapper.getAPI_MEMBERID());
                put(p2_Order,channelWrapper.getAPI_ORDER_ID());
                put(p3_Amt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(p4_Cur,"CNY");
                put(p5_Pid,"PID");
                put(p6_Pcat,"PCAT");
                put(p7_Pdesc,"PDESC");
                put(p8_Url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(p9_SAF,"0");
                put(pa_MP,channelWrapper.getAPI_MEMBERID());
                put(pd_FrpId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pr_NeedResponse,"1");
            }
        };
        log.debug("[新亿通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(p0_Cmd));
        signSrc.append(api_response_params.get(p1_MerId));
        signSrc.append(api_response_params.get(p2_Order));
        signSrc.append(api_response_params.get(p3_Amt));
        signSrc.append(api_response_params.get(p4_Cur));
        signSrc.append(api_response_params.get(p5_Pid));
        signSrc.append(api_response_params.get(p6_Pcat));
        signSrc.append(api_response_params.get(p7_Pdesc));
        signSrc.append(api_response_params.get(p8_Url));
        signSrc.append(api_response_params.get(p9_SAF));
        signSrc.append(api_response_params.get(pa_MP));
        signSrc.append(api_response_params.get(pd_FrpId));
        signSrc.append(api_response_params.get(pr_NeedResponse));
        String signMd5 = DigestUtil.hmacSign(signSrc.toString(), channelWrapper.getAPI_KEY());
        log.debug("[新亿通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }
//        else{
//            String resultStr = null;
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
////            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
////            if (StringUtils.isBlank(resultStr)) {
////                log.error("[新亿通]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
////                throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
////            }
//            
//            System.out.println("参数channelWrapper.getAPI_CHANNEL_BANK_URL()=======》"+channelWrapper.getAPI_CHANNEL_BANK_URL());
//            System.out.println("参数=======》"+JSON.toJSONString(payParam));
//            System.out.println("参数channelWrapper.getAPI_ORDER_ID()=======》"+channelWrapper.getAPI_ORDER_ID());
//            HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
//            
//            HtmlPage page = endHtml.getPage();
//            String baseURI = endHtml.getBaseURI();
//            String documentURI = endHtml.getDocumentURI();
//            String asXml = endHtml.asXml();
//            System.out.println("asXml=====>"+asXml);
//            if (StringUtils.isBlank(asXml)) {
//              log.error("[精准付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//            }
//            Elements select = Jsoup.parse(asXml).select("[id=img1]");
//            if (null == select || select.size() < 1) {
//                log.error("[精准付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            String url = select.first().attr("src");
//            if (StringUtils.isBlank(url)) {
//                log.error("[精准付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (!url.startsWith("http")) {
//                url = "http://api.scylpay.com/"+url;
//            }
//            String decode = null;
//            try {
//                decode = QRCodeUtil.decodeByUrl(URLDecoder.decode(url, "UTF-8"));
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//                log.error("[易联]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (StringUtils.isBlank(decode)) {
//                log.error("[易联]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            System.out.println("=======>"+decode);
//            result.put(QRCONTEXT, decode);
//        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[新亿通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新亿通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}