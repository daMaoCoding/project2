package dc.pay.business.xinbaifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.*;
import dc.pay.utils.kspay.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("XINBAIFU")
public final class XinBaiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinBaiFuPayRequestHandler.class);

    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";


    //业务参数
     private static final  String  amount       ="amount";               //订单总金额	M	单位元
     private static final  String  notifyUrl    ="notifyUrl";            //回调通知地址	M	URLencode编码
     private static final  String  orgOrderNo   ="orgOrderNo";           //机构订单号	M	机构唯一
     private static final  String  source       ="source";               //订单付款方式	M	WXZF:微信
     private static final  String  subject      ="subject";              //商品标题	M
     private static final  String  tranTp       ="tranTp";               //通道类型	M	0：T0 ;1：T1
     private static final  String  version      ="version";              //版本号	M	固定值 : 1.0.1
     private static final  String  extra_para  ="extra_para";

    // 系统参数
     private static final  String  appKey    ="appKey";                  //商户号	M	APP_KEY
     private static final  String  format    ="format";                  //数据格式	M	固定值：json
     private static final  String  method    ="method";                  //请求方法	M	scanPay
     private static final  String  params    ="params";                  //业务参数	M	AES加密后的业务参数
     private static final  String  sign      ="sign";                    //签名	M	系统参数SHA-1加密
     private static final  String  v         ="v";                       //版本号	M	固定值：1.0

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        //
//
//
//
//
//            paramsStr = String.format("amount=%s&notifyUrl=%s&orgOrderNo=%s&source=%s&subject=Pay&tranTp=0&version=1.0.1",
//                    HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()),
//                    URLEncoder.encode(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL(), "utf-8"),
//                    channelWrapper.getAPI_ORDER_ID(),
//                    channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            paramsStr =  new String(Base64.getEncoder().encode(paramsStr.getBytes()));
//            paramsStr =  AesAndBase64Util.Encrypt(paramsStr,channelWrapper.getAPI_KEY());

//            Map<String, String> payParam = new TreeMap<String, String>() {
//                {
//                    put(appKey, channelWrapper.getAPI_MEMBERID());
//                    put(format,"json");
//                    put(method, "scanPay");
//                    put(params, paramsBuild);
//                    put(v, "1.0");
//                }
//            };
//


        Map<String, String> paramValues = new HashMap<String, String>();//所有请求参数
        try{
            Map<String, String> map = new HashMap<String, String>();//所有的业务参数放入这个map集合
            map.put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            map.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());//路径进行encoder编码 URLEncoder.encode(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL(), "utf-8")
            map.put(orgOrderNo,  channelWrapper.getAPI_ORDER_ID());
            map.put(source,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            map.put(subject, "Pay");
            map.put(tranTp, "0");
            map.put(version, "1.0.1");
            map.put(extra_para, "3556239829");
            String param = SortUtils.sort(map);//按照 abcd顺序排序 并转化为id=1&name=tom&age=23 的形式
            String base64 =  org.apache.commons.codec.binary.Base64.encodeBase64String(param.getBytes());
            String aesStr = AESUtil.encrypt(base64, channelWrapper.getAPI_KEY());

            paramValues.put(method, "scanPay"); //请求的方法名  必填 区分不同的请求
            paramValues.put(appKey, channelWrapper.getAPI_MEMBERID());//商户号 必填
            paramValues.put(v, "1.0");//版本 必填  暂定1.0
            paramValues.put(format, "json");// 返回格式 暂时只支持json 必填
            paramValues.put(params, aesStr); // 所有参数转成的字符串  必填

        }catch (Exception e){
            log.error("[新百付]-[请求支付]-1.组装请求参数出错:"+e.getMessage(),e);
            throw new PayException("新百付,组装请求参数出错");
        }
        log.debug("[新百付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(paramValues));
        return paramValues;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        //要验签的字符串
        //String signStrBuild=appKey+payParam.get(appKey)+format+payParam.get(format)+method+payParam.get(method)+params+payParam.get(params)+sign+payParam.get(params)+v+payParam.get(v);
        //待加密字符串
        //signStrBuild=channelWrapper.getAPI_KEY()+signStrBuild+channelWrapper.getAPI_KEY();
        //SHA-1 进行加密,之后转化为二进制,接着转换为十六进制,最后转为大写.
        //String sha1 = Sha1Util.getSha1(signStrBuild);
        //String pay_md5sign = MD5Util.toHex(sha1.getBytes(), false);


        String pay_md5sign = CopUtils.sign(payParam, channelWrapper.getAPI_KEY()); //验签
        log.debug("[新百付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            //JSONObject resJson = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
            JSONObject resJson = JSONObject.parseObject(resultStr);
            if(resJson.containsKey("errorCode") && resJson.getString("errorCode").equalsIgnoreCase("200") && resJson.getJSONObject("data").getString("respCode").equalsIgnoreCase("0000")){
                String qrContent = resJson.getJSONObject("data").getString("qrCode");
                HashMap<String, String> result = Maps.newHashMap();
                result.put(QRCONTEXT, qrContent);
                result.put(PARSEHTML, resJson.toJSONString());
                payResultList.add(result);
            }else{
                log.error("[新百付]3.发送支付请求，及获取支付请求结果：" + resJson.toJSONString() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        } catch (Exception e) {
            log.error("[新百付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[新百付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(JUMPURL)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayHtmlContent(null);
                    requestPayResult.setRequestPayJumpToUrl(resultMap.get(JUMPURL));
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                }
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[新百付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}