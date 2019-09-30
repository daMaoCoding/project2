package dc.pay.business.kuaifutongbao;

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
 * Aug 1, 2018
 */
@RequestPayHandler("KUAIFUTONGBAO")
public final class KuaiFuTongBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiFuTongBaoPayRequestHandler.class);

    //参数名称        参数含义                   是否必填            参数长度        参数说明        顺序
    //p0_Cmd          业务类型                   是                  Max(20)        固定值 “Buy”         1
    //p1_MerId        商户编号                   是                  Max(11)        4位数字，是商户在[API支付平台]系统的唯一身份标识请登录商户后台查看        2
    //p2_Order        商户订单号                 是                  Max(50)        若不为“”，提交的订单号必须在自身账户交易中唯一；为“”时，[API支付平台]会自动生成随机的商户订单号.[API支付平台]系统中对于已付或者撤销的订单，商户端不能重复提交        3
    //p3_Amt          支付金额                   是                  Max(20)        单位：元，精确到分，保留小数点后两位        4
    //p4_Cur          交易币种                   是                  Max(10)        固定值 “CNY”        5
    //p5_Pid          商品名称                   否                  Max(20)        用于支付时显示在[API支付平台]网关左侧的订单产品信息；此参数如用到中文，请注意转码.        6
    //p6_Pcat         商品种类                   否                  Max(20)        商品种类； 此参数如用到中文，请注意转码        7
    //p7_Pdesc        商品描述                   否                  Max(20)        商品描述； 此参数如用到中文，请注意转码        8
    //p8_Url          商户接收支付成功数据地址   是                  Max(200)       支付成功后[API支付平台]会向该地址发送两次成功通知，该地址不可以带参数，如:“ www.yeepay.com/callback.action”.注意不建议使用过长的URL地址        9
    //p9_SAF          送货地址                   是                  Max(1)         为“1”：需要用户将送货地址留在[API支付平台]系统；        为“0”：不需要，默认为“0”        10
    //pa_MP           商户扩展信息               否                  Max(200)       返回时原样返回；此参数如用到中文，请注意转码        11
    //pd_FrpId        支付通道编码               是                  Max(50)        该字段不能为空值；请按照文档下方“支付通道编码列表”所示参数进行提交        12
    //pr_NeedResponse 应答机制                   是                  Max(1)         固定值为“1”：需要应答机制；收到[API支付平台]服务器点对点支付成功通知，必须回写“success”（无关大小写），即使您收到成功通知时发现该订单已经处理过，也要正确回写“success”，否则[API支付平台]将认为您的系统没有收到通知，启动重发机制，直到收到“success”为止        13
    //hmac            签名数据                   是                  Max(32)        产生hmac需要两个参数，并调用相关API；
    private static final String p0_Cmd               ="p0_Cmd";
    private static final String p1_MerId             ="p1_MerId";
    private static final String p2_Order             ="p2_Order";
    private static final String p3_Amt               ="p3_Amt";
    private static final String p4_Cur               ="p4_Cur";
    private static final String p5_Pid               ="p5_Pid";
    private static final String p6_Pcat              ="p6_Pcat";
    private static final String p7_Pdesc             ="p7_Pdesc";
    private static final String p8_Url               ="p8_Url";
    private static final String p9_SAF               ="p9_SAF";
    private static final String pa_MP                ="pa_MP";
    private static final String pd_FrpId             ="pd_FrpId";
    private static final String pr_NeedResponse      ="pr_NeedResponse";

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
        log.debug("[快付通宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        log.debug("[快付通宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }
//        else{
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[快付通宝]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
//                throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
//            }
//            
//            Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
//            Element formEl = document.getElementsByTag("form").first();
//            //Element first = document.getElementsByTag("codeUrl").first();
//            //if (!first.hasText()) {
//            //  log.error("[精准付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //  throw new PayException(resultStr);
//            //}
//            //bodyEl = document.getElementsByTag("body").first();
//            //String attr = bodyEl.getElementById("hidUrl").val();
//            //document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
//            //bodyEl = document.getElementsByTag("body").first();
//            ////按不同的请求接口，向不同的属性设置值
//            //result.put(HandlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, bodyEl.getElementById("hidUrl").attr("value"));
//            //String jumpToUrl = Jsoup.parse(firstPayresult).select("a").first().attr("href");
//            //String val = Jsoup.parse(resultStr).select("[id=qrCodeUrl]").first().val();   如下：
//            //<input name="qrCodeUrl" value="https://qpay.qq.com/qr/51a58fea" id="qrCodeUrl" type="hidden">
//            System.out.println("请求返回=========>"+resultStr);
//            
//            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
//            String root = secondPayParam.get("action");
//            if (!root.startsWith("http")) {
//                root = "http://www.t69j.com"+root;
//            }
//            System.out.println("请求地址2=========>"+root);
//            System.out.println("请求参数2=========>"+JSON.toJSONString(secondPayParam));
//            String resultStr2 = RestTemplateUtil.postForm(root, secondPayParam,"UTF-8");
//            System.out.println("请求返回2=========>"+resultStr2);
//            
//            JSONObject resJson = JSONObject.parseObject(resultStr);
//            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                String code_url = resJson.getString("codeimg");
//                result.put(QRCONTEXT, code_url);
//            }else {
//                log.error("[快付通宝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[快付通宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[快付通宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}