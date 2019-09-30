package dc.pay.business.huihe;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("HUIHE")
public final class HuiHePayRequestHandler extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(HuiHePayRequestHandler.class);

    private static  final String   RESPCODE  = "respCode";
    private static  final String   MESSAGE  = "message";
    private static  final String   BARCODE	="barCode";

    private static  final String Code   = "Code";      //结果代码,返回0表示成功
    private static  final String QrCode   = "QrCode"; //二维码地址，当选择扫码支付时，会返回二维码的链接地址，由商户自行生成二维码

    private static  final String AppId  = "AppId";                //系统分配给开发者的应用ID（等同于商户号）	2014072300007148
    private static  final String Method  = "Method";              //接口名称	trade.page.pay
    private static  final String Format  = "Format";              //仅支持JSON	JSON
    private static  final String Charset  = "Charset";            //请求使用的编码格式，仅支持UTF-8	UTF-8
    private static  final String Version  = "Version";            //调用的接口版本，固定为：1.0	1.0
    private static  final String SignType  = "SignType";          //商户生成签名字符串所使用的签名算法类型，目前仅支持MD5，后续将支持RSA2	MD5
    private static  final String Sign  = "Sign";                  //商户请求参数的签名串，详见附件SDK	加密结果大写
    private static  final String Timestamp  = "Timestamp";        //发送请求的时间，格式"yyyy-MM-dd HH:mm:ss"	2017-07-24 03:07:50
    private static  final String PayType  = "PayType";            //支付类型
    private static  final String OutTradeNo  = "OutTradeNo";      //商户订单号，64个字符以内、可包含字母、数字、下划线；需保证在商户端不重复	20150320010101001
    private static  final String TotalAmount  = "TotalAmount";    //订单总金额，单位为元，精确到小数点后两位，取值范围[0.01,100000000]	88.88
    private static  final String Subject  = "Subject";             //订单标题	Iphone6 16G
    private static  final String Body  = "Body";                   //订单描述	Iphone6 16G
    private static  final String NotifyUrl  = "NotifyUrl";         //服务器主动通知商户服务器里指定的页面，http/https路径。	https://www.baidu.com



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
            Map<String, String> payParam = Maps.newTreeMap();
            payParam.put(AppId, channelWrapper.getAPI_MEMBERID());
            payParam.put(Method, "trade.page.pay");
            payParam.put(Format, "JSON");
            payParam.put(Charset, "UTF-8");
            payParam.put(Version, "1.0");
            payParam.put(SignType, "MD5");
            payParam.put(Timestamp, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
            payParam.put(PayType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(OutTradeNo, channelWrapper.getAPI_ORDER_ID());
            payParam.put(TotalAmount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(Subject,"PAY");
            payParam.put(Body,"3556239829");
            payParam.put(NotifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            log.debug("[汇合]-[请求支付]-1.组装请求参数完成：{}",JSON.toJSONString(payParam));
            return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        String prestr = HuiHePayUtil.createLinkString(payParam); //把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        String pay_md5sign =HuiHePayUtil.sign(prestr, channelWrapper.getAPI_KEY(), "UTF-8");
        log.debug("[汇合]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(pay_md5sign));
       return pay_md5sign;
    }



    protected List<Map<String,String>> sendRequestGetResult(Map<String, String> payParam,String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try {
            String channel_bank = channelWrapper.getAPI_CHANNEL_BANK_NAME();
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,String.class,HttpMethod.POST).trim();
            //resultStr = new String(resultStr.getBytes("ISO-8859-1"), "GBK");
             JSONObject responseJsonObject = JSONObject.parseObject(resultStr);
              String respCode  = responseJsonObject.getString(Code) ;
              String respQrCode   = responseJsonObject.getString(QrCode) ;
            if("0".equalsIgnoreCase(respCode) && StringUtils.isNotBlank(respQrCode)){
                HashMap<String, String> result = Maps.newHashMap();
                if(HandlerUtil.isWapOrApp(channelWrapper)){
                    result.put(JUMPURL,respQrCode);
                }else{
                    result.put(QRCONTEXT,respQrCode);
                }
                    result.put(PARSEHTML,resultStr);
                    payResultList.add(result);
            }else{
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[汇合]3.发送支付请求，及获取支付请求结果出错：{}",e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[汇合]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String,String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(null!=resultListMap && !resultListMap.isEmpty()){
            if(resultListMap.size()==1){
                Map<String, String> resultMap = resultListMap.get(0);
                buildResult( resultMap, channelWrapper,requestPayResult);
            }
            if(ValidateUtil.requestesultValdata(requestPayResult)){
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            }else{
                throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[汇合]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}